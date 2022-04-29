/*
 * Copyright (c) 2019-2022 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.shure;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.api.dal.error.CommandFailureException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.api.dal.monitor.aggregator.Aggregator;
import com.avispl.symphony.dal.aggregator.parser.AggregatedDeviceProcessor;
import com.avispl.symphony.dal.aggregator.parser.PropertiesMapping;
import com.avispl.symphony.dal.aggregator.parser.PropertiesMappingParser;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.communicator.shure.error.DeviceRetrievalException;
import com.avispl.symphony.dal.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.*;

import static java.util.stream.Collectors.toList;

/**
 * This class handles all communications to and from a Shure SystemOn gateway.
 *
 * @author Symphony Dev Team<br> Created on May 21, 2019
 * @since 1.0.0
 */
public class ShureSystemOn extends RestCommunicator implements Aggregator, Monitorable, Controller {
    /**
     * Process that is running constantly and triggers collecting data from SystemOn API endpoints,
     * based on the given timeouts and thresholds.
     *
     * @author Maksym.Rossiytsev
     * @since 1.1.3
     */
    class SystemOnDeviceDataLoader implements Runnable {
        private volatile boolean inProgress;

        public SystemOnDeviceDataLoader() {
            inProgress = true;
        }

        @Override
        public void run() {
            mainloop:
            while (inProgress) {
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    // Ignore for now
                }

                if (!inProgress) {
                    break mainloop;
                }

                // next line will determine whether Shure monitoring was paused
                updateAggregatorStatus();
                if (devicePaused) {
                    continue mainloop;
                }

                while (nextDevicesCollectionIterationTimestamp > System.currentTimeMillis()) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(1000);
                    } catch (InterruptedException e) {
                        //
                    }
                }

                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Fetching devices list");
                    }
                    fetchDevicesList();

                    if (logger.isDebugEnabled()) {
                        logger.debug("Fetched devices list: " + aggregatedDevices);
                    }
                } catch (Exception e) {
                    String message = e.getMessage();
                    latestErrors.put(e.getClass().getSimpleName(), limitErrorMessageByLength(message, 120));
                    logger.error("Error occurred during device list retrieval: " + message + " with cause: " + e.getCause().getMessage(), e);
                }

                String[] hardwareIds = hardwareIdFilter.split(",");
                for (String hardwareId: hardwareIds) {
                    if (!inProgress) {
                        break;
                    }
                    devicesExecutionPool.add(executorService.submit(() -> {
                        boolean retrievedWithError = false;
                        try {
                            fetchDeviceByHardwareId(hardwareId);
                        } catch (Exception e) {
                            retrievedWithError = true;
                            latestErrors.put(String.format("%s[%s]", e.getClass().getSimpleName(), hardwareId), limitErrorMessageByLength(e.getMessage(), 120));
                            logger.error(String.format("Exception during retrieval device by hardware id '%s'.", hardwareId), e);
                        }

                        if (!retrievedWithError) {
                            // Remove error related to a specific device from the collection, since
                            // it is retrieved successfully now.
                            latestErrors.keySet().removeIf(s -> s.contains(String.format("[%s]", hardwareId)));
                        }
                    }));
                }

                if (!inProgress) {
                    break mainloop;
                }
                do {
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        if (!inProgress) {
                            break;
                        }
                    }
                    devicesExecutionPool.removeIf(Future::isDone);
                } while (!devicesExecutionPool.isEmpty());

                // We don't want to fetch devices statuses too often, so by default it's currentTime + 30s
                // otherwise - the variable is reset by the retrieveMultipleStatistics() call, which
                // launches devices detailed statistics collection
                nextDevicesCollectionIterationTimestamp = System.currentTimeMillis() + 30000;

                if (logger.isDebugEnabled()) {
                    logger.debug("Finished collecting devices statistics cycle at " + new Date());
                }
            }
            // Finished collecting
        }

        /**
         * Triggers main loop to stop
         */
        public void stop() {
            inProgress = false;
        }
    }

    private AggregatedDeviceProcessor aggregatedDeviceProcessor;
    /**
     * Adapter metadata, collected from the version.properties
     * @since 1.1.3
     */
    private Properties adapterProperties;
    /**
     * Whether service is running.
     * @since 1.1.3
     */
    private volatile boolean serviceRunning;
    /**
     * Device adapter instantiation timestamp.
     * @since 1.1.3
     */
    private long adapterInitializationTimestamp;
    /**
     * If the {@link ShureSystemOn#deviceMetaDataRetrievalTimeout} is set to a value that is too small -
     * devices list will be fetched too frequently. In order to avoid this - the minimal value is based on this value.
     * @since 1.1.3
     */
    private static final long defaultMetaDataTimeout = 60 * 1000 / 2;
    /**
     * Device metadata retrieval timeout. The general devices list is retrieved once during this time period.
     * @since 1.1.3
     */
    private long deviceMetaDataRetrievalTimeout = 60 * 1000 / 2;
    /**
     * Number of devices loaded per API response page. Used to reduce the size of a singular API payload.
     * @since 1.1.3
     */
    private int deviceNumberPerPage = 10;
    /**
     * Aggregator inactivity timeout. If the {@link ShureSystemOn#retrieveMultipleStatistics()}  method is not
     * called during this period of time - device is considered to be paused, thus the Cloud API
     * is not supposed to be called
     * @since 1.1.3
     */
    private static final long retrieveStatisticsTimeOut = 3 * 60 * 1000;
    /**
     * We don't want the statistics to be collected constantly, because if there's not a big list of devices -
     * new devices statistics loop will be launched before the next monitoring iteration. To avoid that -
     * this variable stores a timestamp which validates it, so when the devices statistics is done collecting, variable
     * is set to currentTime + 30s, at the same time, calling {@link #retrieveMultipleStatistics()} and updating the
     * {@link #aggregatedDevices} resets it to the currentTime timestamp, which will re-activate data collection.
     * @since 1.1.3
     */
    private static long nextDevicesCollectionIterationTimestamp;

    /**
     * This parameter holds timestamp of when we need to stop performing API calls
     * It used when device stop retrieving statistic. Updated each time of called #retrieveMultipleStatistics
     * @since 1.1.3
     */
    private volatile long validRetrieveStatisticsTimestamp;
    /**
     * Time period within which the device metadata (basic devices information) cannot be refreshed.
     * Ignored if device list is not yet retrieved or the cached device list is empty {@link ShureSystemOn#aggregatedDevices}
     * @since 1.1.3
     */
    private volatile long validDeviceMetaDataRetrievalPeriodTimestamp;
    /**
     * Indicates whether a device is considered as paused.
     * True by default so if the system is rebooted and the actual value is lost -> the device won't start stats
     * collection unless the {@link ShureSystemOn#retrieveMultipleStatistics()} method is called which will change it
     * to a correct value
     * @since 1.1.3
     */
    private volatile boolean devicePaused = true;
    /**
     * CSV string of device models to monitor
     * @since 1.1.3
     */
    private String deviceModelFilter;
    /**
     * CSV string of device hardware ids to monitor
     * @since 1.1.3
     */
    private String hardwareIdFilter;
    /**
     * Executor that runs all the async operations, that {@link #deviceDataLoader} is posting and
     * {@link #devicesExecutionPool} is keeping track of
     * @since 1.1.3
     */
    private static ExecutorService executorService;
    /**
     * Runner service responsible for collecting data and posting processes to {@link #devicesExecutionPool}
     * @since 1.1.3
     */
    private SystemOnDeviceDataLoader deviceDataLoader;
    /**
     * Contains the latest communication errors, for the latest data collection iteration
     * @since 1.1.3
     */
    private volatile ConcurrentHashMap<String, String> latestErrors = new ConcurrentHashMap<>();
    /**
     * Devices this aggregator is responsible for
     * Data is cached and retrieved every {@link #defaultMetaDataTimeout}
     * @since 1.1.3
     */
    private ConcurrentHashMap<String, AggregatedDevice> aggregatedDevices = new ConcurrentHashMap<>();
    /**
     * Pool for keeping all the async operations in, to track any operations in progress and cancel them if needed
     * @since 1.1.3
     */
    private List<Future> devicesExecutionPool = new ArrayList<>();

    private static final String BASE_URL = "api/v1.0";
    private static final String ERROR_CODE = "DeviceNotInitialized";

    /**
     * Retrieves {@code {@link #deviceMetaDataRetrievalTimeout }}
     *
     * @return value of {@link #deviceMetaDataRetrievalTimeout}
     * @since 1.1.3
     */
    public long getDeviceMetaDataRetrievalTimeout() {
        return deviceMetaDataRetrievalTimeout;
    }

    /**
     * Sets {@code deviceMetaDataInformationRetrievalTimeout}
     *
     * @param deviceMetaDataRetrievalTimeout the {@code long} field
     * @since 1.1.3
     */
    public void setDeviceMetaDataRetrievalTimeout(long deviceMetaDataRetrievalTimeout) {
        this.deviceMetaDataRetrievalTimeout = Math.max(defaultMetaDataTimeout, deviceMetaDataRetrievalTimeout);
    }

    /**
     * Retrieves {@link #deviceNumberPerPage}
     *
     * @return value of {@link #deviceNumberPerPage}
     * @since 1.1.3
     */
    public int getDeviceNumberPerPage() {
        return deviceNumberPerPage;
    }

    /**
     * Sets {@link #deviceNumberPerPage} value
     *
     * @param deviceNumberPerPage new value of {@link #deviceNumberPerPage}
     * @since 1.1.3
     */
    public void setDeviceNumberPerPage(int deviceNumberPerPage) {
        this.deviceNumberPerPage = deviceNumberPerPage;
    }
    /**
     * Retrieves {@link #deviceModelFilter}
     *
     * @return value of {@link #deviceModelFilter}
     * @since 1.1.3
     */
    public String getDeviceModelFilter() {
        return deviceModelFilter;
    }

    /**
     * Sets {@link #deviceModelFilter} value
     *
     * @param deviceModelFilter new value of {@link #deviceModelFilter}
     * @since 1.1.3
     */
    public void setDeviceModelFilter(String deviceModelFilter) {
        this.deviceModelFilter = deviceModelFilter;
    }

    /**
     * Retrieves {@link #hardwareIdFilter}
     *
     * @return value of {@link #hardwareIdFilter}
     * @since 1.1.3
     */
    public String getHardwareIdFilter() {
        return hardwareIdFilter;
    }

    /**
     * Sets {@link #hardwareIdFilter} value
     *
     * @param hardwareIdFilter new value of {@link #hardwareIdFilter}
     * @since 1.1.3
     */
    public void setHardwareIdFilter(String hardwareIdFilter) {
        this.hardwareIdFilter = hardwareIdFilter;
    }

    /**
     * Default Constructor
     */
    public ShureSystemOn() {
        // override default value to trust all certificates - MCUs typically do not have trusted certificates installed
        // it can be changed back by configuration
        setTrustAllCertificates(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void internalInit() throws Exception {
        super.internalInit();
        Map<String, PropertiesMapping> models = new PropertiesMappingParser()
                .loadYML("shure/model-mapping.yml", getClass());
        aggregatedDeviceProcessor = new AggregatedDeviceProcessor(models);

        if (logger.isDebugEnabled()) {
            logger.debug("Internal init is called.");
        }

        adapterInitializationTimestamp = System.currentTimeMillis();

        if (StringUtils.isNullOrEmpty(hardwareIdFilter)) {
            executorService = Executors.newFixedThreadPool(1);
        } else {
            // Setting a thread pool number to be the largest number between 1-10
            executorService = Executors.newFixedThreadPool(getTargetThreadsNumber(hardwareIdFilter.split(",").length));
        }

        executorService.submit(deviceDataLoader = new SystemOnDeviceDataLoader());
        validDeviceMetaDataRetrievalPeriodTimestamp = System.currentTimeMillis();
        serviceRunning = true;
        adapterProperties = new Properties();
        adapterProperties.load(getClass().getResourceAsStream("/version.properties"));
        super.internalInit();
    }

    /**
     * {@inheritDoc}
     * @since 1.1.3
     */
    @Override
    protected void internalDestroy() {
        if (logger.isDebugEnabled()) {
            logger.debug("Internal destroy is called.");
        }
        serviceRunning = false;

        if (deviceDataLoader != null) {
            deviceDataLoader.stop();
            deviceDataLoader = null;
        }

        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }

        devicesExecutionPool.forEach(future -> future.cancel(true));
        devicesExecutionPool.clear();

        aggregatedDevices.clear();
        super.internalDestroy();
    }

    /**
    * {@inheritDoc}
    * @since 1.1.3
    */
    @Override
    public List<Statistics> getMultipleStatistics() throws Exception {
        Map<String, String> statistics = new HashMap<>();
        ExtendedStatistics extendedStatistics = new ExtendedStatistics();

        statistics.put("AdapterVersion", adapterProperties.getProperty("aggregator.version"));
        statistics.put("AdapterBuildDate", adapterProperties.getProperty("aggregator.build.date"));
        statistics.put("AdapterUptime", normalizeUptime((System.currentTimeMillis() - adapterInitializationTimestamp) / 1000));

        latestErrors.forEach((key, value) -> statistics.put("Errors#" + key, value));
        extendedStatistics.setStatistics(statistics);
        return Collections.singletonList(extendedStatistics);
    }

    /**
     * Shure SystemOn doesn't require authentication.
     */
    @Override
    protected void authenticate() {
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    public void controlProperties(List<ControllableProperty> controllableProperties) throws Exception {
        if (CollectionUtils.isEmpty(controllableProperties)) {
            throw new IllegalArgumentException("Controllable properties cannot be null or empty");
        }

        for (ControllableProperty controllableProperty : controllableProperties) {
            controlProperty(controllableProperty);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    public void controlProperty(ControllableProperty controllableProperty) throws Exception {
        String property = controllableProperty.getProperty();
        Object value = controllableProperty.getValue();
        String deviceId = controllableProperty.getDeviceId();

        if (logger.isDebugEnabled()) {
            logger.debug("ShureSystemOn controlProperty property=" + property + " value=" + value +
                    " deviceId=" + deviceId);
        }

        doControl(property, value, deviceId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    public List<AggregatedDevice> retrieveMultipleStatistics() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Adapter initialized: %s, executorService exists: %s, serviceRunning: %s", isInitialized(), executorService != null, serviceRunning));
        }
        if (executorService == null) {
            // Due to the bug that after changing properties on fly - the adapter is destroyed but is not initialized properly afterwards,
            // so executor service is not running. We need to make sure executorService exists
            executorService = Executors.newFixedThreadPool(1);
            executorService.submit(deviceDataLoader = new SystemOnDeviceDataLoader());
        }
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Aggregator Multiple statistics requested. Aggregated Devices collected so far: %s. Runner thread running: %s. Executor terminated: %s",
                    aggregatedDevices.size(), serviceRunning, executorService.isTerminated()));
        }

        long currentTimestamp = System.currentTimeMillis();
        nextDevicesCollectionIterationTimestamp = currentTimestamp;
        updateValidRetrieveStatisticsTimestamp();

        aggregatedDevices.values().forEach(aggregatedDevice -> aggregatedDevice.setTimestamp(currentTimestamp));
        return new ArrayList<>(aggregatedDevices.values());
    }

    /**
     * Retrieve devices information based on hardwareIds
     *
     * @param hardwareId to retrieve device for
     * @since 1.1.3
     * @throws Exception if any error occurs
     * @throws DeviceRetrievalException if unable to find the device by hardwareId provided
     */
    private void fetchDeviceByHardwareId(String hardwareId) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Fetching device by hardwareId " + hardwareId);
        }
        JsonNode properties = doGet(BASE_URL + "/devices/" + hardwareId, JsonNode.class);

        AggregatedDevice aggregatedDevice = new AggregatedDevice();
        JsonNode deviceModel = properties.get("model");
        if (deviceModel == null || deviceModel.isNull()) {
            aggregatedDevices.remove(hardwareId);
            throw new DeviceRetrievalException(String.format("Unable to retrieve properties for device with hardwareId %s: No device model name available. Properties available: %s",
                    hardwareId, properties));
        }
        String deviceModelName = deviceModel.asText();
        if (StringUtils.isNotNullOrEmpty(deviceModelFilter) && deviceModelFilter.contains(deviceModelName)) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Device with hardwareId '%s' was already retrieved by the model filter '%s'. Skipping.",
                        hardwareId, deviceModelName));
            }
            return;
        }
        aggregatedDeviceProcessor.applyProperties(aggregatedDevice, properties, deviceModel.asText());
        String deviceId = aggregatedDevice.getDeviceId();
        aggregatedDevice.setTimestamp(System.currentTimeMillis());
        if (aggregatedDevices.containsKey(deviceId)) {
            aggregatedDevices.get(deviceId).setDeviceOnline(aggregatedDevice.getDeviceOnline());
        } else {
            aggregatedDevices.put(deviceId, aggregatedDevice);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Updated/Retrieved Shure SystemOn device with hardwareId '%s'", hardwareId));
        }
    }

    /**
     * Fetch full devices list, or list of devices based on {@link #deviceModelFilter}
     *
     * @since 1.1.3
     * @throws Exception if any error occurs
     */
    private void fetchDevicesList() throws Exception {
        long currentTimestamp = System.currentTimeMillis();
        if (aggregatedDevices.size() > 0 && validDeviceMetaDataRetrievalPeriodTimestamp > currentTimestamp) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("General devices metadata retrieval is in cooldown. %s seconds left",
                        (validDeviceMetaDataRetrievalPeriodTimestamp - currentTimestamp) / 1000));
            }
            return;
        }
        if (StringUtils.isNotNullOrEmpty(hardwareIdFilter) && StringUtils.isNullOrEmpty(deviceModelFilter)) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Skipping unfiltered devices search, hardwareIdFilter is provided: '%s'", hardwareIdFilter));
            }
            return;
        }
        // Clear latest errors because devices are retrieved from scratch now. This would only happen if hardwareIdFilter is not active.
        latestErrors.clear();

        validDeviceMetaDataRetrievalPeriodTimestamp = currentTimestamp + deviceMetaDataRetrievalTimeout;
        StringBuilder builder = new StringBuilder();
        buildQueryStringFromCSV(builder, "deviceModels", deviceModelFilter);
        JsonNode properties = doGet(BASE_URL + "/devices" + builder, JsonNode.class);

        List<String> retrievedRoomIds = new ArrayList<>();
        List<AggregatedDevice> statistics = aggregatedDeviceProcessor.extractDevices(properties);

        statistics.forEach(device -> {
            String deviceId = device.getDeviceId();
            device.setTimestamp(currentTimestamp);
            retrievedRoomIds.add(deviceId);
            if (aggregatedDevices.containsKey(deviceId)) {
                aggregatedDevices.get(deviceId).setDeviceOnline(device.getDeviceOnline());
            } else {
                aggregatedDevices.put(deviceId, device);
            }
        });

        if (logger.isDebugEnabled()) {
            logger.debug("Updated Shure SystemOn devices metadata: " + aggregatedDevices);
        }
        // Remove devices that were not populated by the API and are not a part of hardwareIdFilter,
        // so they won't be retrieved later and were not retrieved by the filtered approach
        aggregatedDevices.keySet().removeIf(existingDevice -> !retrievedRoomIds.contains(existingDevice)
        && !hardwareIdFilter.contains(existingDevice));

        if (statistics.isEmpty() && StringUtils.isNullOrEmpty(hardwareIdFilter)) {
            // If all the devices were not populated for any specific reason (no devices available, filtering, etc)
            aggregatedDevices.clear();
        }

        nextDevicesCollectionIterationTimestamp = System.currentTimeMillis();
    }

    /**
     * Build query string based on CSV string.
     * Provides array query string values, like name=value&name=value2....
     *
     * @param builder container for the query string
     * @param name query string name
     * @param values to set for the query string
     * @since 1.1.3
     */
    private void buildQueryStringFromCSV (StringBuilder builder, String name, String values) {
        if (!StringUtils.isNullOrEmpty(values)) {
            if (builder.length() == 0) {
                builder.append("?");
            }
            Arrays.stream(values.split(",")).forEach(s -> builder.append(name).append("=").append(s.trim()).append("&"));
        }
    }

    /**
     * Add authorization and content-type headers for requests
     */
    @Override
    protected HttpHeaders putExtraRequestHeaders(HttpMethod httpMethod, String uri, HttpHeaders headers) {
        headers.set("accept", "application/json");
        String apiKey = getPassword();
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("Authorization", apiKey);
        }

        if (!httpMethod.equals(HttpMethod.GET)) {
            headers.set("Content-Type", "application/json");
        }

        return headers;
    }

    /**
     * According to Shure SystemOn API documentation: DeviceAuthentication API section
     * Initialize Shure networked device /api/v1.0/devices/{hardwareId}/initialize
     * It need do before call any control API. In other cases API return error
     *
     * @param deviceId Shure device ID
     */
    private void initShureDevice(String deviceId) throws Exception {
        doPost(String.format(BASE_URL + "/devices/%s/initialize", deviceId), null);
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    public List<AggregatedDevice> retrieveMultipleStatistics(List<String> deviceIds) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("ShureSystemOn retrieveMultipleStatistics deviceIds=" + String.join(" ", deviceIds));
        }
        return retrieveMultipleStatistics()
                .stream()
                .filter(aggregatedDevice -> deviceIds.contains(aggregatedDevice.getDeviceId()))
                .collect(toList());
    }

    /**
     * Bypass the automixer settings on device
     *
     * @param deviceId device id
     * @throws Exception
     */
    private void automixerBypass(String deviceId) throws Exception {
        try {
            doPut(String.format(BASE_URL + "/devices/%s/automixer/bypass", deviceId), null, JsonNode.class);
        } catch (CommandFailureException e) {
            checkResponseCode(deviceId, e.getResponse(), "BypassAllEq", false);
        }
    }

    /**
     * Check response code retrieved during control operations
     *
     * @param deviceId that the controllable property was triggered for
     * @param response received from the API
     * @param controlName that was triggered
     * @param value that controllable property was set to
     * @throws Exception if any error occurs
     */
    private void checkResponseCode(String deviceId, String response, String controlName, boolean value) throws Exception {
        if (response != null) {
            JsonNode node = new ObjectMapper().readTree(response);
            String responseCode = findPath(node, "code");
            if (responseCode != null && responseCode.equals(ERROR_CODE)) {
                initShureDevice(deviceId);
                doControl(controlName, value, deviceId);
            }
        }
    }

    /**
     * Control device based on the control activated (by name)
     *
     * @param controlName name of the controllable property
     * @param value new value for the controllable property
     * @param deviceId device id for which the controllable property was activated
     * @throws Exception if any error occurs
     */
    private void doControl(String controlName, Object value, String deviceId) throws Exception {
        boolean controlActivated = true;
        switch (controlName) {
            case "BypassAllEq":
                automixerBypass(deviceId);
                break;
            case "Mute":
                mute(deviceId, (int) value == 1);
                break;
            case "DanteEncryption":
                encryption(deviceId, (int) value == 1);
                break;
            case "Reboot":
                reboot(deviceId);
                break;
            case "Reset":
                defaultsReset(deviceId);
                break;
            default:
                controlActivated = false;
                break;
        }
        if (controlActivated) {
            updateLocalControllableProperty(deviceId, controlName, value);
        }
    }

    /**
     * Send mute command to device
     *
     * @param deviceId device id
     * @param value    property status
     * @throws Exception
     */
    private void mute(String deviceId, boolean value) throws Exception {
        String body = String.format("{ \"muteState\": \"%s\" }", value);
        try {
            doPatch(String.format(BASE_URL + "/devices/%s/audio/mute", deviceId), body, JsonNode.class);
        } catch (CommandFailureException e) {
            checkResponseCode(deviceId, e.getResponse(), "Mute", value);
        }

    }

    /**
     * Send enable/disable audio encryption to device
     *
     * @param deviceId device id
     * @param value    property status
     * @throws Exception
     */
    private void encryption(String deviceId, boolean value) throws Exception {
        String state;
        if (value) {
            state = "enable";
        } else {
            state = "disable";
        }
        try {
            doPatch(String.format(BASE_URL + "/devices/%s/encryption/audio/%s", deviceId, state), null, JsonNode.class);
        } catch (CommandFailureException e) {
            checkResponseCode(deviceId, e.getResponse(), "DanteEncryption", value);
        }
    }

    /**
     * Send reboot command to device
     *
     * @param deviceId device id
     * @throws Exception
     */
    private void reboot(String deviceId) throws Exception {
        try {
            doPost(String.format(BASE_URL + "/devices/%s/maintenance/reboot", deviceId), null, JsonNode.class);
        } catch (CommandFailureException e) {
            checkResponseCode(deviceId, e.getResponse(), "Reboot", false);
        }
    }

    /**
     * Send reset all user presets command to device
     *
     * @param deviceId device id
     * @throws Exception
     */
    private void defaultsReset(String deviceId) throws Exception {
        try {
            doPost(String.format(BASE_URL + "/devices/%s/maintenance/defaultsreset", deviceId), null, JsonNode.class);
        } catch (CommandFailureException e) {
            checkResponseCode(deviceId, e.getResponse(), "Reset", false);
        }
    }

    /**
     * Find value by complex json path
     *
     * @param node json node
     * @param path path to node
     * @return node value
     */
    private String findPath(JsonNode node, String path) {
        String pointer = String.format("/%s", path.replace(".", "/"));
        String value = node.at(pointer).asText();
        if (!value.isEmpty()) {
            return value;
        }
        return node.asText();
    }

    /**
     * Update local controllable property before data is fetched from the remote endpoint.
     * This provides more consistency on UI.
     *
     * @param deviceId to update property for
     * @param name property name to update to
     * @param value new controllable property value
     * @since 1.1.3
     */
    private void updateLocalControllableProperty(String deviceId, String name, Object value){
        aggregatedDevices.get(deviceId).getControllableProperties().stream().filter(advancedControllableProperty ->
                advancedControllableProperty.getName().equals(name)).findAny()
                .ifPresent(advancedControllableProperty -> advancedControllableProperty.setValue(value));
    }

    /**
     * Update the status of the device.
     * The device is considered as paused if did not receive any retrieveMultipleStatistics()
     * calls during {@link ShureSystemOn#validRetrieveStatisticsTimestamp}
     *
     * @since 1.1.3
     */
    private synchronized void updateAggregatorStatus() {
        devicePaused = validRetrieveStatisticsTimestamp < System.currentTimeMillis();
    }

    /**
     * Update general aggregator status (paused or active) and update the value, based on which
     * it the device is considered paused (2 minutes inactivity -> {@link #retrieveStatisticsTimeOut})
     *
     * @since 1.1.3
     */
    private synchronized void updateValidRetrieveStatisticsTimestamp() {
        validRetrieveStatisticsTimestamp = System.currentTimeMillis() + retrieveStatisticsTimeOut;
        updateAggregatorStatus();
    }

    /**
     * Uptime is received in seconds, need to normalize it and make it human readable, like
     * 1 day(s) 5 hour(s) 12 minute(s) 55 minute(s)
     * Incoming parameter is may have a decimal point, so in order to safely process this - it's rounded first.
     * We don't need to add a segment of time if it's 0.
     *
     * @param uptimeSeconds value in seconds
     * @return string value of format 'x day(s) x hour(s) x minute(s) x minute(s)'
     * @since 1.1.3
     */
    private String normalizeUptime(long uptimeSeconds) {
        StringBuilder normalizedUptime = new StringBuilder();

        long seconds = uptimeSeconds % 60;
        long minutes = uptimeSeconds % 3600 / 60;
        long hours = uptimeSeconds % 86400 / 3600;
        long days = uptimeSeconds / 86400;

        if (days > 0) {
            normalizedUptime.append(days).append(" day(s) ");
        }
        if (hours > 0) {
            normalizedUptime.append(hours).append(" hour(s) ");
        }
        if (minutes > 0) {
            normalizedUptime.append(minutes).append(" minute(s) ");
        }
        if (seconds > 0) {
            normalizedUptime.append(seconds).append(" second(s)");
        }
        return normalizedUptime.toString().trim();
    }

    /**
     * Multiple statistics include error data, in order to display it properly - messages must be limited
     * by length. This method cuts messages to a max length passed.
     *
     * @param originalMessage string message to crop
     * @param length max length value to adjust message to
     * @return String value of cropped string
     * @since 1.1.3
     */
    private String limitErrorMessageByLength(String originalMessage, int length) {
        int messageLength = originalMessage.length();
        String resultMessage =  originalMessage.substring(0, Math.min(messageLength, length));
        if (messageLength > length) {
            return resultMessage + "...";
        }
        return resultMessage;
    }

    /**
     * Calculates optimal average number of threads necessary for data retrieval,
     * only relevant for retrieving data based on {@link #hardwareIdFilter}
     *
     * @param basis number to retrieve end number of threads based on
     * @return numeric value of optimal number of threads
     * @since 1.1.3
     */
    private int getTargetThreadsNumber(int basis) {
        int max = 1;
        for (int i = 1; i < 10; i++) {
            if (basis % i == 0) {
                max = i;
            }
        }
        return max;
    }
}
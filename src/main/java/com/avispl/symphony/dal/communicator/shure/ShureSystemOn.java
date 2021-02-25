/*
 * Copyright (c) 2019-2020 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.shure;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.api.dal.error.CommandFailureException;
import com.avispl.symphony.api.dal.monitor.aggregator.Aggregator;
import com.avispl.symphony.dal.aggregator.parser.AggregatedDeviceProcessor;
import com.avispl.symphony.dal.aggregator.parser.PropertiesMapping;
import com.avispl.symphony.dal.aggregator.parser.PropertiesMappingParser;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * This class handles all communications to and from a Shure SystemOn gateway.
 *
 * @author Symphony Dev Team<br> Created on May 21, 2019
 * @since 4.7
 */
public class ShureSystemOn extends RestCommunicator implements Aggregator, Controller {

    private List<String> modelsWithoutInitialize = Arrays.asList("MXWAPT2", "MXW6", "MXWNCS2");
    private AggregatedDeviceProcessor aggregatedDeviceProcessor;

    private static final String BASE_URL = "/api/v1.0";
    private static final String ERROR_CODE = "DeviceNotInitialized";

    /**
     * Constructor
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
        JsonNode properties = doGet(BASE_URL + "/devices", JsonNode.class);

        List<AggregatedDevice> statistics = aggregatedDeviceProcessor.extractDevices(properties);
        statistics.forEach(device -> device.setTimestamp(System.currentTimeMillis()));

        if (logger.isDebugEnabled()) {
            logger.debug("ShureSystemOn retrieveMultipleStatistics statistics.size=" + statistics.size());
            statistics.forEach(
                    device -> logger.debug("ShureSystemOn retrieveMultipleStatistics DeviceId=" + device.getDeviceId() +
                            " DeviceModel=" + device.getDeviceModel()));
        }

        return statistics;
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
     * Initialize Shure networked device /api/v1.0/devices/{hardwareId}/initialize
     * It need do before call any control API
     *
     * @param device Shure device
     */
    private void initShureDevice(AggregatedDevice device) throws Exception {
        if (!modelsWithoutInitialize.contains(device.getDeviceModel())) {
            initShureDevice(device.getDeviceId());
        }
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

    private void doControl(String controlName, Object value, String deviceId) throws Exception {
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
}
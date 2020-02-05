/*
 * Copyright (c) 2019-2020 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.shure;

import com.avispl.symphony.api.common.error.TargetNotFoundException;
import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.api.dal.monitor.aggregator.Aggregator;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.communicator.shure.parser.AggregatedDeviceProcessor;
import com.avispl.symphony.dal.communicator.shure.parser.PropertiesMapping;
import com.avispl.symphony.dal.communicator.shure.parser.PropertiesMappingParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static com.avispl.symphony.dal.communicator.shure.parser.PropertiesMappingParser.DEFAULT_MODEL;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

/**
 * This class handles all communications to and from a Shure SystemOn gateway.
 *
 * @author Symphony Dev Team<br>
 * Created on May 21, 2019
 * @since 4.7
 */
public class ShureSystemOn extends RestCommunicator implements Aggregator, Controller {

    private final static Log log = LogFactory.getLog(ShureSystemOn.class);

    private Map<String, PropertiesMapping> models = emptyMap();

    /**
     * Constructor
     */
    public ShureSystemOn() {
        // override default value to trust all certificates - MCUs typically do not have trusted certificates installed
        // it can be changed back by configuration
        setTrustAllCertificates(true);
    }

    public static void main(String[] args) throws Exception {
        ShureSystemOn communicator = new ShureSystemOn();
        communicator.setHost("172.31.254.17");
        communicator.setPort(10000);
        communicator.init();

        List<AggregatedDevice> aggregatedDevices = communicator.retrieveMultipleStatistics();
        System.out.println("aggregatedDevices = " + aggregatedDevices);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void internalInit() throws Exception {
        super.internalInit();
        //Load models properties mapping
        if (logger.isDebugEnabled()) {
            logger.debug("ShureSystemOn internalInit start; models size=" + models.size());
        }
        models = new PropertiesMappingParser().load("shure/model-mapping.xml");
        if (logger.isDebugEnabled()) {
            logger.debug("ShureSystemOn internalInit end; models size=" + models.size());
        }
    }


    /**
     * Shure SystemOn doesn't require authentication.
     */
    @Override
    protected void authenticate() throws Exception {
    }

    /**
     * {@inheritDoc}
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
        switch (property) {
            case "EnableLowCutFilter":
                enableLowCutFilter(deviceId, (int) value == 1);
                break;
            case "BypassAllEq":
                bypassAllEq(deviceId, (int) value == 1);
                break;
            case "Mute":
                mute(deviceId, (int) value == 1);
                break;
            case "Encryption":
                encryption(deviceId, (int) value == 1);
                break;
            case "Reboot":
                reboot(deviceId);
                break;
            case "Reset":
                reset(deviceId);
                break;
        }
    }

    /**
     * {@inheritDoc}
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
     */
    @Override
    public List<AggregatedDevice> retrieveMultipleStatistics() throws Exception {
        JsonNode properties = doGet("/api/v1.0/devices", JsonNode.class);
        List<AggregatedDevice> statistics = StreamSupport.stream(properties.spliterator(), false)
                .map(this::parseAggregatedDevice)
                .collect(toList());

        if (logger.isDebugEnabled()) {
            logger.debug("ShureSystemOn retrieveMultipleStatistics statistics.size=" + statistics.size());
            for (AggregatedDevice device : statistics) {

                logger.debug("ShureSystemOn retrieveMultipleStatistics DeviceId=" + device.getDeviceId() +
                        " DeviceModel=" + device.getDeviceModel());
            }
        }
        return statistics;
    }

    /**
     * Parse json node to extract device data values
     * and apply them to
     *
     * @param node json node
     * @return device with the values populated
     */
    private AggregatedDevice parseAggregatedDevice(JsonNode node) {
        AggregatedDevice device = new AggregatedDevice();

        //get device model
        String model = node.findPath("model").asText();

        //find model properties mapping
        PropertiesMapping mapping = models.getOrDefault(model, models.get(DEFAULT_MODEL));

        //set calculated properties.
        device.setTimestamp(System.currentTimeMillis());

        //applyProperties properties
        AggregatedDeviceProcessor aggregatedDeviceProcessor = new AggregatedDeviceProcessor(mapping);
        aggregatedDeviceProcessor.applyProperties(device, node);

        if (logger.isDebugEnabled()) {
            logger.debug("ShureSystemOn parseAggregatedDevice model=" + model +
                    " mapping=" + mapping.toString());
        }
        System.out.println("ShureSystemOn parseAggregatedDevice model=" + model +
                " mapping=" + mapping.toString());

        return device;
    }

    /**
     * {@inheritDoc}
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
     * Enable low cut filter
     *
     * @param deviceId device is
     * @param value    property status
     * @throws Exception
     */
    private void enableLowCutFilter(String deviceId, boolean value) throws Exception {
        String model = device(deviceId).getDeviceModel().toLowerCase();
        String body = String.format("{ \"EnableLowCutFilter\": \"%s\" }", value);
        doPatch(String.format("/api/v1.0/devices/%s/%s", model, deviceId), body);
    }

    /**
     * Send BypassAllEq to device
     *
     * @param deviceId device id
     * @param value    property status
     * @throws Exception
     */
    private void bypassAllEq(String deviceId, boolean value) throws Exception {
        String model = device(deviceId).getDeviceModel().toLowerCase();
        String body = String.format("{ \"BypassAllEq\": \"%s\" }", value);
        doPatch(String.format("/api/v1.0/devices/%s/%s", model, deviceId), body);
    }

    /**
     * Get device data by device id
     *
     * @param deviceId device is
     * @return device data
     * @throws Exception
     */
    private AggregatedDevice device(String deviceId) throws Exception {
        return retrieveMultipleStatistics(ImmutableList.of(deviceId)).stream()
                .findFirst()
                .orElseThrow(() -> new TargetNotFoundException("Device " + deviceId + " not found"));
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
        doPatch(String.format("/api/v1.0/devices/%s/audio/mute", deviceId), body);
    }


    /**
     * Send enable encryption to device
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
        doPatch(String.format("/api/v1.0/devices/%s/encryption/audio/%s", deviceId, state), null);
    }

    /**
     * Send reboot command to device
     *
     * @param deviceId device id
     * @throws Exception
     */
    private void reboot(String deviceId) throws Exception {
        String body = String.format("{ \"DeviceHardwareIds\": [ \"%s\" ] }", deviceId);
        doPatch("/api/v1.0/devices/reboot", body);
    }

    /**
     * Send reset command to device
     *
     * @param deviceId device id
     * @throws Exception
     */
    private void reset(String deviceId) throws Exception {
        String body = String.format("{ \"DeviceHardwareIds\": [ \"%s\" ] }", deviceId);
        doPatch("/api/v1.0/devices/reset", body);
    }
}

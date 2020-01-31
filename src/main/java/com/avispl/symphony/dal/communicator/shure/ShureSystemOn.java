/*
 * Copyright (c) 2019 AVI-SPL Inc. All Rights Reserved.
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
        System.out.println("Shure SystemON instantiated");
        setTrustAllCertificates(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void internalInit() throws Exception {
        super.internalInit();
        //Load models properties mapping
        System.out.println("Shure SystemON internalInit");
        models = new PropertiesMappingParser().load("shure/shure-model-mapping.xml");
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
        System.out.println("Shure SystemON controlProperty " + property);
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
        JsonNode properties = doGet("/api/devices", JsonNode.class);
        System.out.println("Shure SystemON retrieveMultipleStatistics");
        return StreamSupport.stream(properties.spliterator(), false)
                .map(this::parseAggregatedDevice)
                .collect(toList());
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
        String model = node.findPath("Model").asText();

        //find model properties mapping
        PropertiesMapping mapping = models.getOrDefault(model, models.get(DEFAULT_MODEL));

        //set calculated properties.
        device.setTimestamp(System.currentTimeMillis());

        //applyProperties properties
        AggregatedDeviceProcessor aggregatedDeviceProcessor = new AggregatedDeviceProcessor(mapping);
        aggregatedDeviceProcessor.applyProperties(device, node);

        return device;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AggregatedDevice> retrieveMultipleStatistics(List<String> deviceIds) throws Exception {
        System.out.println("Shure SystemON retrieveMultipleStatistics");
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
        doPatch(String.format("/api/devices/%s/%s", model, deviceId), body);
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
        doPatch(String.format("/api/devices/%s/%s", model, deviceId), body);
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
        String body = String.format("{ \"DeviceHardwareIds\": [ \"%s\" ], \"MuteSet\": \"%s\" }", deviceId, value);
        doPatch("/api/devices/audio/mute", body);
    }


    /**
     * Send enable encryption to device
     *
     * @param deviceId device id
     * @param value    property status
     * @throws Exception
     */
    private void encryption(String deviceId, boolean value) throws Exception {
        String body = String.format("{ \"DeviceHardwareIds\": [ \"%s\" ], \"EnableEncryption\": \"%s\" }", deviceId, value);
        doPatch("/api/devices/encryption/audio", body);
    }

    /**
     * Send reboot command to device
     *
     * @param deviceId device id
     * @throws Exception
     */
    private void reboot(String deviceId) throws Exception {
        String body = String.format("{ \"DeviceHardwareIds\": [ \"%s\" ] }", deviceId);
        doPatch("/api/devices/reboot", body);
    }

    /**
     * Send reset command to device
     *
     * @param deviceId device id
     * @throws Exception
     */
    private void reset(String deviceId) throws Exception {
        String body = String.format("{ \"DeviceHardwareIds\": [ \"%s\" ] }", deviceId);
        doPatch("/api/devices/reset", body);
    }
}

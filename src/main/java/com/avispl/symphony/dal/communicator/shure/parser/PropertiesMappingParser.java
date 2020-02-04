/*
 * Copyright (c) 2019-2020 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.shure.parser;

import com.google.common.collect.ImmutableMap;

import javax.xml.bind.JAXB;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toMap;

/**
 * This class allow to parse shure models description configurations.
 *
 * @author Symphony Dev Team<br>
 * Created on May 30, 2019
 * @since 4.7
 */
public class PropertiesMappingParser {

    public final static String DEFAULT_MODEL = "generic";

    public Map<String, PropertiesMapping> load(String name) throws Exception {
        ModelConfigurationDto configuration;
		try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)) {
			configuration = JAXB.unmarshal(stream, ModelConfigurationDto.class);
		}

        ModelConfigurationDto.Model generic = configuration.getModels()
                .stream()
                .filter(m -> m.getName().equalsIgnoreCase(DEFAULT_MODEL))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Generic model was not found"));

        return configuration.getModels()
                .stream()
                .collect(toMap(
                        ModelConfigurationDto.Model::getName,
                        m -> toStatisticsMapping(m, generic)));
    }

    private PropertiesMapping toStatisticsMapping(ModelConfigurationDto.Model model, ModelConfigurationDto.Model generic) {
        Map<String, String> properties = new HashMap<>(nameToRefMap(generic.getProperties()));
        properties.putAll(nameToRefMap(model.getProperties()));

        Map<String, String> statistics = new HashMap<>(nameToRefMap(generic.getStatistic()));
        statistics.putAll(nameToRefMap(model.getStatistic()));

        Map<String, String> deviceProperties = new HashMap<>(nameToRefMap(generic.getMappings()));
        deviceProperties.putAll(nameToRefMap(model.getMappings()));

        Map<String, String> predefinedProperties = new HashMap<>(nameToValueMap(generic.getMappings()));
        predefinedProperties.putAll(nameToValueMap(model.getMappings()));

        Map<String, String> controlProperties = new HashMap<>(nameToValueMap(generic.getControls()));
        controlProperties.putAll(nameToValueMap(model.getControls()));

        return new PropertiesMapping(properties, statistics, deviceProperties, predefinedProperties, controlProperties);
    }

    private Map<String, String> nameToRefMap(List<ModelConfigurationDto.Property> properties) {
        return properties
                .stream()
                .filter(p -> Objects.nonNull(p.getRef()))
                .collect(toMap(
                        ModelConfigurationDto.Property::getName,
                        ModelConfigurationDto.Property::getRef));
    }

    private Map<String, String> nameToValueMap(List<ModelConfigurationDto.Property> properties) {
        return properties
                .stream()
                .filter(p -> Objects.nonNull(p.getValue()))
                .collect(toMap(
                        ModelConfigurationDto.Property::getName,
                        ModelConfigurationDto.Property::getValue));
    }

    public Map<String, PropertiesMapping> load() {
        PropertiesMapping mxa310Mapping = new PropertiesMapping(
                ImmutableMap.<String, String>builder()
                        .put("BypassAllEq", "bypassAllEq")
                        .put("EqualizerFilters", "equalizerFilters.enabled")
                        .put("PasswordSet", "passwordSet")
                        .put("Mute", "audioMute")
                        .put("ActivePresetIndex", "activePresetIndex")
                        .put("FirmwareVersion", "firmwareVersion")
                        .put("Version", "version")
                        .put("Encryption", "danteEncryptionEnabled")
                        .put("EncryptionPassphraseSet", "danteEncryptionPassphraseSet")
                        .put("IpMode", "ipMode")
                        .put("IpAddress", "currentIpAddress")
                        .put("SubnetMask", "currentSubnetMask")
                        .put("Gateway", "currentGateway")
                        .put("Reboot", "Reboot")
                        .put("Reset", "Reset")
                        .build(),
                ImmutableMap.<String, String>builder()
                        .put("DeviceNameConflict", "danteDeviceNameConflict")
                        .put("ClockSyncError", "danteClockSyncError")
                        .put("Uptime", "uptime")
                        .build(),
                ImmutableMap.<String, String>builder()
                        .put("deviceModel", "model")
                        .put("deviceId", "hardwareId")
                        .put("deviceName", "deviceName")
                        .put("serialNumber", "serialNumber")
                        .put("macAddresses", "macAddress")
                        .build(),
                ImmutableMap.<String, String>builder()
                        .put("deviceType", "Microphone")
                        .put("deviceOnline", "true")
                        .put("deviceMake", "Shure")
                        .build(),
                ImmutableMap.<String, String>builder()
                        .put("Mute", "Toggle")
                        .put("Encryption", "Toggle")
                        .put("EnableLowCutFilter", "Toggle")
                        .put("Reboot", "Push")
                        .put("Reset", "Push")
                        .build());

        PropertiesMapping mxa910Mapping = new PropertiesMapping(
                ImmutableMap.<String, String>builder()
                        .put("BypassAllEq", "bypassAllEq")
                        .put("PasswordSet", "passwordSet")
                        .put("Mute", "audioMute")
                        .put("ActivePresetIndex", "activePresetIndex")
                        .put("FirmwareVersion", "firmwareVersion")
                        .put("Version", "version")
                        .put("Encryption", "danteEncryptionEnabled")
                        .put("EncryptionPassphraseSet", "danteEncryptionPassphraseSet")
                        .put("IpMode", "ipMode")
                        .put("IpAddress", "currentIpAddress")
                        .put("SubnetMask", "currentSubnetMask")
                        .put("Gateway", "currentGateway")
                        .put("Reboot", "Reboot")
                        .put("Reset", "Reset")
                        .build(),
                ImmutableMap.<String, String>builder()
                        .put("DeviceNameConflict", "danteDeviceNameConflict")
                        .put("ClockSyncError", "danteClockSyncError")
                        .put("Uptime", "uptime")
                        .build(),
                ImmutableMap.<String, String>builder()
                        .put("deviceModel", "model")
                        .put("deviceId", "hardwareId")
                        .put("deviceName", "deviceName")
                        .put("serialNumber", "serialNumber")
                        .put("macAddresses", "macAddress")
                        .build(),
                ImmutableMap.<String, String>builder()
                        .put("deviceType", "Microphone")
                        .put("deviceOnline", "true")
                        .put("deviceMake", "Shure")
                        .build(),
                ImmutableMap.<String, String>builder()
                        .put("Mute", "Toggle")
                        .put("Encryption", "Toggle")
                        .put("Reboot", "Push")
                        .put("Reset", "Push")
                        .build());

        return ImmutableMap.of("default", mxa310Mapping, "MXA310", mxa310Mapping, "MXA910", mxa910Mapping);
    }
}
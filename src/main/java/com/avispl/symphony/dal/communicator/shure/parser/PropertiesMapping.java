/*
 * Copyright (c) 2019 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.shure.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * Description of shure model properties
 */
public class PropertiesMapping {


    private final Map<String, String> properties;
    private final Map<String, String> statistics;
    private final Map<String, String> deviceProperties;
    private final Map<String, String> predefinedProperties;
    private final Map<String, String> controlProperties;


    /**
     * Constructs the statistic mapping holder containing the {@code properties, statistics, deviceProperties, hardcodedProperties, control}
     *
     * @param properties           mapping
     * @param statistics           mapping
     * @param deviceProperties     mapping
     * @param predefinedProperties mapping
     * @param controlProperties    mapping
     */
    public PropertiesMapping(Map<String, String> properties,
                             Map<String, String> statistics,
                             Map<String, String> deviceProperties,
                             Map<String, String> predefinedProperties,
                             Map<String, String> controlProperties) {
        this.properties = properties;
        this.statistics = statistics;
        this.deviceProperties = deviceProperties;
        this.predefinedProperties = predefinedProperties;
        this.controlProperties = controlProperties;


    }

    @Override
    public String toString() {
        return String.format("PropertiesMapping {%n" +
                        "controlProperties %s%n" +
                        "deviceProperties %s%n" +
                        "predefinedProperties %s%n" +
                        "properties %s%n" +
                        "statistics %s%n" +
                        "}",
                Arrays.toString(controlProperties.entrySet().toArray()),
                Arrays.toString(deviceProperties.entrySet().toArray()),
                Arrays.toString(predefinedProperties.entrySet().toArray()),
                Arrays.toString(properties.entrySet().toArray()),
                Arrays.toString(statistics.entrySet().toArray()));
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public Map<String, String> getStatistics() {
        return Collections.unmodifiableMap(statistics);
    }

    public Map<String, String> getDeviceProperties() {
        return Collections.unmodifiableMap(deviceProperties);
    }

    public Map<String, String> getPredefinedProperties() {
        return Collections.unmodifiableMap(predefinedProperties);
    }

    public Map<String, String> getControlProperties() {
        return Collections.unmodifiableMap(controlProperties);
    }
}

/*
 * Copyright (c) 2019-2020 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.shure.parser;

import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class AggregatedDeviceProcessor {
    private final static Log log = LogFactory.getLog(AggregatedDeviceProcessor.class);

    private final BeanUtilsBean beanUtilsBean;
    private final PropertiesMapping propertiesMapping;

    public AggregatedDeviceProcessor(PropertiesMapping propertiesMapping) {
        this.propertiesMapping = propertiesMapping;
        this.beanUtilsBean = createBeanUtilsBean();
    }

    /**
     * Create bean utils bean with custom converter
     *
     * @return beanutils bean
     */
    private BeanUtilsBean createBeanUtilsBean() {
        BeanUtilsBean beanUtilsBean = new BeanUtilsBean();
        beanUtilsBean.getConvertUtils().register(new Converter() {
            @Override
            public <T> T convert(Class<T> type, Object value) {
                return (T) Collections.singletonList(value);
            }
        }, List.class);
        return beanUtilsBean;
    }

    /**
     * Set all of the {@code device} properties with {@code node} values
     * accordingly to {@code propertiesMapping} mapping
     *
     * @param device which properties to be set on
     * @param node   json where properties to get from
     */
    public void applyProperties(AggregatedDevice device, JsonNode node) {
        processDeviceProperties(node, device, propertiesMapping.getDeviceProperties());
        processPredefinedProperties(device, propertiesMapping.getPredefinedProperties());

        device.setProperties(parseProperties(node, propertiesMapping.getProperties()));
        device.setStatistics(parseProperties(node, propertiesMapping.getStatistics()));
        device.setControl(new HashMap<>(propertiesMapping.getControlProperties()));
    }

    /**
     * Find value by complex json path
     *
     * @param node json node
     * @param path path to node
     * @return node value
     */
    private String findPath(JsonNode node, String path) {
        String[] nodeNames = path.split("\\.");
        for (String nodeName : nodeNames) {
            List<JsonNode> nodeParents = node.findParents(nodeName);
            for (JsonNode nodes : nodeParents) {
                String value = nodes.findPath(nodeName).asText();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return node.asText();
    }

    /**
     * @param node    json node
     * @param mapping map of json path to property name
     * @return map of property names with
     */
    private Map<String, String> parseProperties(JsonNode node, Map<String, String> mapping) {
        return mapping.entrySet().stream().collect(toMap(
                Map.Entry::getKey,
                entry -> findPath(node, entry.getValue())));
    }

    /**
     * Process device bean properties
     *
     * @param node             json node
     * @param device           aggregated device
     * @param deviceProperties properties mapping
     */
    private void processDeviceProperties(JsonNode node, AggregatedDevice device, Map<String, String> deviceProperties) {
        deviceProperties.forEach((key, value) -> {
            try {
                beanUtilsBean.setProperty(device, key, findPath(node, value));
            } catch (InvocationTargetException | IllegalAccessException e) {
                log.error("Failed to applyProperties: " + key, e);
            }
        });
    }

    /**
     * Process device bean predefined properties
     *
     * @param device           aggregated device
     * @param deviceProperties device properties mapping
     */
    private void processPredefinedProperties(AggregatedDevice device, Map<String, String> deviceProperties) {
        deviceProperties.forEach((key, value) -> {
            try {
                beanUtilsBean.setProperty(device, key, value);
            } catch (InvocationTargetException | IllegalAccessException e) {
                log.error("Failed to applyProperties: " + key, e);
            }
        });
    }
}
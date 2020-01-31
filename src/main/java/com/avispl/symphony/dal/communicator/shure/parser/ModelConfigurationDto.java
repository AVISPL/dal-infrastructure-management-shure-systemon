/*
 * Copyright (c) 2019 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.shure.parser;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ModelConfigurationDto {

    @XmlElement(name = "model")
    private List<Model> models = new ArrayList<>();

    public List<Model> getModels() {
        return models;
    }

    public void setModels(List<Model> models) {
        this.models = models;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Model {

        @XmlAttribute
        private String vendor;
        @XmlAttribute
        private String name;

        @XmlElementWrapper(name = "mapping")
        @XmlElement(name = "property")
        private List<Property> mappings = new ArrayList<>();

        @XmlElementWrapper(name = "properties")
        @XmlElement(name = "property")
        private List<Property> properties = new ArrayList<>();

        @XmlElementWrapper(name = "control")
        @XmlElement(name = "property")
        private List<Property> controls = new ArrayList<>();

        @XmlElementWrapper(name = "statistic")
        @XmlElement(name = "property")
        private List<Property> statistic = new ArrayList<>();

        public String getVendor() {
            return vendor;
        }

        public void setVendor(String vendor) {
            this.vendor = vendor;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Property> getMappings() {
            return mappings;
        }

        public void setMappings(List<Property> mappings) {
            this.mappings = mappings;
        }

        public List<Property> getProperties() {
            return properties;
        }

        public void setProperties(List<Property> properties) {
            this.properties = properties;
        }

        public List<Property> getControls() {
            return controls;
        }

        public void setControls(List<Property> controls) {
            this.controls = controls;
        }

        public List<Property> getStatistic() {
            return statistic;
        }

        public void setStatistic(List<Property> statistic) {
            this.statistic = statistic;
        }

        @Override
        public String toString() {
            return "vendor: " + vendor +
                    "name: " + name +
                    "mappings: " + Arrays.toString(mappings.toArray()) +
                    ", properties: " + Arrays.toString(properties.toArray()) +
                    ", controls: " + Arrays.toString(controls.toArray()) +
                    ", statistic: " + Arrays.toString(statistic.toArray());
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Property {
        @XmlAttribute(name = "name", required = true)
        private String name;

        @XmlAttribute
        private String value;

        @XmlAttribute
        private String ref;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getRef() {
            return ref;
        }

        public void setRef(String ref) {
            this.ref = ref;
        }

        @Override
        public String toString() {
            return "{name='" + name + "'" +
                    ", value='" + value + "'" +
                    ", ref='" + ref + "'}";
        }
    }


    @Override
    public String toString() {
        return "ModelConfigurationDto: " + models.stream().map(Model::toString).collect(Collectors.joining("\n"));
    }
}

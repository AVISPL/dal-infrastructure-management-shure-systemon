package com.avispl.symphony.communicator.shure;

import com.avispl.symphony.dal.communicator.shure.parser.PropertiesMapping;
import com.avispl.symphony.dal.communicator.shure.parser.PropertiesMappingParser;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class ShureMappingParserTest {

    private static final String testXmlBasePath = "shure_beta/";
    private PropertiesMappingParser propertiesMappingParser;

    @Before
    public void setUp() {
        propertiesMappingParser = new PropertiesMappingParser();
    }

    @Test
    public void validGenericMappingTest() throws Exception {
        Map<String, PropertiesMapping> mapping = propertiesMappingParser.load(testXmlBasePath + "mappings-valid-test.xml");

        assertThat(mapping)
                .containsKeys("generic");

        assertThat(mapping.get("generic").getPredefinedProperties())
                .contains(entry("mapping.property.predefined", "mapping.value"));

        assertThat(mapping.get("generic").getDeviceProperties())
                .contains(entry("mapping.property.device", "mapping.reference"));

        assertThat(mapping.get("generic").getProperties())
                .contains(entry("properties.property", "properties.reference"));

        assertThat(mapping.get("generic").getControlProperties())
                .contains(entry("control.property", "control.value"));

        assertThat(mapping.get("generic").getStatistics())
                .contains(entry("statistic.property", "statistic.reference"));
    }

    @Test
    public void validOverriddenMappingTest() throws Exception {
        Map<String, PropertiesMapping> mapping = propertiesMappingParser.load(testXmlBasePath + "mappings-valid-test.xml");

        assertThat(mapping)
                .containsKeys("generic", "overridden");

        assertThat(mapping.get("overridden").getPredefinedProperties())
                .contains(entry("mapping.property.predefined", "overridden.mapping.value"));

        assertThat(mapping.get("overridden").getDeviceProperties())
                .contains(entry("mapping.property.device", "overridden.mapping.reference"));

        assertThat(mapping.get("overridden").getProperties())
                .contains(entry("properties.property", "overridden.properties.reference"));

        assertThat(mapping.get("overridden").getControlProperties())
                .contains(entry("control.property", "overridden.control.value"));

        assertThat(mapping.get("overridden").getStatistics())
                .contains(entry("statistic.property", "overridden.statistic.reference"));
    }

    /**
     * Test obligatory
     *
     * @throws Exception
     */
    @Test(expected = RuntimeException.class)
    public void genericNotExistsTest() throws Exception {
        Map<String, PropertiesMapping> mapping = propertiesMappingParser.load(testXmlBasePath + "mapping-generic-not-exists-test.xml");

    }
}
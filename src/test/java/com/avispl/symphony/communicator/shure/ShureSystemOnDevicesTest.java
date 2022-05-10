/*
 * Copyright (c) 2019-2022 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.communicator.shure;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.dal.communicator.shure.ShureSystemOn;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Tag;

@Tag("unit")
public class ShureSystemOnDevicesTest {

    private ShureSystemOn shureSystemOn;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.DYNAMIC_PORT);

    @Before
    public void setUp() throws Exception {
        shureSystemOn = new ShureSystemOn();
        shureSystemOn.setHost("localhost");
        shureSystemOn.setPort(wireMockRule.port());
        shureSystemOn.init();
    }

    private static String resource(String s) throws IOException {
        return Resources.toString(getResource(s), UTF_8);
    }

    @Test
    public void shureSystemOnAniusbTest() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/ANIUSB.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        Assert.assertEquals(2, devices.size());
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        Assert.assertEquals("ANIUSB", device.getDeviceModel());
        Assert.assertEquals(0, device.getControl().size());
        Assert.assertEquals(4, device.getControllableProperties().size());
        Assert.assertEquals(34, device.getProperties().size());
    }

    @Test
    public void shureSystemOnImxRoomTest() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/IMX-Room.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        Assert.assertEquals(3, devices.size());
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        Assert.assertEquals("IMX-Room", device.getDeviceModel());
        Assert.assertEquals(0, device.getControl().size());
        Assert.assertEquals(3, device.getControllableProperties().size());
        Assert.assertEquals(31, device.getProperties().size());
    }

    @Test
    public void shureSystemOnMXA310Test() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/MXA310.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        Assert.assertEquals(1, devices.size());
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        Assert.assertEquals("MXA310", device.getDeviceModel());
        Assert.assertEquals(0, device.getControl().size());
        Assert.assertEquals(4, device.getControllableProperties().size());
        Assert.assertEquals(40, device.getProperties().size());
    }

    @Test
    public void shureSystemOnMXA910Test() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/MXA910.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        Assert.assertEquals(3, devices.size());
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        Assert.assertEquals("MXA910", device.getDeviceModel());
        Assert.assertEquals(0, device.getControl().size());
        Assert.assertEquals(4, device.getControllableProperties().size());
        Assert.assertEquals(36, device.getProperties().size());
    }

    @Test
    public void shureSystemOnMXWAPT8Test() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/MXWAPT8.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        Assert.assertEquals(2, devices.size());
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        Assert.assertEquals("MXWAPT8", device.getDeviceModel());
        Assert.assertEquals(0, device.getControl().size());
        Assert.assertEquals(0, device.getControllableProperties().size());
        Assert.assertEquals(20, device.getProperties().size());
    }

    @Test
    public void shureSystemOnMXWNCS4Test() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/MXWNCS4.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        Assert.assertEquals(2, devices.size());
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        Assert.assertEquals("MXWNCS4", device.getDeviceModel());
        Assert.assertEquals(0, device.getControl().size());
        Assert.assertEquals(0, device.getControllableProperties().size());
        Assert.assertEquals(12, device.getProperties().size());
    }

    @Test
    public void shureSystemOnP300Test() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/P300.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        Assert.assertEquals(2, devices.size());
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        Assert.assertEquals("P300", device.getDeviceModel());
        Assert.assertEquals(0, device.getControl().size());
        Assert.assertEquals(4, device.getControllableProperties().size());
        Assert.assertEquals(33, device.getProperties().size());
    }

    @Test
    public void shureSystemOnSBC850Test() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/SBC850.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        Assert.assertEquals(1, devices.size());
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        Assert.assertEquals("SBC850", device.getDeviceModel());
        Assert.assertEquals(0, device.getControl().size());
        Assert.assertEquals(0, device.getControllableProperties().size());
        Assert.assertEquals(12, device.getProperties().size());
    }

    @Test
    public void shureSystemOnSCM820DANTest() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/SCM820-DAN.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        Assert.assertEquals(1, devices.size());
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        Assert.assertEquals("SCM820-DAN", device.getDeviceModel());
        Assert.assertEquals(0, device.getControl().size());
        Assert.assertEquals(0, device.getControllableProperties().size());
        Assert.assertEquals(27, device.getProperties().size());
    }

    private void checkForEmptyFields(AggregatedDevice device) {
        Assert.assertNotNull(device.getProperties());
        Assert.assertNotEquals(0, device.getProperties().size());
        Assert.assertNotEquals("", device.getProperties().get("FirmwareVersion"));
        Assert.assertNotEquals("", device.getProperties().get("DeviceVersion"));
    }
}

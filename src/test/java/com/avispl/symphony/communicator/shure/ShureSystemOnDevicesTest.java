package com.avispl.symphony.communicator.shure;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.dal.communicator.shure.ShureSystemOn;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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
        assertThat(devices).isNotEmpty().hasSize(2);
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        assertThat(device.getDeviceModel()).isEqualTo("ANIUSB");
        assertThat(device.getControl().size()).isEqualTo(0);
        assertThat(device.getControllableProperties().size()).isEqualTo(4);
        assertThat(device.getProperties().size()).isEqualTo(34);
    }

    @Test
    public void shureSystemOnImxRoomTest() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/IMX-Room.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        assertThat(devices).isNotEmpty().hasSize(3);
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        assertThat(device.getDeviceModel()).isEqualTo("IMX-Room");
        assertThat(device.getControl().size()).isEqualTo(0);
        assertThat(device.getControllableProperties().size()).isEqualTo(3);
        assertThat(device.getProperties().size()).isEqualTo(31);
    }

    @Test
    public void shureSystemOnMXA310Test() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/MXA310.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        assertThat(devices).isNotEmpty().hasSize(1);
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        assertThat(device.getDeviceModel()).isEqualTo("MXA310");
        assertThat(device.getControl().size()).isEqualTo(0);
        assertThat(device.getControllableProperties().size()).isEqualTo(4);
        assertThat(device.getProperties().size()).isEqualTo(40);
    }

    @Test
    public void shureSystemOnMXA910Test() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/MXA910.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        assertThat(devices).isNotEmpty().hasSize(3);
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        assertThat(device.getDeviceModel()).isEqualTo("MXA910");
        assertThat(device.getControl().size()).isEqualTo(0);
        assertThat(device.getControllableProperties().size()).isEqualTo(4);
        assertThat(device.getProperties().size()).isEqualTo(36);
    }

    @Test
    public void shureSystemOnMXWAPT8Test() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/MXWAPT8.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        assertThat(devices).isNotEmpty().hasSize(2);
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        assertThat(device.getDeviceModel()).isEqualTo("MXWAPT8");
        assertThat(device.getControl().size()).isEqualTo(0);
        assertThat(device.getControllableProperties().size()).isEqualTo(0);
        assertThat(device.getProperties().size()).isEqualTo(20);
    }

    @Test
    public void shureSystemOnMXWNCS4Test() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/MXWNCS4.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        assertThat(devices).isNotEmpty().hasSize(2);
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        assertThat(device.getDeviceModel()).isEqualTo("MXWNCS4");
        assertThat(device.getControl().size()).isEqualTo(0);
        assertThat(device.getControllableProperties().size()).isEqualTo(0);
        assertThat(device.getProperties().size()).isEqualTo(12);
    }

    @Test
    public void shureSystemOnP300Test() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/P300.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        assertThat(devices).isNotEmpty().hasSize(2);
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        assertThat(device.getDeviceModel()).isEqualTo("P300");
        assertThat(device.getControl().size()).isEqualTo(0);
        assertThat(device.getControllableProperties().size()).isEqualTo(4);
        assertThat(device.getProperties().size()).isEqualTo(33);
    }

    @Test
    public void shureSystemOnSBC850Test() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/SBC850.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        assertThat(devices).isNotEmpty().hasSize(1);
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        assertThat(device.getDeviceModel()).isEqualTo("SBC850");
        assertThat(device.getControl().size()).isEqualTo(0);
        assertThat(device.getControllableProperties().size()).isEqualTo(0);
        assertThat(device.getProperties().size()).isEqualTo(12);
    }

    @Test
    public void shureSystemOnSCM820DANTest() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api/v1.0/devices")))
            .setResponse(okJson(resource("shure/responces/SCM820-DAN.json")).build());

        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        assertThat(devices).isNotEmpty().hasSize(1);
        AggregatedDevice device = devices.get(0);

        checkForEmptyFields(device);
        assertThat(device.getDeviceModel()).isEqualTo("SCM820-DAN");
        assertThat(device.getControl().size()).isEqualTo(0);
        assertThat(device.getControllableProperties().size()).isEqualTo(0);
        assertThat(device.getProperties().size()).isEqualTo(27);
    }

    private void checkForEmptyFields(AggregatedDevice device) {
        assertThat(device).hasNoNullFieldsOrPropertiesExcept("aviSplAssetId", "ownerAssetId", "monitoredStatistics",
            "controllableProperties");
        assertThat(device.getProperties()).isNotEmpty();
        assertThat(device.getProperties().get("FirmwareVersion")).isNotEmpty();
        assertThat(device.getProperties().get("DeviceVersion")).isNotEmpty();
    }
}

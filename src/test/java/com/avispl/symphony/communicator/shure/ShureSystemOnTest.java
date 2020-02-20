package com.avispl.symphony.communicator.shure;

import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.dal.communicator.shure.ShureSystemOn;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class ShureSystemOnTest {

    @Rule
    public WireMockRule service = new WireMockRule(WireMockConfiguration.DYNAMIC_PORT);

    private ShureSystemOn shureSystemOn;

    private static String resource(String s) throws IOException {
        return Resources.toString(getResource(s), UTF_8);
    }

    @Before
    public void setUp() throws Exception {
        service.stubFor(get(urlEqualTo("/api/v1.0/devices")))
                .setResponse(okJson(resource("shure/devices-response.json")).build());

        service.stubFor(put(urlMatching(".*/api/v1.0/devices/.*"))).setResponse(ok().build());
        service.stubFor(post(urlMatching(".*/api/v1.0/devices/.*"))).setResponse(ok().build());
        service.stubFor(patch(urlMatching(".*/api/v1.0/devices/.*"))).setResponse(ok().build());

        shureSystemOn = new ShureSystemOn();
        shureSystemOn.setHost("localhost");
        shureSystemOn.setPort(service.port());
        shureSystemOn.setPassword("test");
        shureSystemOn.init();
    }

    @Test
    public void retrieveMultipleStatisticsTest() throws Exception {
        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();

        assertThat(devices)
                .isNotEmpty()
                .hasSize(9);

        AggregatedDevice device = devices.get(0);

        assertThat(device).hasNoNullFieldsOrPropertiesExcept("aviSplAssetId", "ownerAssetId", "monitoredStatistics");
        assertThat(device.getProperties()).isNotEmpty();
        assertThat(device.getStatistics()).isNotEmpty();
        assertThat(device.getControl()).isNotEmpty();
        assertThat(device.getProperties().get("Encryption")).isNotEmpty();
    }

    @Test
    public void muteOnTest() throws Exception {
        String deviceId = shureSystemOn.retrieveMultipleStatistics().get(0).getDeviceId();
        shureSystemOn.controlProperty(new ControllableProperty("Mute", 1, deviceId));

        service.verify(patchRequestedFor(urlEqualTo("/api/v1.0/devices/" + deviceId + "/audio/mute"))
                .withRequestBody(matchingJsonPath("$.muteState", equalTo("true"))));
    }

    @Test
    public void muteOffTest() throws Exception {
        String deviceId = shureSystemOn.retrieveMultipleStatistics().get(0).getDeviceId();
        shureSystemOn.controlProperty(new ControllableProperty("Mute", 0, deviceId));

        service.verify(patchRequestedFor(urlEqualTo("/api/v1.0/devices/" + deviceId + "/audio/mute"))
                .withRequestBody(matchingJsonPath("$.muteState", equalTo("false"))));
    }

    @Test
    public void encryptionOnTest() throws Exception {
        String deviceId = shureSystemOn.retrieveMultipleStatistics().get(0).getDeviceId();
        shureSystemOn.controlProperty(new ControllableProperty("Encryption", 1, deviceId));

        service.verify(patchRequestedFor(urlEqualTo("/api/v1.0/devices/" + deviceId + "/encryption/audio/enable")));
    }

    @Test
    public void encryptionOffTest() throws Exception {
        String deviceId = shureSystemOn.retrieveMultipleStatistics().get(0).getDeviceId();
        shureSystemOn.controlProperty(new ControllableProperty("Encryption", 0, deviceId));

        service.verify(patchRequestedFor(urlEqualTo("/api/v1.0/devices/" + deviceId + "/encryption/audio/disable")));
    }

    @Test
    public void resetTest() throws Exception {
        String deviceId = shureSystemOn.retrieveMultipleStatistics().get(0).getDeviceId();
        shureSystemOn.controlProperty(new ControllableProperty("Reset", null, deviceId));

        service.verify(postRequestedFor(urlEqualTo("/api/v1.0/devices/" + deviceId + "/maintenance/defaultsreset")));
    }

    @Test
    public void rebootTest() throws Exception {
        String deviceId = shureSystemOn.retrieveMultipleStatistics().get(0).getDeviceId();
        shureSystemOn.controlProperty(new ControllableProperty("Reboot", null, deviceId));

        service.verify(postRequestedFor(urlEqualTo("/api/v1.0/devices/" + deviceId + "/maintenance/reboot")));
    }

    @Test
    public void automixerBypassTest() throws Exception {
        String deviceId = shureSystemOn.retrieveMultipleStatistics().get(0).getDeviceId();
        shureSystemOn.controlProperty(new ControllableProperty("BypassAllEq", null, deviceId));

        service.verify(putRequestedFor(urlEqualTo("/api/v1.0/devices/" + deviceId + "/automixer/bypass")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void controlPropertiesTest() throws Exception {
        shureSystemOn.controlProperties(Collections.emptyList());
        failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    }
}

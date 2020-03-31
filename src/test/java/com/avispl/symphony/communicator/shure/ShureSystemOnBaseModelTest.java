package com.avispl.symphony.communicator.shure;

import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.dal.communicator.shure.ShureSystemOn;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class ShureSystemOnBaseModelTest {

    @Rule
    public WireMockRule service = new WireMockRule(WireMockConfiguration.DYNAMIC_PORT);
    private ShureSystemOn shureSystemOn;
    String modelName;
    String jsonName;
    int devicesSize;
    int controlsSize;
    int propertiesSize;

    private static String resource(String s) throws IOException {
        return Resources.toString(getResource(s), UTF_8);
    }

    protected abstract void init();

    @Before
    public void setUp() throws Exception {
        init();
        service.stubFor(get(urlEqualTo("/api/v1.0/devices")))
                .setResponse(okJson(resource(jsonName)).build());
        service.stubFor(post(urlMatching(".*/api/v1.0/devices/.*"))).setResponse(ok().build());

        shureSystemOn = new ShureSystemOn();
        shureSystemOn.setHost("localhost");
        shureSystemOn.setPort(service.port());
        shureSystemOn.setPassword("test");
        shureSystemOn.init();
    }

    @Test
    public void retrieveMultipleStatisticsTest() throws Exception {
        List<AggregatedDevice> devices = shureSystemOn.retrieveMultipleStatistics();
        assertThat(devices).isNotEmpty().hasSize(devicesSize);
        AggregatedDevice device = devices.get(0);
        assertThat(device).hasNoNullFieldsOrPropertiesExcept("aviSplAssetId", "ownerAssetId", "monitoredStatistics", "controllableProperties");
        assertThat(device.getProperties()).isNotEmpty();
        assertThat(device.getProperties().get("FirmwareVersion")).isNotEmpty();
        assertThat(device.getProperties().get("DeviceVersion")).isNotEmpty();
        assertThat(device.getDeviceModel()).isEqualTo(modelName);
        assertThat(device.getControl().size()).isEqualTo(controlsSize);
        assertThat(device.getProperties().size()).isEqualTo(propertiesSize);
    }
}
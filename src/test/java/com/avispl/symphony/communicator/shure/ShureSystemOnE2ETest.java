/*
 * Copyright (c) 2022 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.communicator.shure;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.dal.communicator.shure.ShureSystemOn;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("integration")
public class ShureSystemOnE2ETest {
    private ShureSystemOn shureSystemOn;

    @Before
    public void setUp() throws Exception {
        shureSystemOn = new ShureSystemOn();
        shureSystemOn.setHost("***REMOVED***");
        shureSystemOn.setPort(10000);
        shureSystemOn.setPassword("test");
    }

    @Test
    public void testRetrieveAggregatedDevicesWithModelFilters() throws Exception {
        shureSystemOn.setDeviceModelFilter("MXA910");
        shureSystemOn.init();
        shureSystemOn.retrieveMultipleStatistics();
        Thread.sleep(30000);
        List<AggregatedDevice> aggregatedDeviceList = shureSystemOn.retrieveMultipleStatistics();
        Assert.notNull(aggregatedDeviceList, "Aggregated devices list cannot be null");
        assertEquals(1, aggregatedDeviceList.size());
    }

    @Test
    public void testRetrieveAggregatedDevicesWithHardwareIdFilter() throws Exception {
        shureSystemOn.setHardwareIdFilter("dd602237-0000-11dd-a000-000eddcccccc,d45ddf1f-840c-11e9-a000-000eddaaaaaa, d45ddf1f-840c-11e9-a000-");
        shureSystemOn.init();
        shureSystemOn.retrieveMultipleStatistics();
        Thread.sleep(30000);
        List<AggregatedDevice> aggregatedDeviceList = shureSystemOn.retrieveMultipleStatistics();
        List<Statistics> multipleStatistics = shureSystemOn.getMultipleStatistics();
        Assert.notNull(aggregatedDeviceList, "Aggregated devices list cannot be null");
        Assert.notEmpty(aggregatedDeviceList, "Aggregated devices list cannot be empty");
        Assert.notNull(multipleStatistics, "Multiple Statistics cannot be null");
        Assert.notEmpty(multipleStatistics, "Multiple Statistics cannot be null");
        assertEquals(2, aggregatedDeviceList.size());
        assertEquals("Error response received from: ***REMOVED***. Request: http://***REMOVED***:10000/api/v1.0/devices/ d45ddf1f-840c-11e9-a0...",
                ((ExtendedStatistics)multipleStatistics.get(0)).getStatistics().get("Errors#CommandFailureException[ d45ddf1f-840c-11e9-a000-]"));
    }

    @Test
    public void testRetrieveAggregatedDevicesWithHardwareIdFilterAndModelNameFilter() throws Exception {
        shureSystemOn.setDeviceModelFilter("MXA910");
        shureSystemOn.setHardwareIdFilter("dd602237-0000-11dd-a000-000eddcccccc,d45ddf1f-840c-11e9-a000-000eddaaaaaa, d45ddf1f-840c-11e9-a000-");
        shureSystemOn.init();
        shureSystemOn.retrieveMultipleStatistics();
        Thread.sleep(30000);
        List<AggregatedDevice> aggregatedDeviceList = shureSystemOn.retrieveMultipleStatistics();
        List<Statistics> multipleStatistics = shureSystemOn.getMultipleStatistics();
        Assert.notNull(aggregatedDeviceList, "Aggregated devices list cannot be null");
        Assert.notEmpty(aggregatedDeviceList, "Aggregated devices list cannot be empty");
        Assert.notNull(multipleStatistics, "Multiple Statistics cannot be null");
        Assert.notEmpty(multipleStatistics, "Multiple Statistics cannot be null");
        assertEquals(1, aggregatedDeviceList.size());
        assertEquals("Error response received from: ***REMOVED***. Request: http://***REMOVED***:10000/api/v1.0/devices/ d45ddf1f-840c-11e9-a0...",
                ((ExtendedStatistics)multipleStatistics.get(0)).getStatistics().get("Errors#CommandFailureException[ d45ddf1f-840c-11e9-a000-]"));
    }

    @Test
    public void testGetMultipleStatisticsWithErrors() throws Exception {
        shureSystemOn.setHost("132.31.254.17");
        shureSystemOn.setDeviceModelFilter("MXA910");
        shureSystemOn.init();
        shureSystemOn.retrieveMultipleStatistics();
        Thread.sleep(30000);
        List<Statistics> aggregatedDeviceList = shureSystemOn.getMultipleStatistics();
        new ArrayList<>(((ExtendedStatistics) aggregatedDeviceList.get(0)).getStatistics().keySet()).contains("Errors#ResourceNotReachableException");
        Assert.notNull(aggregatedDeviceList, "Aggregated devices list cannot be null");
    }
}

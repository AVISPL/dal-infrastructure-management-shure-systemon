package com.avispl.symphony.communicator.shure;

public class ShureSystemOnP300Test extends ShureSystemOnBaseModelTest {

    @Override
    protected void init() {
        jsonName = "shure/responces/P300.json";
        devicesSize = 2;
        modelName = "P300";
        controlsSize = 0;
        controllablePropertiesSize = 4;
        propertiesSize = 33;
    }
}
package com.avispl.symphony.communicator.shure;

public class ShureSystemOnMXA310Test extends ShureSystemOnBaseModelTest {

    @Override
    protected void init() {
        jsonName = "shure/responces/MXA310.json";
        devicesSize = 1;
        modelName = "MXA310";
        controlsSize = 0;
        controllablePropertiesSize = 4;
        propertiesSize = 40;
    }
}
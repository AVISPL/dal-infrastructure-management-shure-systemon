package com.avispl.symphony.communicator.shure;

public class ShureSystemOnMXA910Test extends ShureSystemOnBaseModelTest {

    @Override
    protected void init() {
        jsonName = "shure/responces/MXA910.json";
        devicesSize = 3;
        modelName = "MXA910";
        controlsSize = 4;
        propertiesSize = 36;
    }
}
package com.avispl.symphony.communicator.shure;

public class ShureSystemOnMxwapt8Test extends ShureSystemOnBaseModelTest {

    @Override
    protected void init() {
        jsonName = "shure/responces/MXWAPT8.json";
        devicesSize = 2;
        modelName = "MXWAPT8";
        controlsSize = 0;
        controllablePropertiesSize = 0;
        propertiesSize = 20;
    }
}
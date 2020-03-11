package com.avispl.symphony.communicator.shure;

public class ShureSystemOnMxwncs4Test extends ShureSystemOnBaseModelTest {

    @Override
    protected void init() {
        jsonName = "shure/responces/MXWNCS4.json";
        devicesSize = 2;
        modelName = "MXWNCS4";
        controlsSize = 0;
        propertiesSize = 12;
    }
}
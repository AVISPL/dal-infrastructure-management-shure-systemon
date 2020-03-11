package com.avispl.symphony.communicator.shure;

public class ShureSystemOnAniusbTest extends ShureSystemOnBaseModelTest {

    @Override
    protected void init() {
        jsonName = "shure/responces/ANIUSB.json";
        devicesSize = 2;
        modelName = "ANIUSB";
        controlsSize = 4;
        propertiesSize = 34;
    }
}
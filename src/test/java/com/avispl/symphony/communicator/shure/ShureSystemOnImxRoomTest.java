package com.avispl.symphony.communicator.shure;

public class ShureSystemOnImxRoomTest extends ShureSystemOnBaseModelTest {

    @Override
    protected void init() {
        jsonName = "shure/responces/IMX-Room.json";
        devicesSize = 3;
        modelName = "IMX-Room";
        controlsSize = 0;
        controllablePropertiesSize = 3;
        propertiesSize = 31;
    }
}
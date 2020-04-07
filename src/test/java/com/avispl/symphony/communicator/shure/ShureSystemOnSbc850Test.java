package com.avispl.symphony.communicator.shure;

public class ShureSystemOnSbc850Test extends ShureSystemOnBaseModelTest {

    @Override
    protected void init() {
        jsonName = "shure/responces/SBC850.json";
        devicesSize = 1;
        modelName = "SBC850";
        controlsSize = 0;
        controllablePropertiesSize = 0;
        propertiesSize = 12;
    }
}
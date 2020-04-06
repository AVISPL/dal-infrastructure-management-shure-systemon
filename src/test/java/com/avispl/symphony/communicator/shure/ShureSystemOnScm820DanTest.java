package com.avispl.symphony.communicator.shure;

public class ShureSystemOnScm820DanTest extends ShureSystemOnBaseModelTest {

    @Override
    protected void init() {
        jsonName = "shure/responces/SCM820-DAN.json";
        devicesSize = 1;
        modelName = "SCM820-DAN";
        controlsSize = 0;
        controllablePropertiesSize = 0;
        propertiesSize = 27;
    }
}
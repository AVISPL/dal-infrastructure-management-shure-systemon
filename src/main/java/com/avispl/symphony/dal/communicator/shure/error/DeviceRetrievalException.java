/*
 * Copyright (c) 2022 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.shure.error;

/**
 * Exception for internal Shure Adapter related data exchange processes
 *
 * @author Symphony Dev Team<br> Created on April 28, 2022
 * @since 5.5
 */
public class DeviceRetrievalException extends RuntimeException{
    /**
     * Class constructor with a single error message param
     *
     * @param message to include all necessary error details
     */
    public DeviceRetrievalException(String message) {
        super(message);
    }
}

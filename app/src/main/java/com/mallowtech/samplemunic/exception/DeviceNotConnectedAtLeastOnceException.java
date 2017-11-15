package com.mallowtech.samplemunic.exception;

public class DeviceNotConnectedAtLeastOnceException extends Exception {
    private static final long serialVersionUID = -5308517024624991810L;

    public DeviceNotConnectedAtLeastOnceException() {
        super("Device has not been connected at least once. Did you forget to use <connect /> tag?");
    }
}

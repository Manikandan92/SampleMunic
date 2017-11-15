package com.mallowtech.samplemunic.exception;

public class TargetInUseException extends Exception {
    private static final long serialVersionUID = 675809544705074232L;

    public TargetInUseException(String message) {
        super(message);
    }
}

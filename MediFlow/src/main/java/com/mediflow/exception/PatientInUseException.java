package com.mediflow.exception;

public class PatientInUseException extends RuntimeException {
    public PatientInUseException(String message) {
        super(message);
    }
}

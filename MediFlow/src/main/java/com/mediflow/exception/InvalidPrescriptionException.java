package com.mediflow.exception;

public class InvalidPrescriptionException extends RuntimeException {
    public InvalidPrescriptionException(String message) { super(message); }
    public InvalidPrescriptionException(String message, Throwable cause) { super(message, cause); }
}

package com.mediflow.exception;

public class DuplicatePatientException extends RuntimeException {
    public DuplicatePatientException(String message) { super(message); }
    public DuplicatePatientException(String message, Throwable cause) { super(message, cause); }
}

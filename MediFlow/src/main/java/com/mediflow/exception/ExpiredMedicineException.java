package com.mediflow.exception;

public class ExpiredMedicineException extends RuntimeException {
    public ExpiredMedicineException(String message) { super(message); }
    public ExpiredMedicineException(String message, Throwable cause) { super(message, cause); }
}

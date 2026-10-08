package com.mediflow.util;

import java.time.LocalDate;

/** Shared input validation helpers used by services before any database write. */
public final class ValidationUtil {
    private ValidationUtil() {}

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    public static void requireDobNotInFuture(LocalDate dob) {
        if (dob == null || dob.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }
    }

    public static void requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
    }
}

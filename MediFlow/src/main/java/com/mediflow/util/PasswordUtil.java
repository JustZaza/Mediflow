package com.mediflow.util;

import org.mindrot.jbcrypt.BCrypt;

/** Password hashing/verification. Never store or compare plaintext passwords (FR-01, section 13). */
public final class PasswordUtil {
    private PasswordUtil() {}

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public static boolean verify(String plainPassword, String hash) {
        return BCrypt.checkpw(plainPassword, hash);
    }
}

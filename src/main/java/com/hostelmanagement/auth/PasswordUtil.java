package com.hostelmanagement.auth;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hashPassword(String password) {
        if (password == null || password.length() < 10) {
            throw new IllegalArgumentException(
                    "Password must contain at least 10 characters."
            );
        }

        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public static boolean verifyPassword(
            String password,
            String passwordHash
    ) {
        if (password == null || passwordHash == null) {
            return false;
        }

        try {
            return BCrypt.checkpw(password, passwordHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
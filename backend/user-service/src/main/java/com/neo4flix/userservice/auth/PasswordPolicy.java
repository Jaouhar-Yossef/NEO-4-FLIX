package com.neo4flix.userservice.auth;

import java.nio.charset.StandardCharsets;
import java.util.Optional;


public final class PasswordPolicy {

    static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static Optional<String> check(String password) {
        if (password == null || password.length() < 8) {
            return Optional.of("password must be at least 8 characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            return Optional.of("password is too long");
        }
        if (!password.matches("(?s).*[A-Z].*")) {
            return Optional.of("password must contain an uppercase letter");
        }
        if (!password.matches("(?s).*[a-z].*")) {
            return Optional.of("password must contain a lowercase letter");
        }
        if (!password.matches("(?s).*[0-9].*")) {
            return Optional.of("password must contain a digit");
        }
        if (!password.matches("(?s).*[^A-Za-z0-9].*")) {
            return Optional.of("password must contain a symbol");
        }
        return Optional.empty();
    }
}

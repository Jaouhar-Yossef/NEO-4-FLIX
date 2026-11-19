package com.neo4flix.userservice.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PasswordPolicyTest {

    @Test
    void acceptsStrongPassword() {
        assertThat(PasswordPolicy.check("S3cure!pass")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "S3!a,          password must be at least 8 characters",
            "s3cure!pass,   password must contain an uppercase letter",
            "S3CURE!PASS,   password must contain a lowercase letter",
            "Secure!pass,   password must contain a digit",
            "S3curepass,    password must contain a symbol",
    })
    void rejectsWeakPassword(String password, String message) {
        assertThat(PasswordPolicy.check(password)).contains(message);
    }

    @Test
    void rejectsNullAndTooLong() {
        assertThat(PasswordPolicy.check(null)).isPresent();
        assertThat(PasswordPolicy.check("Aa1!" + "x".repeat(70))).contains("password is too long");
    }
}

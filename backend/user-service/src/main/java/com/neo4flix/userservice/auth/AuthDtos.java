package com.neo4flix.userservice.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "email is required")
            @Email(message = "email must be a valid email address")
            @Size(max = 254, message = "email is too long")
            String email,

            @NotBlank(message = "displayName is required")
            @Size(max = 50, message = "displayName must be at most 50 characters")
            String displayName,

            @NotNull(message = "password is required")
            String password) {
    }

    public record RegisterResponse(String id, String email, String displayName) {
    }

    public record LoginRequest(
            @NotBlank(message = "email is required") String email,
            @NotBlank(message = "password is required") String password) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LoginResponse(String token, Boolean twoFactorRequired, String tempToken) {
        static LoginResponse ofToken(String token) {
            return new LoginResponse(token, null, null);
        }

        static LoginResponse ofTwoFactor(String tempToken) {
            return new LoginResponse(null, true, tempToken);
        }
    }

    public record TwoFactorVerifyRequest(
            @NotBlank(message = "tempToken is required") String tempToken,
            @NotNull(message = "code is required")
            @Pattern(regexp = "\\d{6}", message = "code must be 6 digits") String code) {
    }

    public record CodeRequest(
            @NotNull(message = "code is required")
            @Pattern(regexp = "\\d{6}", message = "code must be 6 digits") String code) {
    }

    public record TokenResponse(String token) {
    }

    public record TwoFactorSetupResponse(String otpauthUri) {
    }
}

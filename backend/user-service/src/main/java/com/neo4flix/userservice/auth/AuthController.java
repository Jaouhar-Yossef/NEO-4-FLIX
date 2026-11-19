package com.neo4flix.userservice.auth;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.neo4flix.userservice.auth.AuthDtos.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest body) {
        return authService.register(body);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body) {
        return authService.login(body);
    }

    @PostMapping("/2fa/verify")
    public TokenResponse verify(@Valid @RequestBody TwoFactorVerifyRequest body) {
        return authService.verifyTwoFactor(body);
    }

    @PostMapping("/2fa/setup")
    public TwoFactorSetupResponse setup(@AuthenticationPrincipal String userId) {
        return authService.setupTwoFactor(userId);
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<Void> enable(@AuthenticationPrincipal String userId, @Valid @RequestBody CodeRequest body) {
        authService.enableTwoFactor(userId, body);
        return ResponseEntity.noContent().build();
    }
}

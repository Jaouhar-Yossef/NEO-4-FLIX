package com.neo4flix.userservice.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.neo4flix.userservice.auth.AuthDtos.*;
import com.neo4flix.userservice.security.JwtService;
import com.neo4flix.userservice.user.User;
import com.neo4flix.userservice.user.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BAD_CREDENTIALS = "invalid email or password";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TotpService totpService;

    public RegisterResponse register(RegisterRequest req) {
        PasswordPolicy.check(req.password()).ifPresent(message -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        });
        String email = normalize(req.email());
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
        }
        User user = new User();
        user.setEmail(email);
        user.setDisplayName(req.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole("USER");
        user.setTwoFactorEnabled(false);
        User saved = users.save(user);
        return new RegisterResponse(saved.getId(), saved.getEmail(), saved.getDisplayName());
    }

    public LoginResponse login(LoginRequest req) {
        if (req.password().getBytes(StandardCharsets.UTF_8).length > PasswordPolicy.MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS);
        }
        User user = users.findByEmail(normalize(req.email()))
                .filter(u -> u.getPasswordHash() != null)
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS));

        if (user.isTwoFactorEnabled()) {
            return LoginResponse.ofTwoFactor(jwtService.createTwoFactorToken(user));
        }
        return LoginResponse.ofToken(jwtService.createAccessToken(user));
    }

    public TokenResponse verifyTwoFactor(TwoFactorVerifyRequest req) {
        User user = jwtService.parse(req.tempToken(), JwtService.TYPE_TWO_FACTOR)
                .flatMap(claims -> users.findById(claims.getSubject()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login expired, sign in again"));
        if (!totpService.isValid(user.getTotpSecret(), req.code())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid code");
        }
        return new TokenResponse(jwtService.createAccessToken(user));
    }

    /** Step 1: store a new secret (not active yet) and give the app its otpauth:// URI. */
    public TwoFactorSetupResponse setupTwoFactor(String userId) {
        User user = currentUser(userId);
        if (user.isTwoFactorEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "two-factor authentication is already enabled");
        }
        String secret = totpService.newSecret();
        user.setTotpSecret(secret);
        users.save(user);
        return new TwoFactorSetupResponse(totpService.otpauthUri(user.getEmail(), secret));
    }

    /** Step 2: a correct code proves the app was set up; only then is 2FA turned on. */
    public void enableTwoFactor(String userId, CodeRequest req) {
        User user = currentUser(userId);
        if (user.isTwoFactorEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "two-factor authentication is already enabled");
        }
        if (user.getTotpSecret() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "call /auth/2fa/setup first");
        }
        if (!totpService.isValid(user.getTotpSecret(), req.code())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid code");
        }
        user.setTwoFactorEnabled(true);
        users.save(user);
    }

    private User currentUser(String userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

package com.neo4flix.userservice.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.neo4flix.userservice.user.User;


@Service
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_TWO_FACTOR = "2fa";
    private static final Duration TWO_FACTOR_TTL = Duration.ofMinutes(5);

    private final SecretKey key;
    private final Duration accessTtl;

    public JwtService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofMillis(expirationMs);
    }

    public String createAccessToken(User user) {
        return build(user.getId(), TYPE_ACCESS, accessTtl)
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .compact();
    }

    public String createTwoFactorToken(User user) {
        return build(user.getId(), TYPE_TWO_FACTOR, TWO_FACTOR_TTL).compact();
    }

    public Optional<Claims> parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
            return expectedType.equals(claims.get("type", String.class)) ? Optional.of(claims) : Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private JwtBuilder build(String userId, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key, Jwts.SIG.HS256);
    }
}

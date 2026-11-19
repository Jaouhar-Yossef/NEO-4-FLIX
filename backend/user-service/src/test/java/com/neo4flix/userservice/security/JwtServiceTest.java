package com.neo4flix.userservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.neo4flix.userservice.user.User;

class JwtServiceTest {

    private static final String SECRET = "test-secret-for-unit-tests-only-0123456789";

    private final JwtService jwt = new JwtService(SECRET, 3_600_000);

    private static User alice() {
        User u = new User();
        u.setId("user-123");
        u.setEmail("alice@example.com");
        u.setRole("USER");
        return u;
    }

    @Test
    void accessTokenCarriesTheContractClaims() {
        var claims = jwt.parse(jwt.createAccessToken(alice()), JwtService.TYPE_ACCESS).orElseThrow();
        assertThat(claims.getSubject()).isEqualTo("user-123");
        assertThat(claims.get("email", String.class)).isEqualTo("alice@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void twoFactorTokenIsNotAnAccessToken() {
        String temp = jwt.createTwoFactorToken(alice());
        assertThat(jwt.parse(temp, JwtService.TYPE_ACCESS)).isEmpty();
        assertThat(jwt.parse(temp, JwtService.TYPE_TWO_FACTOR)).isPresent();
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        var other = new JwtService("another-secret-another-secret-0123456789", 3_600_000);
        assertThat(jwt.parse(other.createAccessToken(alice()), JwtService.TYPE_ACCESS)).isEmpty();
    }

    @Test
    void rejectsExpiredAndGarbageTokens() {
        var expiring = new JwtService(SECRET, -1000);
        assertThat(jwt.parse(expiring.createAccessToken(alice()), JwtService.TYPE_ACCESS)).isEmpty();
        assertThat(jwt.parse("not-a-jwt", JwtService.TYPE_ACCESS)).isEmpty();
        assertThat(jwt.parse("", JwtService.TYPE_ACCESS)).isEmpty();
    }

    @Test
    void refusesShortSecret() {
        assertThatThrownBy(() -> new JwtService("too-short", 1000)).isInstanceOf(RuntimeException.class);
    }
}

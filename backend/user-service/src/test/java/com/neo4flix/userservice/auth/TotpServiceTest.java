package com.neo4flix.userservice.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;

class TotpServiceTest {

    private final TotpService totp = new TotpService();

    @Test
    void acceptsCurrentCodeAndRejectsWrongOne() throws Exception {
        String secret = totp.newSecret();
        long bucket = new SystemTimeProvider().getTime() / 30;
        String code = new DefaultCodeGenerator().generate(secret, bucket);

        assertThat(totp.isValid(secret, code)).isTrue();
        assertThat(totp.isValid(secret, "000000".equals(code) ? "111111" : "000000")).isFalse();
        assertThat(totp.isValid(null, code)).isFalse();
    }

    @Test
    void buildsOtpauthUri() {
        String uri = totp.otpauthUri("alice@example.com", "JBSWY3DPEHPK3PXP");
        assertThat(uri).startsWith("otpauth://totp/").contains("secret=JBSWY3DPEHPK3PXP").contains("issuer=Neo4flix");
    }
}

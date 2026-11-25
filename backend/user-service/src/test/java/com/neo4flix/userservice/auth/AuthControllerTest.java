package com.neo4flix.userservice.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.neo4flix.userservice.security.JwtService;
import com.neo4flix.userservice.security.SecurityConfig;
import com.neo4flix.userservice.user.User;

/** Web layer only: routing, security rules, validation, error format. AuthService is mocked. */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-for-unit-tests-only-0123456789",
        "app.jwt.expiration-ms=3600000"})
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtService jwtService;

    @MockitoBean
    AuthService authService;

    @Test
    void loginIsPublic() throws Exception {
        when(authService.login(any())).thenReturn(AuthDtos.LoginResponse.ofToken("abc"));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.c\",\"password\":\"x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("abc"))
                .andExpect(jsonPath("$.twoFactorRequired").doesNotExist());
    }

    @Test
    void invalidEmailGivesProblemDetail400() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nope\",\"displayName\":\"A\",\"password\":\"S3cure!pass\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("email must be a valid email address"));
    }

    @Test
    void brokenJsonGives400() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setupNeedsToken() throws Exception {
        mvc.perform(post("/auth/2fa/setup")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void bearerTokenWithoutGatewayHeadersGives401() throws Exception {
        User u = new User();
        u.setId("user-1");
        u.setEmail("a@b.c");
        mvc.perform(post("/auth/2fa/setup").header("Authorization", "Bearer " + jwtService.createAccessToken(u)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void setupWorksWithGatewayHeaders() throws Exception {
        when(authService.setupTwoFactor("user-1")).thenReturn(new AuthDtos.TwoFactorSetupResponse("otpauth://totp/x"));
        mvc.perform(post("/auth/2fa/setup")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Email", "a@b.c")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpauthUri").value("otpauth://totp/x"));
    }
}

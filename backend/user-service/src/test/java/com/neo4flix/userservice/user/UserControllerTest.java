package com.neo4flix.userservice.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.neo4flix.userservice.user.UserDtos.*;

/**
 * Web layer only: routing, security rules, validation, error format.
 * UserService is mocked.
 */
@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, JwtService.class })
@TestPropertySource(properties = {
		"app.jwt.secret=test-secret-for-unit-tests-only-0123456789",
		"app.jwt.expiration-ms=3600000" })
class UserControllerTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	JwtService jwtService;

	@MockitoBean
	UserService userService;

	private String bearer() {
		User u = new User();
		u.setId("user-1");
		u.setEmail("alice@example.com");
		return "Bearer " + jwtService.createAccessToken(u);
	}

	@Test
	void meNeedsToken() throws Exception {
		mvc.perform(get("/users/me")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void meReturnsProfileOfTokenOwner() throws Exception {
		when(userService.getProfile("user-1"))
				.thenReturn(new ProfileResponse("user-1", "alice@example.com", "Alice", false));
		mvc.perform(get("/users/me").header("Authorization", bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("user-1"))
				.andExpect(jsonPath("$.email").value("alice@example.com"))
				.andExpect(jsonPath("$.displayName").value("Alice"))
				.andExpect(jsonPath("$.twoFactorEnabled").value(false))
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.totpSecret").doesNotExist());
	}

	@Test
	void updateReturnsUpdatedProfile() throws Exception {
		when(userService.updateProfile(eq("user-1"), any()))
				.thenReturn(new ProfileResponse("user-1", "alice@example.com", "Alice B.", false));
		mvc.perform(put("/users/me").header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"Alice B.\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Alice B."));
	}

	@Test
	void blankDisplayNameGivesProblemDetail400() throws Exception {
		mvc.perform(put("/users/me").header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"  \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("displayName is required"));
		verify(userService, never()).updateProfile(any(), any());
	}

	@Test
	void tooLongDisplayNameGives400() throws Exception {
		mvc.perform(put("/users/me").header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"" + "a".repeat(51) + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("displayName must be at most 50 characters"));
	}
}

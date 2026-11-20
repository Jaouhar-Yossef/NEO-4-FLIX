package com.neo4flix.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

// prod profile on purpose: routes must not depend on the dev profile.
@ActiveProfiles("prod")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "app.jwt.secret=" + GatewayRoutesTest.SECRET)
class GatewayRoutesTest {

	static final String SECRET = "test-secret-for-unit-tests-only-0123456789";

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private RouteLocator routeLocator;

	private WebTestClient client;

	// A port nothing listens on, so user-service looks down.
	private static int freePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@DynamicPropertySource
	static void urls(DynamicPropertyRegistry registry) {
		registry.add("USER_SERVICE_URL", () -> "http://localhost:" + freePort());
		registry.add("MOVIE_SERVICE_URL", () -> "http://movie-service:8080");
	}

	@BeforeEach
	void setUp() {
		client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
	}

	@Test
	void routesUseServiceUrlsFromEnvironment() {
		Map<String, String> uris = routeLocator.getRoutes()
				.collectMap(Route::getId, route -> route.getUri().toString())
				.block();

		assertThat(uris).containsEntry("movie-service", "http://movie-service:8080")
				.containsEntry("rating-service", "http://localhost:8083")
				.containsEntry("recommendation-service", "http://localhost:8084")
				.containsKey("user-service");
	}

	@Test
	void serviceDownReturns503() {
		String token = Jwts.builder()
				.subject("user-1")
				.claim("email", "user1@example.com")
				.claim("role", "USER")
				.claim("type", "access")
				.expiration(Date.from(Instant.now().plusSeconds(600)))
				.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
				.compact();

		client.get().uri("/api/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.exchange()
				.expectStatus().isEqualTo(503)
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(503)
				.jsonPath("$.title").isEqualTo("Service Unavailable")
				.jsonPath("$.detail").isEqualTo("service unavailable, try again later");
	}
}

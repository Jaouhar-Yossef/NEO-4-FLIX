package com.neo4flix.gateway;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import com.sun.net.httpserver.HttpServer;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "app.jwt.secret=" + JwtAuthFilterTest.SECRET)
class JwtAuthFilterTest {

	static final String SECRET = "test-secret-for-unit-tests-only-0123456789";
	private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

	// Fake downstream service: echoes the path and X-User-* headers it receives.
	private static final HttpServer DOWNSTREAM = startDownstream();

	@Value("${local.server.port}")
	private int port;

	private WebTestClient client;

	private static HttpServer startDownstream() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			server.createContext("/", exchange -> {
				var h = exchange.getRequestHeaders();
				String body = exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath()
						+ " id=" + h.get("X-User-Id")
						+ " email=" + h.get("X-User-Email")
						+ " role=" + h.get("X-User-Role");
				byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, bytes.length);
				try (OutputStream out = exchange.getResponseBody()) {
					out.write(bytes);
				}
			});
			server.start();
			return server;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	// Same routes and StripPrefix=1 as application-dev.yml, but pointing at the fake downstream.
	@DynamicPropertySource
	static void routes(DynamicPropertyRegistry registry) {
		String uri = "http://localhost:" + DOWNSTREAM.getAddress().getPort();
		String prefix = "spring.cloud.gateway.server.webflux.routes";
		registry.add(prefix + "[0].id", () -> "user-service");
		registry.add(prefix + "[0].uri", () -> uri);
		registry.add(prefix + "[0].predicates[0]", () -> "Path=/api/users/**,/api/auth/**");
		registry.add(prefix + "[0].filters[0]", () -> "StripPrefix=1");
		registry.add(prefix + "[1].id", () -> "movie-service");
		registry.add(prefix + "[1].uri", () -> uri);
		registry.add(prefix + "[1].predicates[0]", () -> "Path=/api/movies/**");
		registry.add(prefix + "[1].filters[0]", () -> "StripPrefix=1");
	}

	@AfterAll
	static void stopDownstream() {
		DOWNSTREAM.stop(0);
	}

	@BeforeEach
	void setUp() {
		client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
	}

	private static String token(SecretKey key, String type, String role, Instant expiration) {
		return Jwts.builder()
				.subject("user-1")
				.claim("email", "user1@example.com")
				.claim("role", role)
				.claim("type", type)
				.expiration(Date.from(expiration))
				.signWith(key, Jwts.SIG.HS256)
				.compact();
	}

	private static String accessToken(String role) {
		return token(KEY, "access", role, Instant.now().plusSeconds(600));
	}

	private void expectUnauthorized(String authorization) {
		WebTestClient.RequestHeadersSpec<?> request = client.get().uri("/api/users/me");
		if (authorization != null) {
			request = request.header(HttpHeaders.AUTHORIZATION, authorization);
		}
		request.exchange()
				.expectStatus().isUnauthorized()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(401)
				.jsonPath("$.title").isEqualTo("Unauthorized")
				.jsonPath("$.detail").isEqualTo("missing or invalid token");
	}

	@Test
	void publicRouteWithoutTokenPassesAndIsStripped() {
		client.post().uri("/api/auth/login")
				.header("X-User-Role", "ADMIN")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("POST /auth/login id=null email=null role=null");
	}

	@Test
	void twoFactorSetupIsNotPublic() {
		client.post().uri("/api/auth/2fa/setup")
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void noTokenIsRefused() {
		expectUnauthorized(null);
	}

	@Test
	void badSignatureIsRefused() {
		SecretKey otherKey = Keys.hmacShaKeyFor("another-secret-that-is-long-enough-012345".getBytes(StandardCharsets.UTF_8));
		expectUnauthorized("Bearer " + token(otherKey, "access", "USER", Instant.now().plusSeconds(600)));
	}

	@Test
	void expiredTokenIsRefused() {
		expectUnauthorized("Bearer " + token(KEY, "access", "USER", Instant.now().minusSeconds(60)));
	}

	@Test
	void twoFactorTempTokenIsRefused() {
		expectUnauthorized("Bearer " + token(KEY, "2fa", "USER", Instant.now().plusSeconds(600)));
	}

	@Test
	void validTokenForwardsUserHeaders() {
		client.get().uri("/api/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("USER"))
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.isEqualTo("GET /users/me id=[user-1] email=[user1@example.com] role=[USER]");
	}

	@Test
	void clientUserHeadersAreReplaced() {
		client.get().uri("/api/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("USER"))
				.header("X-User-Role", "ADMIN")
				.header("x-user-id", "someone-else")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.isEqualTo("GET /users/me id=[user-1] email=[user1@example.com] role=[USER]");
	}

	// Proves the filter runs before StripPrefix=1: it must still see /api/movies to apply the ADMIN rule.
	@Test
	void userCannotPostMovie() {
		client.post().uri("/api/movies")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("USER"))
				.exchange()
				.expectStatus().isForbidden()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(403)
				.jsonPath("$.title").isEqualTo("Forbidden");
	}

	@Test
	void userCannotDeleteMovie() {
		client.delete().uri("/api/movies/42")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("USER"))
				.exchange()
				.expectStatus().isForbidden();
	}

	@Test
	void userCannotPatchMovie() {
		client.patch().uri("/api/movies/42")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("USER"))
				.exchange()
				.expectStatus().isForbidden()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void userCannotPostMovieWithMatrixParameter() {
		for (String path : new String[] { "/api/movies;x=1", "/api/movies;", "/api/movies/42;x=1" }) {
			client.post().uri(URI.create("http://localhost:" + port + path))
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("USER"))
					.exchange()
					.expectStatus().isForbidden();
		}
	}

	@Test
	void adminCanPostMovie() {
		client.post().uri("/api/movies")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("ADMIN"))
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.isEqualTo("POST /movies id=[user-1] email=[user1@example.com] role=[ADMIN]");
	}

	@Test
	void userCanGetGenres() {
		client.get().uri("/api/movies/genres")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("USER"))
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.isEqualTo("GET /movies/genres id=[user-1] email=[user1@example.com] role=[USER]");
	}
}

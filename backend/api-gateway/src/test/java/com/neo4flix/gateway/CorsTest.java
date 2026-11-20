package com.neo4flix.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "app.jwt.secret=test-secret-for-unit-tests-only-0123456789")
class CorsTest {

	private static final String FRONTEND = "http://localhost:4200";

	@Value("${local.server.port}")
	private int port;

	private WebTestClient client;

	@BeforeEach
	void setUp() {
		client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
	}

	// The browser's preflight has no Authorization header, so it must not get a 401.
	@Test
	void preflightFromFrontendIsAllowedWithoutToken() {
		client.options().uri("/api/movies")
				.header(HttpHeaders.ORIGIN, FRONTEND)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND)
				.expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
	}

	@Test
	void preflightFromOtherOriginIsRefused() {
		client.options().uri("/api/movies")
				.header(HttpHeaders.ORIGIN, "https://evil.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.exchange()
				.expectStatus().isForbidden()
				.expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
	}

	@Test
	void responseToFrontendCarriesAllowOrigin() {
		client.get().uri("/api/movies")
				.header(HttpHeaders.ORIGIN, FRONTEND)
				.exchange()
				.expectStatus().isUnauthorized()
				.expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND);
	}
}

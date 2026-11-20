package com.neo4flix.gateway;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"app.jwt.secret=test-secret-for-unit-tests-only-0123456789",
				"spring.cloud.gateway.server.webflux.httpclient.response-timeout=500ms"
		})
class GatewayTimeoutTest {

	// Fake downstream service that answers after 3 s, longer than the 500 ms timeout.
	private static final HttpServer SLOW_DOWNSTREAM = startSlowDownstream();

	@Value("${local.server.port}")
	private int port;

	private WebTestClient client;

	private static HttpServer startSlowDownstream() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			server.createContext("/", exchange -> {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				exchange.sendResponseHeaders(200, -1);
				exchange.close();
			});
			server.start();
			return server;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	// Login is public, so the request reaches the downstream without a token.
	@DynamicPropertySource
	static void routes(DynamicPropertyRegistry registry) {
		String prefix = "spring.cloud.gateway.server.webflux.routes";
		registry.add(prefix + "[0].id", () -> "user-service");
		registry.add(prefix + "[0].uri", () -> "http://localhost:" + SLOW_DOWNSTREAM.getAddress().getPort());
		registry.add(prefix + "[0].predicates[0]", () -> "Path=/api/auth/**");
		registry.add(prefix + "[0].filters[0]", () -> "StripPrefix=1");
	}

	@AfterAll
	static void stopDownstream() {
		SLOW_DOWNSTREAM.stop(0);
	}

	@BeforeEach
	void setUp() {
		client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
	}

	@Test
	void slowServiceGivesProblemDetail504() {
		client.post().uri("/api/auth/login")
				.exchange()
				.expectStatus().isEqualTo(504)
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(504)
				.jsonPath("$.title").isEqualTo("Gateway Timeout")
				.jsonPath("$.detail").isEqualTo("gateway timeout");
	}
}

package com.neo4flix.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "app.jwt.secret=test-secret-for-unit-tests-only-0123456789")
class GatewayErrorHandlerTest {

	@Value("${local.server.port}")
	private int port;

	private WebTestClient client;

	@BeforeEach
	void setUp() {
		client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
	}

	@Test
	void unknownPathGivesProblemDetail404() {
		client.get().uri("/api/unknown")
				.exchange()
				.expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(404)
				.jsonPath("$.title").isEqualTo("Not Found")
				.jsonPath("$.detail").isEqualTo("not found")
				.jsonPath("$.timestamp").doesNotExist();
	}

	@Test
	void wrongMethodGivesProblemDetail405() {
		client.post().uri("/actuator/health")
				.exchange()
				.expectStatus().isEqualTo(405)
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(405)
				.jsonPath("$.title").isEqualTo("Method Not Allowed");
	}

	@Test
	void healthIsUpWithoutToken() {
		client.get().uri("/actuator/health")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("UP");
	}

	@Test
	void unexpectedErrorGives500WithoutInternalDetails() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me"));

		new GatewayErrorHandler()
				.handle(exchange, new IllegalStateException("Neo.ClientError.Statement.SyntaxError in MATCH (u:User)"))
				.block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(exchange.getResponse().getBodyAsString().block())
				.isEqualTo("{\"status\":500,\"title\":\"Internal Server Error\",\"detail\":\"unexpected error\"}");
	}
}

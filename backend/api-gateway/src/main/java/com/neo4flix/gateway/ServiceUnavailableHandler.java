package com.neo4flix.gateway;

import java.net.ConnectException;
import java.net.UnknownHostException;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

import reactor.core.publisher.Mono;

// Order -2 = before Spring Boot's default error handler (-1), which would answer 500.
@Component
@Order(-2)
public class ServiceUnavailableHandler implements WebExceptionHandler {

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		if (exchange.getResponse().isCommitted() || !isServiceDown(ex)) {
			return Mono.error(ex);
		}
		return JwtAuthFilter.problem(exchange.getResponse(), HttpStatus.SERVICE_UNAVAILABLE,
				"service unavailable, try again later");
	}

	// Connection refused = service stopped; unknown host = its Docker container is not running.
	private boolean isServiceDown(Throwable ex) {
		while (ex != null) {
			if (ex instanceof ConnectException || ex instanceof UnknownHostException) {
				return true;
			}
			ex = ex.getCause();
		}
		return false;
	}
}

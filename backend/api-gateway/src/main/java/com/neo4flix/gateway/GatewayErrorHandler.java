package com.neo4flix.gateway;

import java.net.ConnectException;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

import reactor.core.publisher.Mono;

// Every gateway error as a ProblemDetail, like the services.
// Order -2 = before Spring Boot's default error handler (-1), which answers with its own JSON.
@Component
@Order(-2)
public class GatewayErrorHandler implements WebExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		if (exchange.getResponse().isCommitted()) {
			return Mono.error(ex);
		}
		if (isServiceDown(ex)) {
			return JwtAuthFilter.problem(exchange.getResponse(), HttpStatus.SERVICE_UNAVAILABLE,
					"service unavailable, try again later");
		}
		// Known HTTP errors (404 no route, 405, 504 timeout...): keep the status, but not the
		// exception message, which can contain internal details.
		if (ex instanceof ErrorResponse error) {
			HttpStatus status = HttpStatus.resolve(error.getStatusCode().value());
			if (status != null) {
				return JwtAuthFilter.problem(exchange.getResponse(), status, status.getReasonPhrase().toLowerCase());
			}
		}
		log.error("Unexpected gateway error on {}", exchange.getRequest().getPath(), ex);
		return JwtAuthFilter.problem(exchange.getResponse(), HttpStatus.INTERNAL_SERVER_ERROR, "unexpected error");
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

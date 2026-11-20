package com.neo4flix.gateway;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

	private static final List<String> PUBLIC_POST_PATHS = List.of(
			"/api/auth/register", "/api/auth/login", "/api/auth/2fa/verify");

	private final SecretKey key;

	public JwtAuthFilter(@Value("${app.jwt.secret}") String secret) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	// Runs before every route filter (StripPrefix=1 included), so paths here still
	// start with /api.
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// Services trust X-User-* blindly, so a client must never be able to send them,
		// even on public routes.
		ServerHttpRequest.Builder builder = exchange.getRequest().mutate().headers(headers -> {
			for (String name : new ArrayList<>(headers.headerNames())) {
				if (name.toLowerCase().startsWith("x-user-")) {
					headers.remove(name);
				}
			}
		});

		ServerHttpRequest request = exchange.getRequest();
		String path = request.getPath().value();
		HttpMethod method = request.getMethod();

		if (method == HttpMethod.POST && PUBLIC_POST_PATHS.contains(path)) {
			return chain.filter(exchange.mutate().request(builder.build()).build());
		}

		Claims claims = parseAccessToken(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
		if (claims == null) {
			return problem(exchange.getResponse(), HttpStatus.UNAUTHORIZED, "missing or invalid token");
		}

		String role = claims.get("role", String.class);
		boolean writesMovies = (path.equals("/api/movies") || path.startsWith("/api/movies/"))
				&& (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.DELETE);
		if (writesMovies && !"ADMIN".equals(role)) {
			return problem(exchange.getResponse(), HttpStatus.FORBIDDEN, "admin role required");
		}

		builder.header("X-User-Id", claims.getSubject())
				.header("X-User-Email", claims.get("email", String.class))
				.header("X-User-Role", role);
		return chain.filter(exchange.mutate().request(builder.build()).build());
	}

	private Claims parseAccessToken(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return null;
		}
		try {
			Claims claims = Jwts.parser().verifyWith(key).build()
					.parseSignedClaims(authorization.substring(7))
					.getPayload();
			// The 2FA tempToken is signed with the same key; only "access" tokens may call
			// the API.
			return "access".equals(claims.get("type", String.class)) ? claims : null;
		} catch (JwtException | IllegalArgumentException e) {
			return null;
		}
	}

	static Mono<Void> problem(ServerHttpResponse response, HttpStatus status, String detail) {
		response.setStatusCode(status);
		response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/problem+json");
		String body = "{\"status\":" + status.value() + ",\"title\":\"" + status.getReasonPhrase()
				+ "\",\"detail\":\"" + detail + "\"}";
		DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
		return response.writeWith(Mono.just(buffer));
	}
}

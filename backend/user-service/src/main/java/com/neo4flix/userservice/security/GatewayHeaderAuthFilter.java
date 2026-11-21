package com.neo4flix.userservice.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

// Trusts X-User-* without any check. Safe only because:
// 1. the gateway checks the JWT, strips any client X-User-* header and sets its own
//    (api-gateway JwtAuthFilter);
// 2. only the gateway can reach this service: its port is never published outside Docker.
// If either stops being true, anyone can send X-User-Id and act as any user.
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String userId = request.getHeader("X-User-Id");
		String role = request.getHeader("X-User-Role");
		if (userId != null && !userId.isBlank() && role != null && !role.isBlank()) {
			var authority = new SimpleGrantedAuthority("ROLE_" + role);
			var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of(authority));
			SecurityContextHolder.getContext().setAuthentication(auth);
		}
		chain.doFilter(request, response);
	}
}

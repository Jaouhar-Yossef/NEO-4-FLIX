# ADR 0001: JWT checked at the gateway, user identity forwarded as headers

- Status: accepted
- Date: 2026-11-20

## Context

Neo4flix has five backend services: api-gateway, user-service, movie-service,
rating-service and recommendation-service. All of them except the gateway need to know who
the caller is. user-service creates the JWT at login (HS256, signed with `JWT_SECRET`).

Option A: every service checks the JWT itself. Every service then needs `JWT_SECRET` and the
same validation code, and a bug in one of them is a hole.

Option B: only the gateway checks the JWT and tells the services who the caller is.

## Decision

Option B.

1. The gateway checks every request except register, login and 2FA verify: signature,
   expiry, and `type == access` (the 2FA `tempToken` is refused). Otherwise `401`.
2. The gateway removes every `X-User-*` header sent by the client, then sets its own from
   the token: `X-User-Id`, `X-User-Email`, `X-User-Role`.
3. Any method except `GET` / `HEAD` on `/api/movies/**` needs role `ADMIN`, checked at the
   gateway (`403`).
4. Services trust the `X-User-*` headers and do not check the JWT.
5. Only the gateway can reach the services. In Docker, only nginx (frontend) publishes a
   port; nginx proxies `/api` to the gateway. The gateway and the services have no published
   port. Neo4j is published on `127.0.0.1` only, for the Neo4j Browser.

Errors from the gateway (`401`, `403`, `503`, `504`) are ProblemDetail
(`application/problem+json`), like the services.

## Consequences

- `JWT_SECRET` is needed only by user-service (to sign) and api-gateway (to verify).
- The services stay simple: they read one header to know the user.
- Points 2 and 5 together are what makes this safe. If a service port is ever published, or
  the gateway stops stripping `X-User-*`, anyone can send `X-User-Id` and act as any user.
  So: never add a `ports:` entry for a backend service in `docker-compose.yml`.
- When the services run locally with mvn (dev profile), they listen on `localhost` and can
  be called directly. Accepted for development only.
- The gateway is a single point of failure: if it is down, the whole API is down. Accepted
  for this project's size.
- Tests: `JwtAuthFilterTest` (gateway rules, header stripping), `CorsTest` (preflight is
  answered without a token), `GatewayErrorHandlerTest` and `GatewayTimeoutTest`
  (ProblemDetail errors).

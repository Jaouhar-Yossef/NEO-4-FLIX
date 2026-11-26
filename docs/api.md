# Neo4flix — Backend API Contract

This document describes the backend endpoints currently implemented in the
Neo4flix services.

## 1. Overview

The frontend communicates with the backend through the API Gateway.

**Base URL (local Docker setup):**

```text
http://localhost:4200/api
```

The API Gateway routes requests as follows:

| Gateway prefix | Backend service | Forwarded prefix |
|---|---|---|
| `/api/auth/**` | user-service | `/auth/**` |
| `/api/users/**` | user-service | `/users/**` |
| `/api/movies/**` | movie-service | `/movies/**` |
| `/api/ratings/**` | rating-service | `/ratings/**` |
| `/api/recommendations/**` | recommendation-service | `/recommendations/**` |

The gateway removes the `/api` prefix before forwarding requests.

## 2. Authentication and authorization

Protected endpoints require:

```http
Authorization: Bearer <access-token>
```

The JWT is signed using `JWT_SECRET`.

The gateway validates the token signature, expiration and token type.
Only tokens with `type=access` are accepted for protected API requests.

### Public endpoints

These POST endpoints do not require a JWT:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/2fa/verify`

All other API endpoints require a valid access token.

### Roles

Movie write operations require the `ADMIN` role:

- `POST /api/movies`
- `PUT /api/movies/{id}`
- `DELETE /api/movies/{id}`

Movie `GET` and `HEAD` requests require authentication but do not require
the `ADMIN` role.

### Trusted user headers

After validating the JWT, the gateway forwards:

- `X-User-Id`
- `X-User-Email`
- `X-User-Role`

Client-supplied headers beginning with `X-User-` are removed before
forwarding. Backend services use the trusted headers supplied by the gateway.

### CORS

The gateway allows the configured frontend origin. The default is:

```text
http://localhost:4200
```

Allowed methods: `GET`, `POST`, `PUT`, `DELETE`.

## 3. Authentication API

Base path: `/api/auth`

### POST `/api/auth/register`

Registers a new user. No JWT required.

Request:

```json
{
  "email": "alice@example.com",
  "displayName": "Alice",
  "password": "S3cure!pass"
}
```

Validation:
- `email` is required and must be a valid email address (maximum 254 characters).
- `displayName` is required (maximum 50 characters).
- `password` is required. The authentication service also applies its password policy.

Response: `201 Created`

```json
{
  "id": "user-id",
  "email": "alice@example.com",
  "displayName": "Alice"
}
```

### POST `/api/auth/login`

Authenticates a user. No JWT required.

Request:

```json
{
  "email": "alice@example.com",
  "password": "S3cure!pass"
}
```

Response when two-factor authentication is disabled:

```json
{
  "token": "access-jwt"
}
```

Response when two-factor authentication is enabled:

```json
{
  "twoFactorRequired": true,
  "tempToken": "temporary-jwt"
}
```

The temporary token is only for the second authentication step. It is not
accepted as an access token by the API Gateway.

### POST `/api/auth/2fa/verify`

Completes login when two-factor authentication is required. No access JWT required.

Request:

```json
{
  "tempToken": "temporary-jwt",
  "code": "123456"
}
```

The code must contain exactly six digits.

Response:

```json
{
  "token": "access-jwt"
}
```

### POST `/api/auth/2fa/setup`

Requires an access JWT.

Starts two-factor authentication setup for the authenticated user.

Response:

```json
{
  "otpauthUri": "otpauth://totp/..."
}
```

### POST `/api/auth/2fa/enable`

Requires an access JWT.

Request:

```json
{
  "code": "123456"
}
```

The code must contain exactly six digits.

Response: `204 No Content`.

## 4. User API

Base path: `/api/users`

All endpoints require an access JWT. The user is identified using the JWT
subject (`sub`), not a user ID supplied by the client.

### GET `/api/users/me`

Returns the authenticated user's profile.

Example response:

```json
{
  "id": "user-id",
  "email": "alice@example.com",
  "displayName": "Alice",
  "twoFactorEnabled": false
}
```

### PUT `/api/users/me`

Updates the authenticated user's display name.

Request:

```json
{
  "displayName": "Alice B."
}
```

`displayName` is required and must not exceed 50 characters.

Returns the updated profile.

### GET `/api/users/me/ratings`

Returns the authenticated user's rating history.

Example response:

```json
[
  {
    "movie": {
      "id": "movie-id",
      "title": "The Matrix",
      "releaseYear": 1999,
      "genres": ["Science Fiction"],
      "averageRating": 4.5,
      "ratingCount": 2
    },
    "score": 4,
    "updatedAt": "2026-11-26T10:00:00Z"
  }
]
```

### DELETE `/api/users/me`

Deletes the authenticated user's account.

Response: `204 No Content`.

## 5. Movie API

Base path: `/api/movies`

All endpoints require an access JWT. Write operations additionally require
the `ADMIN` role.

### GET `/api/movies`

Searches and retrieves movies. All query parameters are optional.

| Parameter | Description |
|---|---|
| `title` | Movie title filter |
| `genre` | Genre filter |
| `year` | Release year |
| `page` | Page index, default `0` |
| `size` | Page size, default `20`, maximum `100` |
| `sortBy` | Sort field, default `title` |
| `direction` | Sort direction, default `ASC` |

Example:

```http
GET /api/movies?genre=Drama&page=0&size=20
```

`GET /api/movies/search` supports the same search parameters.

Returns a paginated movie response.

### GET `/api/movies/{id}`

Returns movie details for the requested movie ID.

The optional `X-User-Id` header is supplied by the gateway when a user is
authenticated.

Returns movie details, including user-specific information where available.

### POST `/api/movies`

Creates a movie. Requires the `ADMIN` role.

Request:

```json
{
  "title": "The Matrix",
  "releaseYear": 1999,
  "genres": ["Science Fiction", "Action"]
}
```

Validation:
- `title` is required and cannot exceed 200 characters.
- `releaseYear` is required and must be between 1888 and 2100.
- `genres` must not be empty and must not contain blank values.

Returns the created movie.

### PUT `/api/movies/{id}`

Updates a movie. Requires the `ADMIN` role.

Uses the same request body and validation rules as movie creation.

### DELETE `/api/movies/{id}`

Deletes a movie. Requires the `ADMIN` role.

## 6. Rating API

Base path: `/api/ratings`

Rating operations use the authenticated user's ID supplied by the gateway.

A rating must be an integer between 1 and 5.

### POST `/api/ratings`

Creates a rating for a movie.

Request:

```json
{
  "movieId": "movie-id",
  "rating": 4
}
```

Response: `201 Created`.

Returns a rating object containing the movie ID, user ID, rating and
creation/update timestamps.

### GET `/api/ratings/me`

Returns the authenticated user's ratings.

### GET `/api/ratings/movie/{movieId}`

Returns ratings associated with the specified movie.

### PUT `/api/ratings/{movieId}`

Updates the authenticated user's rating for the specified movie.

Request:

```json
{
  "rating": 5
}
```

The rating must be between 1 and 5.

### DELETE `/api/ratings/{movieId}`

Deletes the authenticated user's rating for the specified movie.

Response: `204 No Content`.

## 7. Recommendation API

Base path: `/api/recommendations`

All endpoints require an access JWT.

### GET `/api/recommendations`

Returns movie recommendations for the authenticated user.

Optional query parameters:

| Parameter | Description |
|---|---|
| `genre` | Filter recommendations by genre |
| `year` | Filter by release year |
| `page` | Page index, default `0` |
| `size` | Page size, default `20`, maximum `100` |

Example:

```http
GET /api/recommendations?genre=Drama&page=0&size=10
```

Response structure:

```json
{
  "content": [
    {
      "id": "movie-id",
      "title": "Example Movie",
      "releaseYear": 2020,
      "genres": ["Drama"],
      "averageRating": 4.5,
      "ratingCount": 12,
      "relevanceScore": 0.87,
      "similarUsersCount": 5
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "personalized": true
}
```

Recommendations are personalized when the user has rating history.
Otherwise, the service can return popular recommendations.

## 8. Error responses

The API uses HTTP status codes to indicate request outcomes.

| Status | Meaning |
|---|---|
| `400 Bad Request` | Invalid request data or query parameters |
| `401 Unauthorized` | Missing, invalid, expired or unsupported token |
| `403 Forbidden` | Authenticated user does not have permission |
| `404 Not Found` | Requested resource does not exist |
| `409 Conflict` | Resource conflict, such as an existing email |
| `500 Internal Server Error` | Unexpected server error |
| `503 Service Unavailable` | A downstream service is unavailable |

Gateway authentication errors use `application/problem+json`.

Example:

```json
{
  "status": 401,
  "title": "Unauthorized",
  "detail": "missing or invalid token"
}
```

## 9. Data conventions

- Request and response bodies use JSON.
- JSON properties use `camelCase`.
- Date/time values use ISO-8601 representations.
- Protected requests use the `Authorization: Bearer <token>` header.
- The API Gateway is the intended entry point for frontend API requests.

# Neo4flix — API Contract (frontend ↔ backend)

## 1. Basics

**Base URL.** The Angular app only talks to the API gateway: `https://<host>/api/...`.
The gateway strips `/api` and forwards by prefix:

| Prefix | Service |
|---|---|
| `/api/auth/**` | user-service |
| `/api/users/**` | user-service |
| `/api/movies/**` | movie-service |
| `/api/ratings/**` | rating-service |
| `/api/recommendations/**` | recommendation-service |

**Auth.**
- Login returns one JWT (HS256, signed with `JWT_SECRET` from `.env`, valid
  `JWT_EXPIRATION_MS` = 1 h). Claims: `sub` (user id), `email`, `role` (`USER` or `ADMIN`),
  `type` = `access`.
- The 2FA `tempToken` is also a JWT, with `type` = `2fa`. **The gateway rejects a token
  whose `type` is not `access`**, otherwise the temp token would open the API before the code.
- The frontend sends it as `Authorization: Bearer <token>` and keeps it in `sessionStorage`.
- **The gateway checks the JWT** (signature, expiry, `type`) with `JWT_SECRET`, then forwards
  `X-User-Id` (`sub`), `X-User-Email` and `X-User-Role` to the service. It removes any
  `X-User-*` header sent by the client. Services trust these headers and are only reachable
  through the gateway (never published outside Docker).
- **Every endpoint needs the JWT**, except register, login and 2FA verify.
- Every method except `GET` / `HEAD` on `/api/movies/**` needs role `ADMIN` (checked at the gateway, `403`).
- If a service is down, the gateway returns `503` (ProblemDetail).
- No refresh token: when the token expires the API returns `401` and the app goes back to login.
  Logout = the frontend deletes the token.

**Format.** JSON, `camelCase`, dates in ISO-8601 (`2026-11-17T10:00:00Z`).

**Errors.** Spring's built-in `ProblemDetail` (`application/problem+json`):

```json
{ "status": 400, "title": "Bad Request", "detail": "password must contain a digit", "instance": "/auth/register" }
```

| Status | When |
|---|---|
| `400` | Invalid input (missing field, wrong type, bad value) |
| `401` | No token, bad or expired token, wrong password, wrong 2FA code |
| `403` | Logged in but not allowed (e.g. not `ADMIN`, sharing with a non-friend) |
| `404` | Not found |
| `409` | Already exists (email taken, already friends) |
| `500` | Unexpected error: generic message, never a stack trace or Cypher error |

All Cypher uses parameters (no string concatenation), so search input can't change the query.

## 2. Auth — `/api/auth` (user-service)

**Password policy** (checked by the backend, shown live by the register form): at least 8
characters, with an uppercase letter, a lowercase letter, a digit and a symbol (= any character
that is not a letter or digit). At most 72 bytes (BCrypt limit). Stored as a BCrypt hash.

### POST `/api/auth/register` — no JWT
```json
{ "email": "alice@example.com", "displayName": "Alice", "password": "S3cure!pass" }
```
→ `201` `{ "id": "…", "email": "alice@example.com", "displayName": "Alice" }`
Errors: `400` (invalid email, weak password), `409` (email taken).

### POST `/api/auth/login` — no JWT
```json
{ "email": "alice@example.com", "password": "S3cure!pass" }
```
→ `200`, 2FA off: `{ "token": "eyJ…" }`
→ `200`, 2FA on: `{ "twoFactorRequired": true, "tempToken": "…" }` (valid 5 min)
Errors: `400`, `401` (same message for unknown email and wrong password).

### POST `/api/auth/2fa/verify` — no JWT
Second login step when 2FA is on.
```json
{ "tempToken": "…", "code": "123456" }
```
→ `200` `{ "token": "eyJ…" }`. Errors: `400`, `401` (wrong code or expired `tempToken`).

### POST `/api/auth/2fa/setup` — JWT
Turns 2FA on, step 1. The backend creates a TOTP secret (authenticator app, e.g. Google
Authenticator).
→ `200` `{ "otpauthUri": "otpauth://totp/alice%40example.com?secret=…&issuer=Neo4flix" }`
The frontend shows the setup key for manual entry (no QR code, see NEO-10).
Errors: `409` (2FA already on).

### POST `/api/auth/2fa/enable` — JWT
Turns 2FA on, step 2: proves the app was set up.
`{ "code": "123456" }` → `204`. Errors: `400` (bad format, or `setup` not called), `401` (wrong
code), `409` (2FA already on).

## 3. Users — `/api/users` (user-service)

`/users/me` is always the token's user (`sub`). If that account no longer exists (deleted while
the token is still valid), `GET` and `PUT /users/me` answer `404`.

### GET `/api/users/me` — JWT
→ `200` `{ "id": "…", "email": "alice@example.com", "displayName": "Alice", "twoFactorEnabled": false }`

### PUT `/api/users/me` — JWT
`{ "displayName": "Alice B." }` → `200` updated user. Errors: `400`.

### DELETE `/api/users/me` — JWT
→ `204`. Deletes the user and all their relationships. Calling it again is fine (`204`, not
`404`). The JWT stays valid until it expires, so the frontend drops it right away.

### GET `/api/users/me/ratings` — JWT
The user's rating history (the brief puts it in the User service).
→ `200` `[ { "movie": MovieSummary, "score": 4, "updatedAt": "…" } ]`

### Watchlist — JWT

| Method | Path | Result |
|---|---|---|
| GET | `/api/users/me/watchlist` | `200` `[ MovieSummary ]` |
| PUT | `/api/users/me/watchlist/{movieId}` | `204` (adding twice is fine). `404` if no such movie |
| DELETE | `/api/users/me/watchlist/{movieId}` | `204`. `404` if not in the watchlist |

### Friends — JWT
Needed so users can share with friends.

| Method | Path | Body | Result |
|---|---|---|---|
| GET | `/api/users/me/friends` | — | `200` `[ { "id": "…", "displayName": "Bob" } ]` |
| POST | `/api/users/me/friends` | `{ "email": "bob@example.com" }` | `201` friend. `404` no such user, `409` already friends, `400` yourself |

## 4. Movies — `/api/movies` (movie-service)

`MovieSummary`:
```json
{ "id": "movie-001", "title": "The Matrix", "releaseYear": 1999,
  "genres": ["Science Fiction"], "averageRating": 4.5, "ratingCount": 2 }
```
`averageRating` is computed from the ratings (`null` if none).

### GET `/api/movies` — JWT
Home page list and search. All filters optional.

| Param | Meaning |
|---|---|
| `title` | contains, case-insensitive |
| `genre` | genre name |
| `year` | release year |
| `page`, `size` | default `0`, `20`; max size `100` |

→ `200` `{ "content": [ MovieSummary ], "page": 0, "size": 20, "totalElements": 57 }`
Errors: `400` (e.g. `year=abc`).

### GET `/api/movies/{id}` — JWT
Details page: `MovieSummary` + `"myRating": 5` (`null` if not rated) + `"inWatchlist": false`.
Errors: `404`.

### GET `/api/movies/genres` — JWT
For the filter dropdowns. → `200` `["Action", "Drama", "Science Fiction"]`

### Write movies — JWT, role `ADMIN`
The brief asks the Movie service to read **and write** movie data.

| Method | Path | Body | Result |
|---|---|---|---|
| POST | `/api/movies` | `{ "title", "releaseYear", "genres": [] }` | `201` |
| PUT | `/api/movies/{id}` | same | `200` |
| DELETE | `/api/movies/{id}` | — | `204` |

Errors: `400`, `403` (not admin), `404`.

## 5. Ratings — `/api/ratings` (rating-service)

One rating per user and movie, score **1 to 5**.

| Method | Path | Body | Result |
|---|---|---|---|
| PUT | `/api/ratings/{movieId}` | `{ "score": 4 }` | `200` `{ "movieId", "score", "updatedAt" }` (creates or updates) |
| DELETE | `/api/ratings/{movieId}` | — | `204` |

Errors: `400` (score not 1–5), `404` (movie, or no rating to delete).
Reading my ratings: `GET /api/users/me/ratings` (§3) and `myRating` on the movie page (§4).

## 6. Recommendations — `/api/recommendations` (recommendation-service)

### GET `/api/recommendations` — JWT
Optional filters: `genre`, `yearFrom`, `yearTo`.
→ `200` `[ { "movie": MovieSummary, "score": 0.87 } ]` (max 20, never movies I already rated).
A user with no ratings gets the top-rated movies.
Errors: `400`.

How it's computed (GDS Node Similarity on `User-RATED->Movie`) is the Recommendation
service's job and doesn't change this contract.

### POST `/api/recommendations/share` — JWT
`{ "movieId": "movie-002", "friendId": "user-002" }` → `201`.
Errors: `400`, `403` (not a friend), `404` (movie or user).

### GET `/api/recommendations/shared-with-me` — JWT
→ `200` `[ { "from": { "id", "displayName" }, "movie": MovieSummary, "sharedAt": "…" } ]`

## 7. What the graph model needs

Compared with `docs/database/neo4j-graph-model.md`:

| Where | Add | For |
|---|---|---|
| `User` | `displayName`, `passwordHash`, `role`, `twoFactorEnabled`, `totpSecret` | auth, 2FA |
| `Movie` | `releaseYear` (already in the seed, missing from the model doc) | search, details |
| `RATED` | `updatedAt` | rating updates |
| `FRIEND` | treat as undirected: one relationship per pair, queried with `(a)-[:FRIEND]-(b)` | friends |
| new: `SHARED` | `(:User)-[:SHARED {movieId, createdAt}]->(:User)` | sharing |
| index | RANGE index on `Movie.releaseYear` | search by year |

## 8. Which ticket builds what

| Section | Backend | Frontend |
|---|---|---|
| §2 Auth + 2FA | NEO-03 | NEO-10 |
| `/api/auth` gateway route | NEO-03 | — |
| JWT check at the gateway | NEO-07 | — |
| §3 Profile, my ratings | NEO-21 | NEO-21 |
| §3 Watchlist, friends | NEO-20 | NEO-15 |
| §4 Movies | NEO-04 | NEO-11, 12, 13 |
| §5 Ratings | NEO-05 | NEO-13 |
| §6 Recommendations | NEO-06 | NEO-14 |
| §6 Sharing | NEO-20 | NEO-15 |
| §7 Model | NEO-02 follow-up | — |

# Neo4flix — Backend Automated Testing

This document describes the backend automated test suites and how to run them.

## 1. Testing stack

The backend tests use:

- JUnit 5 for test execution.
- Mockito for unit tests and dependency mocking.
- Spring Boot Test for application and integration testing.
- Spring MVC test support for controller tests.
- Neo4j integration tests for repository queries and graph behavior.

## 2. Test coverage by service

| Service | Test classes | Tests passed |
|---|---|---:|
| Movie Service | MovieServiceTest, MovieControllerTest, MovieRepositoryIntegrationTest | 32 |
| Rating Service | RatingServiceTest, RatingControllerTest, RatingRepositoryIntegrationTest | 33 |
| Recommendation Service | RecommendationServiceTest, RecommendationControllerTest, RecommendationRepositoryIntegrationTest | 26 |
| User Service | AuthControllerTest, PasswordPolicyTest, TotpServiceTest, JwtServiceTest, UserControllerTest, UserServiceTest | 35 |
| API Gateway | ApiGatewayApplicationTests, CorsTest, GatewayErrorHandlerTest, GatewayRoutesTest, GatewayTimeoutTest, JwtAuthFilterTest | 25 |
| **Total** | | **151** |

The test suites cover:

### User Service
- Registration and authentication behavior.
- Password policy validation.
- Two-factor authentication and TOTP behavior.
- JWT service behavior.
- User profile operations.
- User service business logic.

### Movie Service
- Movie service business logic.
- Movie controller request handling.
- Movie repository queries against Neo4j.
- Movie search and persistence behavior.

### Rating Service
- Rating creation and update behavior.
- Rating validation.
- Rating controller endpoints.
- Rating repository queries against Neo4j.

### Recommendation Service
- Recommendation business logic.
- Recommendation controller behavior.
- Recommendation repository queries against Neo4j.
- Recommendation pagination and fallback behavior.

### API Gateway
- Gateway route configuration.
- JWT authentication filter behavior.
- CORS configuration.
- Gateway error handling.
- Connection and response timeout behavior.

## 3. Prerequisites

- Java 21.
- Maven.
- A running Neo4j instance for repository integration tests.
- Project environment variables configured in `.env`.

The repository integration tests use a real Neo4j database. Make sure Neo4j
is running and that the connection settings are available to the test process.

For a local Docker Compose setup:

```bash
docker compose up -d neo4j
```

Load environment variables before running tests when required:

```bash
set -a
source .env
set +a
```

Do not use `docker compose down -v` as part of test setup: it deletes
persistent database data.

## 4. Run all tests

Run each backend service test suite from the repository root:

```bash
mvn -f backend/user-service/pom.xml test
mvn -f backend/movie-service/pom.xml test
mvn -f backend/rating-service/pom.xml test
mvn -f backend/recommendation-service/pom.xml test
mvn -f backend/api-gateway/pom.xml test
```

The expected result for the currently verified test suites is:

```text
151 tests passed
0 failures
0 errors
```

## 5. Run an individual test class

Use Maven Surefire's `-Dtest` option.

Examples:

```bash
mvn -f backend/movie-service/pom.xml \
  -Dtest=MovieServiceTest test

mvn -f backend/rating-service/pom.xml \
  -Dtest=RatingControllerTest test

mvn -f backend/recommendation-service/pom.xml \
  -Dtest=RecommendationServiceTest test

mvn -f backend/user-service/pom.xml \
  -Dtest=AuthControllerTest test

mvn -f backend/api-gateway/pom.xml \
  -Dtest=JwtAuthFilterTest test
```

## 6. Run Neo4j repository integration tests

These tests verify repository behavior against Neo4j.

```bash
mvn -f backend/movie-service/pom.xml \
  -Dtest=MovieRepositoryIntegrationTest test

mvn -f backend/rating-service/pom.xml \
  -Dtest=RatingRepositoryIntegrationTest test

mvn -f backend/recommendation-service/pom.xml \
  -Dtest=RecommendationRepositoryIntegrationTest test
```

Ensure the Neo4j connection URI, username and password match the running
database before executing these tests.

## 7. Test reports

Maven Surefire generates XML and text reports under each service's:

```text
target/surefire-reports/
```

For example:

```text
backend/movie-service/target/surefire-reports/
backend/rating-service/target/surefire-reports/
backend/recommendation-service/target/surefire-reports/
backend/user-service/target/surefire-reports/
backend/api-gateway/target/surefire-reports/
```

These reports contain test counts, failures, errors and execution details.

## 8. API documentation

The current backend API contract is documented in:

- [API Contract](api.md)

The API contract documents gateway routes, authentication, authorization,
request parameters, request bodies and response structures.

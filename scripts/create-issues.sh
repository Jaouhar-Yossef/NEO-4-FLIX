#!/usr/bin/env bash
set -euo pipefail

REPO="Jaouhar-Yossef/NEO-4-FLIX"

gh repo view "$REPO" >/dev/null

# Create labels (safe if they already exist)
gh label create backend --repo "$REPO" --color 0052CC --description "Backend development" --force
gh label create frontend --repo "$REPO" --color 1D76DB --description "Frontend development" --force
gh label create database --repo "$REPO" --color 5319E7 --description "Database and Neo4j" --force
gh label create devops --repo "$REPO" --color 0E8A16 --description "Docker and deployment" --force
gh label create security --repo "$REPO" --color D93F0B --description "Security tasks" --force
gh label create testing --repo "$REPO" --color FBCA04 --description "Testing and quality" --force
gh label create documentation --repo "$REPO" --color C5DEF5 --description "Documentation" --force

create_issue() {
    local title="$1"
    local body="$2"
    shift 2

    local existing
    existing=$(gh issue list --repo "$REPO" --state all --limit 1000 --json title --jq '.[].title')

    if printf '%s\n' "$existing" | grep -Fqx "$title"; then
        echo "SKIPPED (already exists): $title"
        return
    fi

    local args=()
    for label in "$@"; do
        args+=(--label "$label")
    done

    gh issue create \
        --repo "$REPO" \
        --title "$title" \
        --body "$body" \
        "${args[@]}"

    echo "CREATED: $title"
}

# ==================================================
# DEVELOPER 1 - BACKEND & DATABASE
# ==================================================

create_issue "NEO-01: Initialize Spring Boot Microservices" \
"## Description
Create the five independent Spring Boot applications using Java 21 and Maven.

## Tasks
- Create user-service.
- Create movie-service.
- Create rating-service.
- Create recommendation-service.
- Create api-gateway.
- Configure application properties and environment variables.
- Add Actuator health endpoints.
- Configure development and production profiles.

## Acceptance Criteria
- [ ] Five independent Spring Boot applications exist.
- [ ] Each application has its own pom.xml.
- [ ] Each service builds independently.
- [ ] Health endpoints respond successfully." \
backend

create_issue "NEO-02: Configure Neo4j Database and Graph Model" \
"## Description
Configure Neo4j as the primary database and implement the initial graph model.

## Tasks
- Configure Neo4j connection.
- Create User, Movie and Genre nodes.
- Create RATED relationship with rating and createdAt properties.
- Create HAS_GENRE, WATCHLIST and FRIEND relationships.
- Add constraints and indexes.
- Prepare development seed data.
- Document the graph model.

## Acceptance Criteria
- [ ] Services can connect to Neo4j.
- [ ] Required nodes and relationships are documented.
- [ ] Constraints and indexes are configured.
- [ ] Seed data can be loaded." \
backend database

create_issue "NEO-03: Implement User Service and Authentication" \
"## Description
Implement user registration, login and profile management.

## Tasks
- POST /auth/register
- POST /auth/login
- GET /users/me
- PUT /users/me
- DELETE /users/me
- Hash passwords using BCrypt.
- Generate and validate JWT tokens.
- Implement USER and ADMIN roles.
- Add password validation.

## Acceptance Criteria
- [ ] Users can register and log in.
- [ ] Passwords are never stored in plain text.
- [ ] JWT authentication works.
- [ ] Protected endpoints require authentication.
- [ ] Role-based access is enforced.
- [ ] Input validation and error handling are implemented." \
backend security

create_issue "NEO-04: Implement Movie Service CRUD and Search" \
"## Description
Implement movie management and search functionality.

## Tasks
- Create, read, update and delete movies.
- GET /movies and GET /movies/{id}.
- POST /movies.
- PUT /movies/{id}.
- DELETE /movies/{id}.
- GET /movies/search.
- Search by title, genre and release year.
- Add pagination and sorting.
- Restrict movie management to ADMIN users.

## Acceptance Criteria
- [ ] Movie CRUD endpoints work.
- [ ] Search supports title, genre and year.
- [ ] Pagination and sorting work.
- [ ] Invalid input returns appropriate errors.
- [ ] Admin permissions are enforced." \
backend database

create_issue "NEO-05: Implement Rating Service" \
"## Description
Implement movie rating management and Neo4j relationships.

## Tasks
- POST /ratings.
- GET /ratings/me.
- GET /ratings/movie/{movieId}.
- PUT /ratings/{movieId}.
- DELETE /ratings/{movieId}.
- Create or update RATED relationships.
- Store rating timestamps.
- Validate rating values from 1 to 5.
- Calculate average movie ratings.

## Acceptance Criteria
- [ ] Authenticated users can rate movies.
- [ ] Each user has at most one rating per movie.
- [ ] Ratings can be updated and deleted.
- [ ] Rating values outside 1-5 are rejected.
- [ ] Timestamps are stored correctly." \
backend database

create_issue "NEO-06: Implement Graph-Based Recommendation Service" \
"## Description
Build personalized movie recommendations using Neo4j graph traversal.

## Tasks
- Find users with similar movie ratings.
- Recommend movies liked by similar users.
- Exclude movies already rated by the current user.
- Rank recommendations by relevance.
- Support genre and year filters.
- Add pagination.
- Implement a fallback for new users.

## API
GET /recommendations

## Acceptance Criteria
- [ ] Recommendations use Neo4j graph relationships.
- [ ] Already-rated movies are excluded.
- [ ] Results are ranked.
- [ ] Cold-start users receive fallback recommendations.
- [ ] Current user identity comes from validated JWT, not arbitrary client input." \
backend database

create_issue "NEO-07: Configure API Gateway and Centralized Security" \
"## Description
Configure Spring Cloud Gateway as the single entry point for frontend requests.

## Tasks
- Configure routes to all backend services.
- Validate JWT tokens.
- Enforce authentication and roles.
- Configure CORS.
- Add centralized error handling.
- Configure service URLs through environment variables.

## Routes
- /api/auth/**
- /api/users/**
- /api/movies/**
- /api/ratings/**
- /api/recommendations/**

## Acceptance Criteria
- [ ] Requests are routed to the correct service.
- [ ] Invalid tokens are rejected.
- [ ] Public and protected routes behave correctly.
- [ ] CORS is configured for Angular.
- [ ] Gateway health endpoint works." \
backend security

create_issue "NEO-08: Backend Integration and Automated Tests" \
"## Description
Add automated tests and verify communication between backend services.

## Tasks
- Add unit tests with JUnit.
- Add Spring Boot integration tests.
- Test Neo4j repositories and queries.
- Test registration and authentication.
- Test role-based access.
- Test movie CRUD and search.
- Test rating validation.
- Test recommendation logic.
- Test API Gateway routing.
- Document API endpoints.

## Acceptance Criteria
- [ ] Critical backend features have automated tests.
- [ ] Integration tests verify Neo4j behavior.
- [ ] Security scenarios are tested.
- [ ] Test execution is documented.
- [ ] API documentation is available." \
backend testing documentation

# ==================================================
# DEVELOPER 2 - ANGULAR, DOCKER & DEVOPS
# ==================================================

create_issue "NEO-09: Initialize Angular Application and UI Structure" \
"## Description
Create the Angular frontend and establish the application's structure.

## Tasks
- Initialize Angular application.
- Configure routing.
- Configure SCSS and shared styles.
- Create shared components and layouts.
- Create core and feature modules or standalone feature areas.
- Configure environment files.
- Establish responsive design.

## Acceptance Criteria
- [ ] Angular application starts successfully.
- [ ] Routing is configured.
- [ ] Shared layout and navigation exist.
- [ ] Responsive styling is established." \
frontend

create_issue "NEO-10: Implement Authentication UI" \
"## Description
Build registration, login and logout interfaces.

## Tasks
- Create login page.
- Create registration page.
- Connect forms to User Service APIs.
- Store and manage JWT securely.
- Add HTTP authentication interceptor.
- Add route guards.
- Implement logout.
- Display validation and API errors.

## Acceptance Criteria
- [ ] Users can register and log in through the UI.
- [ ] Protected routes require authentication.
- [ ] JWT is attached to authorized API requests.
- [ ] Logout clears the authentication session.
- [ ] Forms show validation errors." \
frontend security

create_issue "NEO-11: Implement Home Page and Movie Browsing" \
"## Description
Build the main page for browsing movies.

## Tasks
- Display movie cards.
- Show title, poster, genre and release year.
- Connect to Movie Service.
- Add loading, empty and error states.
- Add pagination.
- Create responsive movie grid.

## Acceptance Criteria
- [ ] Movies are loaded from the backend.
- [ ] Movie cards display essential information.
- [ ] Pagination works.
- [ ] Layout works on desktop and mobile." \
frontend

create_issue "NEO-12: Implement Movie Search and Filters" \
"## Description
Allow users to search and filter the movie catalog.

## Tasks
- Search by movie title.
- Filter by genre.
- Filter by release year.
- Support combined filters.
- Connect filters to backend search API.
- Add loading and no-results states.

## Acceptance Criteria
- [ ] Search returns matching movies.
- [ ] Genre and year filters work.
- [ ] Filters can be combined.
- [ ] Empty results are handled clearly." \
frontend

create_issue "NEO-13: Implement Movie Details and Rating UI" \
"## Description
Build the movie details page and rating interface.

## Tasks
- Display movie details and metadata.
- Display average rating.
- Show existing user rating.
- Allow authenticated users to rate movies from 1 to 5.
- Allow users to update or delete their rating.
- Connect to Movie and Rating services.

## Acceptance Criteria
- [ ] Movie details load correctly.
- [ ] Rating can be created, updated and deleted.
- [ ] Rating is restricted to authenticated users.
- [ ] Validation and API errors are displayed." \
frontend

create_issue "NEO-14: Implement Recommendations UI" \
"## Description
Build a personalized recommendations page.

## Tasks
- Connect to Recommendation Service.
- Display recommended movie cards.
- Show recommendation loading and error states.
- Add genre and year filters.
- Add pagination.
- Provide a useful empty-state message.

## Acceptance Criteria
- [ ] Recommendations are loaded for the logged-in user.
- [ ] Recommended movies can be opened.
- [ ] Filters and pagination work.
- [ ] Empty and error states are handled." \
frontend

create_issue "NEO-15: Implement Watchlist and Recommendation Sharing" \
"## Description
Allow users to manage a personal watchlist and share recommendations.

## Tasks
- Add movies to watchlist.
- Remove movies from watchlist.
- Display watchlist page.
- Add share recommendation functionality.
- Connect to backend APIs.
- Handle unauthorized requests.

## Acceptance Criteria
- [ ] Users can add and remove movies.
- [ ] Watchlist is associated with the authenticated user.
- [ ] Watchlist persists after reload.
- [ ] Sharing functionality works as designed." \
frontend

create_issue "NEO-16: Dockerize Services and Configure Docker Compose" \
"## Description
Containerize the application and configure local development deployment.

## Tasks
- Create Dockerfile for each backend service.
- Create Dockerfile for Angular frontend.
- Configure Neo4j container.
- Configure Docker Compose services and network.
- Add persistent Neo4j volume.
- Configure environment variables.
- Add health checks and startup dependencies.
- Document startup and shutdown commands.

## Acceptance Criteria
- [ ] All required services have Dockerfiles.
- [ ] Docker Compose starts the application stack.
- [ ] Neo4j data persists across restarts.
- [ ] Services can communicate over the Docker network.
- [ ] Environment secrets are not committed." \
devops database

create_issue "NEO-17: Configure HTTPS and Security Hardening" \
"## Description
Secure application traffic and implement additional security protections.

## Tasks
- Configure HTTPS using Nginx or Caddy reverse proxy.
- Configure TLS certificates for local development.
- Add secure HTTP headers.
- Review CORS and allowed origins.
- Configure secure cookie and token handling where applicable.
- Integrate two-factor authentication (2FA) with backend and frontend.
- Document production security configuration.

## Acceptance Criteria
- [ ] HTTPS works in the configured environment.
- [ ] HTTP redirects to HTTPS where appropriate.
- [ ] Security headers are configured.
- [ ] 2FA enrollment and verification work.
- [ ] Secrets and certificates are handled safely." \
devops security

create_issue "NEO-18: End-to-End Testing and Project Documentation" \
"## Description
Verify complete user workflows and prepare project documentation.

## Tasks
- Configure Playwright.
- Test registration and login.
- Test movie browsing and search.
- Test movie details and rating.
- Test recommendations.
- Test watchlist.
- Add screenshots.
- Document architecture and setup.
- Document environment variables and API usage.

## Acceptance Criteria
- [ ] Critical user journeys have E2E tests.
- [ ] Tests can run using documented commands.
- [ ] README explains installation and startup.
- [ ] Architecture and API documentation are available.
- [ ] Screenshots demonstrate the main features." \
frontend testing documentation

echo ""
echo "All 18 Neo4flix issue definitions have been processed."
echo "View issues: https://github.com/$REPO/issues"

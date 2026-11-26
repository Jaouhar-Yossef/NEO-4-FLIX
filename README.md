# NEO-4-FLIX

Movie recommendation engine: Neo4j graph database, Spring Boot microservices, Angular
frontend, all running in Docker.

| Part | What it does |
|---|---|
| `frontend` | Angular app served by nginx. The only published container: it also forwards `/api` to the gateway |
| `api-gateway` | Checks the JWT, routes `/api/**` to the services ([ADR 0001](docs/adr/0001-jwt-at-gateway.md)) |
| `user-service` | Register, login, 2FA, profile |
| `movie-service` | Movies, genres, search |
| `rating-service` | Ratings |
| `recommendation-service` | Recommendations (Neo4j Graph Data Science) |
| `neo4j` | Neo4j 5.26 Community + GDS 2.13 |
| `neo4j-init` | One-shot: loads the schema and the demo data, then exits |

## Requirements

- Docker with Docker Compose v2 (`docker compose version`)
- Git
- Internet on the first start (images, Maven and npm packages, GDS plugin)

## Start

```bash
git clone https://github.com/Jaouhar-Yossef/NEO-4-FLIX.git
cd NEO-4-FLIX
cp .env.example .env    # then change NEO4J_PASSWORD and JWT_SECRET
docker compose up -d --build --wait
```

`JWT_SECRET` needs at least 32 characters, for example `openssl rand -base64 48`.

The first start takes a few minutes (builds and downloads). `--wait` returns when every
container is healthy. Check with:

```bash
docker compose ps -a
```

All services show `healthy`; `neo4j-init` shows `Exited (0)`, which is normal (it only
loads the data).

| Open | Address |
|---|---|
| The app | http://localhost:4200 (`FRONTEND_PORT` in `.env`) |
| The API (Postman) | http://localhost:4200/api |
| Neo4j Browser | http://localhost:7474, user `neo4j`, password = `NEO4J_PASSWORD` |

Only the app port is published. Neo4j listens on `127.0.0.1` only; the gateway and the
services cannot be reached from outside Docker.

## Demo data

`neo4j-init` loads [`scripts/neo4j/03-demo-data.cypher`](scripts/neo4j/03-demo-data.cypher)
on every start (safe to run again): 10 genres, 50 movies, 20 demo users in 4 taste groups
and their ratings, so recommendations have something to work with.

Demo users have **no password**: nobody can log in as them. Register your own account in
the app (or `POST /api/auth/register`), then rate movies to get recommendations.

## Stop

```bash
docker compose down       # stops and removes the containers, keeps the database
docker compose up -d --wait   # start again (no --build needed if the code did not change)
```

Start from an empty database (**deletes all data**, including registered accounts):

```bash
docker compose down -v
```

After a code change: `docker compose up -d --build --wait`.

## Troubleshooting

```bash
docker compose ps -a                # which container is not healthy
docker compose logs <service>       # e.g. docker compose logs user-service
```

- `NEO4J_PASSWORD is required` / `JWT_SECRET is required`: `.env` is missing or incomplete.
- Port 4200 already in use (e.g. `ng serve`): set another `FRONTEND_PORT` in `.env`.
- `neo4j` stays unhealthy on the first start: it downloads the GDS plugin, which needs
  internet. Check `docker compose logs neo4j`.
- Changed `NEO4J_PASSWORD` after the first start: Neo4j keeps the old one in its volume.
  Put the old one back, or reset with `docker compose down -v`.

## Local development (without Docker for the services)

Run Neo4j in Docker and the services with Maven (`dev` profile):
see [docs/database/neo4j-setup-guide.md](docs/database/neo4j-setup-guide.md).
Frontend: `cd frontend && npm install && npm start` (http://localhost:4200, proxies `/api`
to the gateway on 8080).

## Docs

- [API contract](docs/api.md)
- [Backend automated testing](docs/TESTING.md)
- [Graph model](docs/database/neo4j-graph-model.md)
- [Neo4j setup and local development](docs/database/neo4j-setup-guide.md)
- [ADR 0001: JWT checked at the gateway](docs/adr/0001-jwt-at-gateway.md)

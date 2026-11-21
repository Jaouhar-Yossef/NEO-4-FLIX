# NEO-4-FLIX – Neo4j Database Setup Guide

## 1. Introduction

This guide explains how to activate and use the Neo4j database in the NEO-4-FLIX project.

To run the whole application in Docker, follow the [README](../../README.md) instead: the
`neo4j-init` container loads the schema and the demo data by itself. This guide is for
local development, with the services run by Maven.

The project uses **Neo4j 5 Community Edition** (`neo4j:5.26.33-community`) with the
**Graph Data Science** plugin (GDS 2.13) as its primary database.

The database stores:
- Users
- Movies
- Genres
- Ratings
- Watchlists
- Friendships

## 2. Requirements

Before starting, make sure you have installed:

- Docker
- Docker Compose
- Git
- Java 21
- Maven

## 3. Clone the project

Clone the repository:

```bash
git clone https://github.com/Jaouhar-Yossef/NEO-4-FLIX.git
cd NEO-4-FLIX
```

Make sure you are on the latest project branch or pull the latest changes from `main`.

## 4. Configure environment variables

Create your local `.env` file if it does not already exist.

```bash
cp .env.example .env
```

If `.env.example` does not exist, create `.env` manually using the following structure:

```env
NEO4J_URI=bolt://localhost:7687
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=your_secure_password
```

**Important:**
- Use the same Neo4j username and password when configuring the database and connecting the Spring Boot services.
- Never commit your `.env` file or share your real password.

## 5. Start the Neo4j database

From the project root, run:

```bash
docker compose up -d neo4j
```

This command starts the Neo4j container in the background.

Check the container:

```bash
docker compose ps
```

Wait until the container status becomes `healthy`. The first start also downloads the GDS
plugin (internet needed). Check it in Neo4j Browser with `RETURN gds.version();`.

The database exposes these local ports:

| Service | Address |
|---|---|
| Neo4j Browser | http://localhost:7474 |
| Bolt connection | bolt://localhost:7687 |

## 6. Open Neo4j Browser

Open your browser:

http://localhost:7474

Log in using:

- Username: `neo4j`
- Password: The value configured in `.env`

You can now interact with the graph database using Cypher queries.

## 7. Create the graph schema

The project contains a schema initialization script:

```text
scripts/neo4j/01-schema.cypher
```

This script creates:

**Constraints:**
- Unique User ID
- Unique User email
- Unique Movie ID
- Unique Genre ID

**Indexes:**
- Movie title index
- Genre name index

Load the schema from the project root:

```bash
set -a
source ./.env   # "./" matters: a bare ".env" is looked up in $PATH first
set +a

docker exec -i neo4flix-neo4j cypher-shell \
  -u "$NEO4J_USERNAME" \
  -p "$NEO4J_PASSWORD" \
  < scripts/neo4j/01-schema.cypher
```

## 8. Load development seed data

The project provides sample data in:

```text
scripts/neo4j/02-seed-data.cypher
```

It creates example users, movies, genres, and relationships.

Load the data:

```bash
docker exec -i neo4flix-neo4j cypher-shell \
  -u "$NEO4J_USERNAME" \
  -p "$NEO4J_PASSWORD" \
  < scripts/neo4j/02-seed-data.cypher
```

The seed script uses `MERGE` to avoid creating duplicate nodes and relationships when executed repeatedly.

The seed users (`alice@neo4flix.dev`, `bob@neo4flix.dev`, `charlie@neo4flix.dev`) share a
known password, written in the script. That is why `02-seed-data.cypher` is for local
development only and is never loaded in Docker.

### Demo data (loaded automatically in Docker)

`scripts/neo4j/03-demo-data.cypher` holds the larger data set used in Docker: 10 genres,
50 movies, 20 demo users in 4 taste groups (no password, nobody can log in as them) and
their ratings. The `neo4j-init` container loads `01-schema.cypher` and this script on every
`docker compose up`. To load both locally without the rest of the stack:

```bash
docker compose up neo4j-init
```

It uses the same ids as `02-seed-data.cypher` for the first 3 genres and movies, so both
scripts can be loaded in the same database without duplicates.

## 9. Understand the graph model

### Nodes

| Node | Description |
|---|---|
| User | Represents a registered user |
| Movie | Represents a movie |
| Genre | Represents a movie category |

### Relationships

| Relationship | Structure | Description |
|---|---|---|
| RATED | User → Movie | A user rates a movie. Contains `rating` and `createdAt`. |
| HAS_GENRE | Movie → Genre | Connects a movie to its genre. |
| WATCHLIST | User → Movie | Adds a movie to a user's watchlist. |
| FRIEND | User → User | Represents a friendship connection. |

Example:

```cypher
MATCH (u:User)-[r:RATED]->(m:Movie)
RETURN u, r, m;
```

## 10. Verify the database

Open Neo4j Browser and execute:

**Check all nodes:**

```cypher
MATCH (n)
RETURN labels(n) AS NodeType, count(n) AS Total;
```

**Check all relationships:**

```cypher
MATCH ()-[r]->()
RETURN type(r) AS RelationshipType, count(r) AS Total;
```

**Check constraints:**

```cypher
SHOW CONSTRAINTS;
```

**Check indexes:**

```cypher
SHOW INDEXES;
```

You should find the configured constraints and indexes, as well as the development sample data.

## 11. Connect Spring Boot services

The four backend services use Spring Data Neo4j:

- user-service
- movie-service
- rating-service
- recommendation-service

Their Neo4j connection is configured in:

```text
src/main/resources/application.yml
```

The configuration uses environment variables:

```yaml
spring:
  neo4j:
    uri: ${NEO4J_URI:bolt://localhost:7687}
    authentication:
      username: ${NEO4J_USERNAME:neo4j}
      password: ${NEO4J_PASSWORD:change_me}
```

Make sure the credentials match your `.env` configuration.

For local development, activate the `dev` profile.

Example:

```bash
mvn -f backend/user-service/pom.xml spring-boot:run \
  -Dspring-boot.run.profiles=dev
```

Repeat for the other services when needed.

Development service ports:

| Service | Port |
|---|---:|
| user-service | 8081 |
| movie-service | 8182 |
| rating-service | 8083 |
| recommendation-service | 8084 |

## 12. Verify Spring Boot database connectivity

After starting a service, open its health endpoint.

For example:

```bash
curl http://localhost:8081/actuator/health
```

Repeat using the other service ports.

A successful connection should show:

```json
{
  "status": "UP",
  "components": {
    "neo4j": {
      "status": "UP"
    }
  }
}
```

The exact response may include additional database details.

## 13. Stop the database

To stop the Neo4j container:

```bash
docker compose stop neo4j
```

To start it again:

```bash
docker compose start neo4j
```

To stop and remove the container:

```bash
docker compose down
```

**Warning:** Do not use `docker compose down -v` unless you intentionally want to delete the persistent database volumes and their data.

## 14. Troubleshooting

### Container is not running

```bash
docker compose ps
docker compose logs neo4j
```

### Connection refused

Check that Neo4j is running and port `7687` is available:

```bash
docker compose ps
```

### Authentication failed

Check that the username and password in `.env` match the credentials configured for the Neo4j database.

### Spring Boot cannot connect

Check:
1. Neo4j container is healthy.
2. `NEO4J_URI` points to the correct address.
3. Username and password are correct.
4. The service is running with the intended Spring profile.

---

## Final Notes

The normal development startup sequence is:

1. Start Docker.
2. Start Neo4j using `docker compose up -d neo4j`.
3. Load the schema if the database has not been initialized (or `docker compose up neo4j-init`,
   which loads the schema and the demo data).
4. Load the development seed data if sample data is needed.
5. Start the Spring Boot services.
6. Verify the database connection through Actuator.

**You do not need to recreate the schema and seed data every time you start the project.** The Neo4j Docker volumes preserve the database data between restarts.
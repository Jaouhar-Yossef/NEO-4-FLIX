// NEO-4-FLIX - Neo4j Graph Schema

// User constraints
CREATE CONSTRAINT user_id_unique IF NOT EXISTS
FOR (u:User)
REQUIRE u.id IS UNIQUE;

CREATE CONSTRAINT user_email_unique IF NOT EXISTS
FOR (u:User)
REQUIRE u.email IS UNIQUE;

// Movie constraints
CREATE CONSTRAINT movie_id_unique IF NOT EXISTS
FOR (m:Movie)
REQUIRE m.id IS UNIQUE;

// Genre constraints
CREATE CONSTRAINT genre_id_unique IF NOT EXISTS
FOR (g:Genre)
REQUIRE g.id IS UNIQUE;

// Search indexes
CREATE INDEX movie_title_index IF NOT EXISTS
FOR (m:Movie)
ON (m.title);

CREATE INDEX genre_name_index IF NOT EXISTS
FOR (g:Genre)
ON (g.name);

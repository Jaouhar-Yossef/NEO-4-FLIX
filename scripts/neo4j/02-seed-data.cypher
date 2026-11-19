// ============================================
// NEO-4-FLIX - Development Seed Data
// ============================================

// Users
MERGE (u1:User {id: 'user-001'})
SET u1.email = 'alice@neo4flix.dev',
    u1.displayName = 'Alice';

MERGE (u2:User {id: 'user-002'})
SET u2.email = 'bob@neo4flix.dev',
    u2.displayName = 'Bob';

MERGE (u3:User {id: 'user-003'})
SET u3.email = 'charlie@neo4flix.dev',
    u3.displayName = 'Charlie';

// Genres
MERGE (g1:Genre {id: 'genre-001'})
SET g1.name = 'Science Fiction';

MERGE (g2:Genre {id: 'genre-002'})
SET g2.name = 'Action';

MERGE (g3:Genre {id: 'genre-003'})
SET g3.name = 'Drama';

// Movies
MERGE (m1:Movie {id: 'movie-001'})
SET m1.title = 'The Matrix',
    m1.releaseYear = 1999;

MERGE (m2:Movie {id: 'movie-002'})
SET m2.title = 'Inception',
    m2.releaseYear = 2010;

MERGE (m3:Movie {id: 'movie-003'})
SET m3.title = 'The Dark Knight',
    m3.releaseYear = 2008;

// Movie genres
MATCH (m:Movie {id: 'movie-001'}), (g:Genre {id: 'genre-001'})
MERGE (m)-[:HAS_GENRE]->(g);

MATCH (m:Movie {id: 'movie-002'}), (g:Genre {id: 'genre-001'})
MERGE (m)-[:HAS_GENRE]->(g);

MATCH (m:Movie {id: 'movie-003'}), (g:Genre {id: 'genre-002'})
MERGE (m)-[:HAS_GENRE]->(g);

MATCH (m:Movie {id: 'movie-003'}), (g:Genre {id: 'genre-003'})
MERGE (m)-[:HAS_GENRE]->(g);

// Ratings
MATCH (u:User {id: 'user-001'}), (m:Movie {id: 'movie-001'})
MERGE (u)-[r:RATED]->(m)
SET r.rating = 5,
    r.createdAt = datetime('2026-11-15T10:00:00');

MATCH (u:User {id: 'user-001'}), (m:Movie {id: 'movie-002'})
MERGE (u)-[r:RATED]->(m)
SET r.rating = 4,
    r.createdAt = datetime('2026-11-15T10:05:00');

MATCH (u:User {id: 'user-002'}), (m:Movie {id: 'movie-001'})
MERGE (u)-[r:RATED]->(m)
SET r.rating = 4,
    r.createdAt = datetime('2026-11-15T10:10:00');

// Watchlists
MATCH (u:User {id: 'user-002'}), (m:Movie {id: 'movie-002'})
MERGE (u)-[w:WATCHLIST]->(m)
SET w.createdAt = datetime('2026-11-15T10:15:00');

// Friendship
MATCH (u1:User {id: 'user-001'}), (u2:User {id: 'user-002'})
MERGE (u1)-[f:FRIEND]->(u2)
SET f.createdAt = datetime('2026-11-15T10:20:00');

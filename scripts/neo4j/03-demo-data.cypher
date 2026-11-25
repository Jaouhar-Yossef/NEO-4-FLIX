// ============================================
// NEO-4-FLIX - Demo data (Docker)
// ============================================
// Loaded by the neo4j-init container after 01-schema.cypher, on every "docker compose up".
// Safe to run again: MERGE everywhere, fixed values, nothing random.
// 02-seed-data.cypher stays dev-only (it has a known password). Same ids as 02 for
// genre-001..003 and movie-001..003, so loading both creates no duplicates.

// Genres
UNWIND [
  {id: 'genre-001', name: 'Science Fiction'},
  {id: 'genre-002', name: 'Action'},
  {id: 'genre-003', name: 'Drama'},
  {id: 'genre-004', name: 'Comedy'},
  {id: 'genre-005', name: 'Animation'},
  {id: 'genre-006', name: 'Thriller'},
  {id: 'genre-007', name: 'Romance'},
  {id: 'genre-008', name: 'Crime'},
  {id: 'genre-009', name: 'Fantasy'},
  {id: 'genre-010', name: 'Horror'}
] AS row
MERGE (g:Genre {id: row.id})
SET g.name = row.name;

// Movies and their genres
UNWIND [
  {id: 'movie-001', title: "The Matrix", year: 1999, genres: ['Science Fiction', 'Action']},
  {id: 'movie-002', title: "Inception", year: 2010, genres: ['Science Fiction', 'Action', 'Thriller']},
  {id: 'movie-003', title: "The Dark Knight", year: 2008, genres: ['Action', 'Crime', 'Drama']},
  {id: 'movie-004', title: "Interstellar", year: 2014, genres: ['Science Fiction', 'Drama']},
  {id: 'movie-005', title: "Blade Runner", year: 1982, genres: ['Science Fiction', 'Thriller']},
  {id: 'movie-006', title: "Alien", year: 1979, genres: ['Science Fiction', 'Horror']},
  {id: 'movie-007', title: "Arrival", year: 2016, genres: ['Science Fiction', 'Drama']},
  {id: 'movie-008', title: "Dune", year: 2021, genres: ['Science Fiction']},
  {id: 'movie-009', title: "The Terminator", year: 1984, genres: ['Science Fiction', 'Action']},
  {id: 'movie-010', title: "Back to the Future", year: 1985, genres: ['Science Fiction', 'Comedy']},
  {id: 'movie-011', title: "The Lord of the Rings: The Fellowship of the Ring", year: 2001, genres: ['Fantasy']},
  {id: 'movie-012', title: "Harry Potter and the Philosopher's Stone", year: 2001, genres: ['Fantasy']},
  {id: 'movie-013', title: "Pan's Labyrinth", year: 2006, genres: ['Fantasy', 'Drama']},
  {id: 'movie-014', title: "Mad Max: Fury Road", year: 2015, genres: ['Action', 'Science Fiction']},
  {id: 'movie-015', title: "Die Hard", year: 1988, genres: ['Action', 'Thriller']},
  {id: 'movie-016', title: "Gladiator", year: 2000, genres: ['Action', 'Drama']},
  {id: 'movie-017', title: "John Wick", year: 2014, genres: ['Action', 'Thriller']},
  {id: 'movie-018', title: "Heat", year: 1995, genres: ['Crime', 'Thriller']},
  {id: 'movie-019', title: "Pulp Fiction", year: 1994, genres: ['Crime', 'Drama']},
  {id: 'movie-020', title: "The Godfather", year: 1972, genres: ['Crime', 'Drama']},
  {id: 'movie-021', title: "Goodfellas", year: 1990, genres: ['Crime', 'Drama']},
  {id: 'movie-022', title: "Se7en", year: 1995, genres: ['Crime', 'Thriller']},
  {id: 'movie-023', title: "The Silence of the Lambs", year: 1991, genres: ['Thriller', 'Crime', 'Horror']},
  {id: 'movie-024', title: "Gone Girl", year: 2014, genres: ['Thriller', 'Drama']},
  {id: 'movie-025', title: "Parasite", year: 2019, genres: ['Thriller', 'Drama', 'Comedy']},
  {id: 'movie-026', title: "The Shawshank Redemption", year: 1994, genres: ['Drama']},
  {id: 'movie-027', title: "Forrest Gump", year: 1994, genres: ['Drama', 'Romance']},
  {id: 'movie-028', title: "Schindler's List", year: 1993, genres: ['Drama']},
  {id: 'movie-029', title: "Whiplash", year: 2014, genres: ['Drama']},
  {id: 'movie-030', title: "Titanic", year: 1997, genres: ['Romance', 'Drama']},
  {id: 'movie-031', title: "La La Land", year: 2016, genres: ['Romance', 'Comedy', 'Drama']},
  {id: 'movie-032', title: "Pride & Prejudice", year: 2005, genres: ['Romance', 'Drama']},
  {id: 'movie-033', title: "The Notebook", year: 2004, genres: ['Romance', 'Drama']},
  {id: 'movie-034', title: "Amélie", year: 2001, genres: ['Romance', 'Comedy']},
  {id: 'movie-035', title: "Before Sunrise", year: 1995, genres: ['Romance', 'Drama']},
  {id: 'movie-036', title: "Toy Story", year: 1995, genres: ['Animation', 'Comedy']},
  {id: 'movie-037', title: "Spirited Away", year: 2001, genres: ['Animation', 'Fantasy']},
  {id: 'movie-038', title: "Finding Nemo", year: 2003, genres: ['Animation', 'Comedy']},
  {id: 'movie-039', title: "Up", year: 2009, genres: ['Animation', 'Comedy', 'Drama']},
  {id: 'movie-040', title: "Shrek", year: 2001, genres: ['Animation', 'Comedy', 'Fantasy']},
  {id: 'movie-041', title: "Coco", year: 2017, genres: ['Animation', 'Fantasy']},
  {id: 'movie-042', title: "Inside Out", year: 2015, genres: ['Animation', 'Comedy']},
  {id: 'movie-043', title: "The Grand Budapest Hotel", year: 2014, genres: ['Comedy']},
  {id: 'movie-044', title: "Groundhog Day", year: 1993, genres: ['Comedy', 'Romance', 'Fantasy']},
  {id: 'movie-045', title: "Superbad", year: 2007, genres: ['Comedy']},
  {id: 'movie-046', title: "The Big Lebowski", year: 1998, genres: ['Comedy', 'Crime']},
  {id: 'movie-047', title: "The Shining", year: 1980, genres: ['Horror']},
  {id: 'movie-048', title: "Get Out", year: 2017, genres: ['Horror', 'Thriller']},
  {id: 'movie-049', title: "Hereditary", year: 2018, genres: ['Horror', 'Drama']},
  {id: 'movie-050', title: "A Quiet Place", year: 2018, genres: ['Horror', 'Science Fiction']}
] AS row
MERGE (m:Movie {id: row.id})
SET m.title = row.title,
    m.releaseYear = row.year
WITH m, row
UNWIND row.genres AS genreName
MATCH (g:Genre {name: genreName})
MERGE (m)-[:HAS_GENRE]->(g);

// Demo users in 4 taste groups, and their ratings.
// No passwordHash: nobody can log in as a demo user (login refuses a user without one).
// Rating rule, per user and movie (un = user number, mn = movie number):
// - movie in one of the group's genres: rated by most users of the group (skipped when
//   (un + mn) % 5 = 0), rating 4 or 5;
// - any other movie: rated by a few users only ((un * 3 + mn) % 7 = 0), rating 1 or 2.
// Users of one group share many high ratings: that is what the recommendations find.
UNWIND [
  {likes: ['Science Fiction', 'Fantasy'], users: [
    {id: 'demo-user-01', name: 'Nova'}, {id: 'demo-user-02', name: 'Orion'},
    {id: 'demo-user-03', name: 'Lyra'}, {id: 'demo-user-04', name: 'Atlas'},
    {id: 'demo-user-05', name: 'Vega'}]},
  {likes: ['Action', 'Thriller', 'Crime'], users: [
    {id: 'demo-user-06', name: 'Rex'}, {id: 'demo-user-07', name: 'Blaze'},
    {id: 'demo-user-08', name: 'Kane'}, {id: 'demo-user-09', name: 'Dex'},
    {id: 'demo-user-10', name: 'Jet'}]},
  {likes: ['Drama', 'Romance'], users: [
    {id: 'demo-user-11', name: 'Rose'}, {id: 'demo-user-12', name: 'Elena'},
    {id: 'demo-user-13', name: 'Jules'}, {id: 'demo-user-14', name: 'Clara'},
    {id: 'demo-user-15', name: 'Noah'}]},
  {likes: ['Animation', 'Comedy'], users: [
    {id: 'demo-user-16', name: 'Pip'}, {id: 'demo-user-17', name: 'Milo'},
    {id: 'demo-user-18', name: 'Kiki'}, {id: 'demo-user-19', name: 'Toby'},
    {id: 'demo-user-20', name: 'Lulu'}]}
] AS taste
UNWIND taste.users AS row
MERGE (u:User {id: row.id})
SET u.email = row.id + '@example.com',
    u.displayName = row.name,
    u.role = 'USER',
    u.twoFactorEnabled = false
WITH u, taste
MATCH (m:Movie)
WITH u, m,
     toInteger(right(u.id, 2)) AS un,
     toInteger(right(m.id, 3)) AS mn,
     EXISTS { (m)-[:HAS_GENRE]->(g:Genre) WHERE g.name IN taste.likes } AS liked
WHERE (liked AND (un + mn) % 5 <> 0)
   OR (NOT liked AND (un * 3 + mn) % 7 = 0)
MERGE (u)-[r:RATED]->(m)
SET r.rating = CASE WHEN liked THEN 4 + (un + mn) % 2 ELSE 1 + (un + mn) % 2 END,
    r.createdAt = datetime('2026-11-01T12:00:00Z') + duration({minutes: un * 100 + mn});

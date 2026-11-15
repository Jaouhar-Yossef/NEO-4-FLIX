# Neo4flix - Neo4j Graph Model

## 1. Nodes

### User
Represents a registered user.
- id: unique identifier
- email: unique email address

### Movie
Represents a movie.
- id: unique identifier
- title: movie title

### Genre
Represents a movie category.
- id: unique identifier
- name: genre name

## 2. Relationships

### RATED
(User)-[:RATED {rating, createdAt}]->(Movie)

A user rates a movie.

### HAS_GENRE
(Movie)-[:HAS_GENRE]->(Genre)

A movie belongs to a genre.

### WATCHLIST
(User)-[:WATCHLIST {createdAt}]->(Movie)

A user adds a movie to their watchlist.

### FRIEND
(User)-[:FRIEND {createdAt}]->(User)

Represents a friendship connection. The relationship is directed in the graph; application logic must handle reciprocal friendship if required.

## 3. Constraints

- User.id must be unique.
- User.email must be unique.
- Movie.id must be unique.
- Genre.id must be unique.

## 4. Indexes

- Movie.title
- Genre.name

## 5. Database

- Database: neo4j
- Engine: Neo4j 5 Community

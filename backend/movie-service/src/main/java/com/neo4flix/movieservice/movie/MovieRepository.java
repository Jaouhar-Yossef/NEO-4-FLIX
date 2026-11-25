package com.neo4flix.movieservice.movie;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import com.neo4flix.movieservice.movie.dto.MovieDetailsRecord;
import com.neo4flix.movieservice.movie.dto.MovieSearchRecord;

public interface MovieRepository extends Neo4jRepository<Movie, String> {

    @Query("""
            MATCH (m:Movie)
            WHERE ($title IS NULL OR toLower(m.title) CONTAINS toLower($title))
              AND ($year IS NULL OR m.releaseYear = $year)
              AND (
                    $genre IS NULL
                    OR EXISTS {
                        MATCH (m)-[:HAS_GENRE]->(filterGenre:Genre)
                        WHERE toLower(filterGenre.name) = toLower($genre)
                    }
              )

            WITH m
            ORDER BY
                CASE WHEN $sortBy = 'title' AND $direction = 'ASC'
                     THEN toLower(m.title) END ASC,
                CASE WHEN $sortBy = 'title' AND $direction = 'DESC'
                     THEN toLower(m.title) END DESC,
                CASE WHEN $sortBy = 'releaseYear' AND $direction = 'ASC'
                     THEN m.releaseYear END ASC,
                CASE WHEN $sortBy = 'releaseYear' AND $direction = 'DESC'
                     THEN m.releaseYear END DESC,
                m.id ASC

            SKIP $skip
            LIMIT $limit

            WITH collect(m) AS pageMovies
            UNWIND CASE
                WHEN size(pageMovies) = 0 THEN []
                ELSE range(0, size(pageMovies) - 1)
            END AS position

            WITH pageMovies[position] AS m, position

            OPTIONAL MATCH (m)-[:HAS_GENRE]->(g:Genre)

            CALL (m) {
                OPTIONAL MATCH (:User)-[r:RATED]->(m)
                RETURN avg(r.rating) AS averageRating,
                       count(r) AS ratingCount
            }

            WITH m, position, g.name AS genre, averageRating, ratingCount
            ORDER BY position ASC, genre ASC

            RETURN m.id AS id,
                   m.title AS title,
                   m.releaseYear AS releaseYear,
                   genre AS genre,
                   averageRating AS averageRating,
                   ratingCount AS ratingCount
            """)
    List<MovieSearchRecord> searchMovies(
            @Param("title") String title,
            @Param("genre") String genre,
            @Param("year") Integer year,
            @Param("sortBy") String sortBy,
            @Param("direction") String direction,
            @Param("skip") int skip,
            @Param("limit") int limit);

    @Query("""
            MATCH (m:Movie)
            WHERE ($title IS NULL OR toLower(m.title) CONTAINS toLower($title))
              AND ($year IS NULL OR m.releaseYear = $year)
              AND (
                    $genre IS NULL
                    OR EXISTS {
                        MATCH (m)-[:HAS_GENRE]->(filterGenre:Genre)
                        WHERE toLower(filterGenre.name) = toLower($genre)
                    }
              )
            RETURN count(m)
            """)
    long countMovies(
            @Param("title") String title,
            @Param("genre") String genre,
            @Param("year") Integer year);

    @Query("MATCH (m:Movie {id: $id}) DETACH DELETE m")
    void deleteWithRelationships(String id);

    @Query("""
            MATCH (m:Movie {id: $id})

            OPTIONAL MATCH (m)-[:HAS_GENRE]->(g:Genre)

            CALL (m) {
                OPTIONAL MATCH (:User)-[r:RATED]->(m)
                RETURN avg(r.rating) AS averageRating,
                       count(r) AS ratingCount
            }

            CALL (m) {
                OPTIONAL MATCH (u:User {id: $userId})-[r:RATED]->(m)
                RETURN collect(r.rating)[0] AS myRating
            }

            CALL (m) {
                OPTIONAL MATCH (u:User {id: $userId})-[w:WATCHLIST]->(m)
                RETURN count(w) > 0 AS inWatchlist
            }

            RETURN m.id AS id,
                   m.title AS title,
                   m.releaseYear AS releaseYear,
                   g.name AS genre,
                   averageRating AS averageRating,
                   ratingCount AS ratingCount,
                   myRating AS myRating,
                   inWatchlist AS inWatchlist

            ORDER BY genre ASC
            """)
    List<MovieDetailsRecord> findMovieDetails(
            @Param("id") String id,
            @Param("userId") String userId);
}

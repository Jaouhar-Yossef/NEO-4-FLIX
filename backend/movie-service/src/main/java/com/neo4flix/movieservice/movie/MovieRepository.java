package com.neo4flix.movieservice.movie;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

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

            CALL {
                WITH m
                OPTIONAL MATCH (:User)-[r:RATED]->(m)
                RETURN avg(r.rating) AS averageRating,
                       count(r) AS ratingCount
            }

            OPTIONAL MATCH (m)-[:HAS_GENRE]->(g:Genre)

            WITH m, averageRating, ratingCount,
                 collect(DISTINCT g.name) AS genres

            RETURN m.id AS id,
                   m.title AS title,
                   m.releaseYear AS releaseYear,
                   genres AS genres,
                   averageRating AS averageRating,
                   ratingCount AS ratingCount

            ORDER BY m.title ASC, m.id ASC
            SKIP $skip
            LIMIT $limit
            """)
    List<MovieSearchRecord> searchMovies(
            @Param("title") String title,
            @Param("genre") String genre,
            @Param("year") Integer year,
            @Param("skip") int skip,
            @Param("limit") int limit
    );

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
            @Param("year") Integer year
    );

    @Query("MATCH (m:Movie {id: $id}) DETACH DELETE m")
    void deleteWithRelationships(String id);
}

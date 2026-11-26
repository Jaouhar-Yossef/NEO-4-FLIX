package com.neo4flix.recommendationservice.recommendation;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.driver.Record;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import com.neo4flix.recommendationservice.recommendation.RecommendationDtos.RecommendationResponse;

@Repository
public class RecommendationRepository {

    private final Neo4jClient neo4jClient;

    public RecommendationRepository(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    public boolean userExists(String userId) {
        return neo4jClient.query("""
                MATCH (u:User {id: $userId})
                RETURN count(u) > 0 AS result
                """)
                .bind(userId).to("userId")
                .fetchAs(Boolean.class)
                .one()
                .orElse(false);
    }

    public boolean hasRatings(String userId) {
        return neo4jClient.query("""
                MATCH (:User {id: $userId})-[r:RATED]->(:Movie)
                RETURN count(r) > 0 AS result
                """)
                .bind(userId).to("userId")
                .fetchAs(Boolean.class)
                .one()
                .orElse(false);
    }

    public List<RecommendationResponse> findPersonalized(
            String userId,
            String genre,
            int year,
            int skip,
            int limit) {

        return new ArrayList<>(neo4jClient.query("""
                MATCH (me:User {id: $userId})-[my:RATED]->(shared:Movie)
                      <-[their:RATED]-(other:User)
                WHERE other.id <> $userId

                WITH other,
                     count(DISTINCT shared) AS sharedCount,
                     avg(abs(toFloat(my.rating) - toFloat(their.rating)))
                         AS avgDifference

                WITH other,
                     (toFloat(sharedCount) / (toFloat(sharedCount) + 2.0))
                     / (1.0 + avgDifference) AS similarity

                MATCH (other)-[candidateRating:RATED]->(movie:Movie)
                WHERE candidateRating.rating >= 4
                  AND NOT EXISTS {
                      MATCH (me:User {id: $userId})-[:RATED]->(movie)
                  }
                  AND ($year = -1 OR movie.releaseYear = $year)
                  AND (
                      $genre = ''
                      OR EXISTS {
                          MATCH (movie)-[:HAS_GENRE]->(filterGenre:Genre)
                          WHERE toLower(filterGenre.name) = toLower($genre)
                      }
                  )

                WITH movie,
                     collect({
                         similarity: similarity,
                         rating: toFloat(candidateRating.rating)
                     }) AS supporters

                WITH movie,
                     supporters,
                     reduce(
                         weighted = 0.0,
                         item IN supporters |
                         weighted + item.similarity * item.rating
                     ) /
                     reduce(
                         weights = 0.0,
                         item IN supporters |
                         weights + item.similarity
                     ) AS relevanceScore,
                     size(supporters) AS similarUsersCount

                CALL (movie) {
                    OPTIONAL MATCH (:User)-[r:RATED]->(movie)
                    RETURN avg(r.rating) AS averageRating,
                           count(r) AS ratingCount
                }

                OPTIONAL MATCH (movie)-[:HAS_GENRE]->(g:Genre)

                WITH movie,
                     relevanceScore,
                     similarUsersCount,
                     averageRating,
                     ratingCount,
                     collect(DISTINCT g.name) AS genres

                RETURN movie.id AS id,
                       movie.title AS title,
                       movie.releaseYear AS releaseYear,
                       genres,
                       averageRating,
                       ratingCount,
                       relevanceScore,
                       toInteger(similarUsersCount) AS similarUsersCount

                ORDER BY relevanceScore DESC,
                         averageRating DESC,
                         ratingCount DESC,
                         title ASC

                SKIP $skip
                LIMIT $limit
                """)
                .bind(userId).to("userId")
                .bind(genre).to("genre")
                .bind(year).to("year")
                .bind(skip).to("skip")
                .bind(limit).to("limit")
                .fetchAs(RecommendationResponse.class)
                .mappedBy((types, record) -> mapRecommendation(record))
                .all());
    }

    public long countPersonalized(
            String userId,
            String genre,
            int year) {

        return neo4jClient.query("""
                MATCH (me:User {id: $userId})-[my:RATED]->(shared:Movie)
                      <-[their:RATED]-(other:User)
                WHERE other.id <> $userId

                WITH other,
                     avg(abs(toFloat(my.rating) - toFloat(their.rating)))
                         AS avgDifference

                WITH other,
                     1.0 / (1.0 + avgDifference) AS similarity

                MATCH (other)-[candidateRating:RATED]->(movie:Movie)
                WHERE candidateRating.rating >= 4
                  AND NOT EXISTS {
                      MATCH (me:User {id: $userId})-[:RATED]->(movie)
                  }
                  AND ($year = -1 OR movie.releaseYear = $year)
                  AND (
                      $genre = ''
                      OR EXISTS {
                          MATCH (movie)-[:HAS_GENRE]->(filterGenre:Genre)
                          WHERE toLower(filterGenre.name) = toLower($genre)
                      }
                  )

                WITH DISTINCT movie
                RETURN count(movie) AS total
                """)
                .bind(userId).to("userId")
                .bind(genre).to("genre")
                .bind(year).to("year")
                .fetchAs(Long.class)
                .one()
                .orElse(0L);
    }

    public List<RecommendationResponse> findPopular(
            String genre,
            int year,
            int skip,
            int limit) {

        return new ArrayList<>(neo4jClient.query("""
                MATCH (movie:Movie)
                WHERE ($year = -1 OR movie.releaseYear = $year)
                  AND (
                      $genre = ''
                      OR EXISTS {
                          MATCH (movie)-[:HAS_GENRE]->(filterGenre:Genre)
                          WHERE toLower(filterGenre.name) = toLower($genre)
                      }
                  )

                OPTIONAL MATCH (:User)-[r:RATED]->(movie)

                WITH movie,
                     avg(r.rating) AS averageRating,
                     count(r) AS ratingCount

                OPTIONAL MATCH (movie)-[:HAS_GENRE]->(g:Genre)

                WITH movie,
                     averageRating,
                     ratingCount,
                     collect(DISTINCT g.name) AS genres

                RETURN movie.id AS id,
                       movie.title AS title,
                       movie.releaseYear AS releaseYear,
                       genres,
                       averageRating,
                       ratingCount,
                       coalesce(averageRating, 0.0)
                           + 0.1 * log(1.0 + ratingCount) AS relevanceScore,
                       0 AS similarUsersCount

                ORDER BY relevanceScore DESC,
                         ratingCount DESC,
                         title ASC

                SKIP $skip
                LIMIT $limit
                """)
                .bind(genre).to("genre")
                .bind(year).to("year")
                .bind(skip).to("skip")
                .bind(limit).to("limit")
                .fetchAs(RecommendationResponse.class)
                .mappedBy((types, record) -> mapRecommendation(record))
                .all());
    }

    public long countPopular(String genre, int year) {
        return neo4jClient.query("""
                MATCH (movie:Movie)
                WHERE ($year = -1 OR movie.releaseYear = $year)
                  AND (
                      $genre = ''
                      OR EXISTS {
                          MATCH (movie)-[:HAS_GENRE]->(filterGenre:Genre)
                          WHERE toLower(filterGenre.name) = toLower($genre)
                      }
                  )
                RETURN count(movie) AS total
                """)
                .bind(genre).to("genre")
                .bind(year).to("year")
                .fetchAs(Long.class)
                .one()
                .orElse(0L);
    }

    private RecommendationResponse mapRecommendation(Record record) {
        return new RecommendationResponse(
                record.get("id").asString(),
                record.get("title").asString(),
                record.get("releaseYear").isNull()
                        ? null : record.get("releaseYear").asInt(),
                record.get("genres").asList(value -> value.asString()),
                record.get("averageRating").isNull()
                        ? null : record.get("averageRating").asDouble(),
                record.get("ratingCount").asLong(),
                record.get("relevanceScore").asDouble(),
                record.get("similarUsersCount").asLong()
        );
    }
}

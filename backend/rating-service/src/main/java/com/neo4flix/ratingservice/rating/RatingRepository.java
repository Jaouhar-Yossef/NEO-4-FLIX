package com.neo4flix.ratingservice.rating;

import java.util.Collection;
import java.util.Optional;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import com.neo4flix.ratingservice.rating.RatingDtos.RatingResponse;
import com.neo4flix.ratingservice.rating.RatingDtos.MyRatingResponse;
import com.neo4flix.ratingservice.rating.RatingDtos.MovieRatingResponse;

@Repository
public class RatingRepository {

    private final Neo4jClient neo4jClient;

    public RatingRepository(Neo4jClient neo4jClient) {
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

    public boolean movieExists(String movieId) {
        return neo4jClient.query("""
                MATCH (m:Movie {id: $movieId})
                RETURN count(m) > 0 AS result
                """)
                .bind(movieId).to("movieId")
                .fetchAs(Boolean.class)
                .one()
                .orElse(false);
    }

    public boolean ratingExists(String userId, String movieId) {
        return neo4jClient.query("""
                MATCH (:User {id: $userId})-[r:RATED]->(:Movie {id: $movieId})
                RETURN count(r) > 0 AS result
                """)
                .bind(userId).to("userId")
                .bind(movieId).to("movieId")
                .fetchAs(Boolean.class)
                .one()
                .orElse(false);
    }

    public Optional<RatingResponse> create(
            String userId, String movieId, Integer rating) {

        return neo4jClient.query("""
                MATCH (u:User {id: $userId}), (m:Movie {id: $movieId})
                MERGE (u)-[r:RATED]->(m)
                ON CREATE SET r.createdAt = datetime()
                SET r.rating = $rating,
                    r.updatedAt = datetime()
                RETURN u.id AS userId,
                       m.id AS movieId,
                       r.rating AS rating,
                       r.createdAt AS createdAt,
                       r.updatedAt AS updatedAt
                """)
                .bind(userId).to("userId")
                .bind(movieId).to("movieId")
                .bind(rating).to("rating")
                .fetchAs(RatingResponse.class)
                .mappedBy((types, record) -> toRatingResponse(record))
                .one();
    }

    public Optional<RatingResponse> update(
            String userId, String movieId, Integer rating) {

        return neo4jClient.query("""
                MATCH (u:User {id: $userId})-[r:RATED]->(m:Movie {id: $movieId})
                SET r.rating = $rating,
                    r.updatedAt = datetime()
                RETURN u.id AS userId,
                       m.id AS movieId,
                       r.rating AS rating,
                       r.createdAt AS createdAt,
                       r.updatedAt AS updatedAt
                """)
                .bind(userId).to("userId")
                .bind(movieId).to("movieId")
                .bind(rating).to("rating")
                .fetchAs(RatingResponse.class)
                .mappedBy((types, record) -> toRatingResponse(record))
                .one();
    }

    public Optional<RatingResponse> delete(String userId, String movieId) {
        return neo4jClient.query("""
                MATCH (u:User {id: $userId})-[r:RATED]->(m:Movie {id: $movieId})
                WITH u, r, m,
                     r.rating AS rating,
                     r.createdAt AS createdAt,
                     r.updatedAt AS updatedAt
                DELETE r
                RETURN u.id AS userId,
                       m.id AS movieId,
                       rating,
                       createdAt,
                       updatedAt
                """)
                .bind(userId).to("userId")
                .bind(movieId).to("movieId")
                .fetchAs(RatingResponse.class)
                .mappedBy((types, record) -> toRatingResponse(record))
                .one();
    }

    public Collection<MyRatingResponse> findByUser(String userId) {
        return neo4jClient.query("""
                MATCH (u:User {id: $userId})-[r:RATED]->(m:Movie)
                CALL {
                    WITH m
                    OPTIONAL MATCH (:User)-[other:RATED]->(m)
                    RETURN avg(other.rating) AS averageRating,
                           count(other) AS ratingCount
                }
                RETURN m.id AS movieId,
                       m.title AS movieTitle,
                       r.rating AS rating,
                       averageRating,
                       ratingCount,
                       r.createdAt AS createdAt,
                       coalesce(r.updatedAt, r.createdAt) AS updatedAt
                ORDER BY updatedAt DESC
                """)
                .bind(userId).to("userId")
                .fetchAs(MyRatingResponse.class)
                .mappedBy((types, record) -> toMyRatingResponse(record))
                .all();
    }

    public Collection<MovieRatingResponse> findByMovie(String movieId) {
        return neo4jClient.query("""
                MATCH (u:User)-[r:RATED]->(m:Movie {id: $movieId})
                RETURN u.id AS userId,
                       u.displayName AS displayName,
                       r.rating AS rating,
                       r.createdAt AS createdAt,
                       coalesce(r.updatedAt, r.createdAt) AS updatedAt
                ORDER BY updatedAt DESC
                """)
                .bind(movieId).to("movieId")
                .fetchAs(MovieRatingResponse.class)
                .mappedBy((types, record) -> toMovieRatingResponse(record))
                .all();
    }

    private RatingResponse toRatingResponse(Record record) {
        return new RatingResponse(
                record.get("movieId").asString(),
                record.get("userId").asString(),
                record.get("rating").asInt(),
                toDateTime(record.get("createdAt")),
                toDateTime(record.get("updatedAt"))
        );
    }

    private MyRatingResponse toMyRatingResponse(Record record) {
        return new MyRatingResponse(
                record.get("movieId").asString(),
                record.get("movieTitle").asString(),
                record.get("rating").asInt(),
                record.get("averageRating").isNull()
                        ? null : record.get("averageRating").asDouble(),
                record.get("ratingCount").asLong(),
                toDateTime(record.get("createdAt")),
                toDateTime(record.get("updatedAt"))
        );
    }

    private MovieRatingResponse toMovieRatingResponse(Record record) {
        return new MovieRatingResponse(
                record.get("userId").asString(),
                record.get("displayName").asString(),
                record.get("rating").asInt(),
                toDateTime(record.get("createdAt")),
                toDateTime(record.get("updatedAt"))
        );
    }

    private java.time.ZonedDateTime toDateTime(Value value) {
        return value.isNull() ? null : value.asZonedDateTime();
    }
}

package com.neo4flix.userservice.user;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import com.neo4flix.userservice.user.UserDtos.*;

@Repository
@RequiredArgsConstructor
public class RatingHistoryRepository {

	static final String QUERY = """
			MATCH (:User {id: $userId})-[r:RATED]->(m:Movie)
			OPTIONAL MATCH (m)-[:HAS_GENRE]->(g:Genre)
			WITH r, m, collect(DISTINCT g.name) AS genres
			OPTIONAL MATCH (:User)-[other:RATED]->(m)
			WITH r, m, genres, avg(other.rating) AS averageRating, count(other) AS ratingCount
			RETURN m.id AS id, m.title AS title, m.releaseYear AS releaseYear, genres,
			       averageRating, ratingCount, r.rating AS score,
			       coalesce(r.updatedAt, r.createdAt) AS updatedAt
			ORDER BY updatedAt DESC
			""";

	private final Neo4jClient neo4jClient;

	public List<MyRatingResponse> findByUser(String userId) {
		return List.copyOf(neo4jClient.query(QUERY)
				.bind(userId).to("userId")
				.fetchAs(MyRatingResponse.class)
				.mappedBy((types, record) -> toResponse(record))
				.all());
	}

	static MyRatingResponse toResponse(Record record) {
		MovieSummary movie = new MovieSummary(
				record.get("id").asString(),
				record.get("title").asString(null),
				record.get("releaseYear").isNull() ? null : record.get("releaseYear").asInt(),
				record.get("genres").asList(Value::asString),
				record.get("averageRating").isNull() ? null : record.get("averageRating").asDouble(),
				record.get("ratingCount").asLong());
		Value updatedAt = record.get("updatedAt");
		return new MyRatingResponse(movie, record.get("score").asInt(),
				updatedAt.isNull() ? null : updatedAt.asZonedDateTime());
	}
}

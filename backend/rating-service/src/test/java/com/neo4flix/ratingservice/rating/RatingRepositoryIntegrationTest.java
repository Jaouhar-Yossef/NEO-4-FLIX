
package com.neo4flix.ratingservice.rating;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.neo4flix.ratingservice.rating.RatingDtos.MyRatingResponse;
import com.neo4flix.ratingservice.rating.RatingDtos.MovieRatingResponse;
import com.neo4flix.ratingservice.rating.RatingDtos.RatingResponse;

@SpringBootTest
@ActiveProfiles("dev")
class RatingRepositoryIntegrationTest {

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private Driver driver;

    private String prefix;
    private String user1;
    private String user2;
    private String movieId;

    @BeforeEach
    void setUp() {
        prefix = "rating-it-" + UUID.randomUUID();
        user1 = prefix + "-user-1";
        user2 = prefix + "-user-2";
        movieId = prefix + "-movie-1";

        try (Session session = driver.session()) {
            session.run("""
                    CREATE (:User {
                        id: $user1,
                        displayName: 'Integration User One'
                    })
                    """, Map.of("user1", user1));

            session.run("""
                    CREATE (:User {
                        id: $user2,
                        displayName: 'Integration User Two'
                    })
                    """, Map.of("user2", user2));

            session.run("""
                    CREATE (:Movie {
                        id: $movieId,
                        title: 'Integration Test Movie',
                        releaseYear: 2026
                    })
                    """, Map.of("movieId", movieId));
        }
    }

    @AfterEach
    void cleanUp() {
        if (prefix == null) {
            return;
        }

        try (Session session = driver.session()) {
            session.run("""
                    MATCH (n)
                    WHERE n.id STARTS WITH $prefix
                    DETACH DELETE n
                    """, Map.of("prefix", prefix));
        }
    }

    @Test
    void existenceQueriesReturnCorrectResults() {
        assertTrue(ratingRepository.userExists(user1));
        assertTrue(ratingRepository.userExists(user2));
        assertTrue(ratingRepository.movieExists(movieId));

        assertFalse(ratingRepository.userExists(prefix + "-missing-user"));
        assertFalse(ratingRepository.movieExists(prefix + "-missing-movie"));

        assertFalse(ratingRepository.ratingExists(user1, movieId));
    }

    @Test
    void createRatingPersistsRelationshipAndTimestamps() {
        Optional<RatingResponse> result =
                ratingRepository.create(user1, movieId, 5);

        assertTrue(result.isPresent());

        RatingResponse response = result.get();

        assertEquals(user1, response.userId());
        assertEquals(movieId, response.movieId());
        assertEquals(5, response.rating());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());

        assertTrue(ratingRepository.ratingExists(user1, movieId));
    }

    @Test
    void findByUserReturnsRatingsWithMovieAverageAndCount() {
        ratingRepository.create(user1, movieId, 5);
        ratingRepository.create(user2, movieId, 4);

        Collection<MyRatingResponse> results =
                ratingRepository.findByUser(user1);

        assertEquals(1, results.size());

        MyRatingResponse response = results.iterator().next();

        assertEquals(movieId, response.movieId());
        assertEquals("Integration Test Movie", response.movieTitle());
        assertEquals(5, response.rating());
        assertEquals(4.5, response.movieAverageRating(), 0.001);
        assertEquals(2L, response.movieRatingCount());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
    }

    @Test
    void findByMovieReturnsRatingsFromAllUsers() {
        ratingRepository.create(user1, movieId, 5);
        ratingRepository.create(user2, movieId, 3);

        Collection<MovieRatingResponse> results =
                ratingRepository.findByMovie(movieId);

        assertEquals(2, results.size());

        assertTrue(results.stream().anyMatch(r ->
                r.userId().equals(user1)
                        && r.displayName().equals("Integration User One")
                        && r.rating() == 5));

        assertTrue(results.stream().anyMatch(r ->
                r.userId().equals(user2)
                        && r.displayName().equals("Integration User Two")
                        && r.rating() == 3));
    }

    @Test
    void updateAndDeleteOnlyAffectTheMatchingUsersRating() {
        ratingRepository.create(user1, movieId, 2);

        // Another user cannot update the first user's rating.
        Optional<RatingResponse> unauthorizedUpdate =
                ratingRepository.update(user2, movieId, 4);

        assertTrue(unauthorizedUpdate.isEmpty());

        // Another user cannot delete the first user's rating.
        Optional<RatingResponse> unauthorizedDelete =
                ratingRepository.delete(user2, movieId);

        assertTrue(unauthorizedDelete.isEmpty());
        assertTrue(ratingRepository.ratingExists(user1, movieId));

        // The owner can update their rating.
        Optional<RatingResponse> updated =
                ratingRepository.update(user1, movieId, 5);

        assertTrue(updated.isPresent());
        assertEquals(5, updated.get().rating());
        assertNotNull(updated.get().updatedAt());

        // The owner can delete their rating.
        Optional<RatingResponse> deleted =
                ratingRepository.delete(user1, movieId);

        assertTrue(deleted.isPresent());
        assertEquals(5, deleted.get().rating());
        assertFalse(ratingRepository.ratingExists(user1, movieId));
    }
}

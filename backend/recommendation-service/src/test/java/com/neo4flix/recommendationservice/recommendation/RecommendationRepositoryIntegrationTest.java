
package com.neo4flix.recommendationservice.recommendation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.neo4flix.recommendationservice.recommendation.RecommendationDtos.RecommendationResponse;

@SpringBootTest
@ActiveProfiles("dev")
class RecommendationRepositoryIntegrationTest {

        @Autowired
        private RecommendationRepository recommendationRepository;

        @Autowired
        private Driver driver;

        private String testPrefix;

        @BeforeEach
        void setUp() {
                testPrefix = "recommendation-it-" + UUID.randomUUID() + "-";
        }

        @AfterEach
        void cleanUp() {
                try (Session session = driver.session()) {
                        session.run(
                                        "MATCH (n) WHERE n.id STARTS WITH $prefix DETACH DELETE n",
                                        Values.parameters("prefix", testPrefix))
                                        .consume();
                }
        }

        @Test
        void userExistsAndHasRatingsShouldReturnCorrectResults() {
                String userId = testPrefix + "user";
                String movieId = testPrefix + "movie";

                createUser(userId);
                createMovie(movieId, "Test Movie", 2024, List.of("Drama"));

                assertTrue(recommendationRepository.userExists(userId));
                assertFalse(recommendationRepository.hasRatings(userId));

                createRating(userId, movieId, 5);

                assertTrue(recommendationRepository.hasRatings(userId));
                assertFalse(recommendationRepository.userExists(
                                testPrefix + "missing-user"));
        }

        @Test
        void personalizedRecommendationsShouldExcludeRatedMoviesAndApplyFilters() {
                String currentUser = testPrefix + "current-user";
                String similarUser = testPrefix + "similar-user";

                String sharedMovie = testPrefix + "shared-movie";
                String candidateMovie = testPrefix + "candidate-movie";
                String alreadyRatedMovie = testPrefix + "already-rated-movie";
                String lowRatedMovie = testPrefix + "low-rated-movie";

                createUser(currentUser);
                createUser(similarUser);

                createMovie(
                                sharedMovie,
                                "Shared Movie",
                                2020,
                                List.of("Drama"));

                createMovie(
                                candidateMovie,
                                "Recommended Action Movie",
                                2024,
                                List.of("Action"));

                createMovie(
                                alreadyRatedMovie,
                                "Already Rated Movie",
                                2024,
                                List.of("Action"));

                createMovie(
                                lowRatedMovie,
                                "Low Rated Movie",
                                2024,
                                List.of("Action"));

                // Both users gave the shared movie the same rating.
                createRating(currentUser, sharedMovie, 5);
                createRating(similarUser, sharedMovie, 5);

                // The similar user recommends the candidate.
                createRating(similarUser, candidateMovie, 5);

                // The current user already rated this movie, so it must be excluded.
                createRating(similarUser, alreadyRatedMovie, 5);
                createRating(currentUser, alreadyRatedMovie, 4);

                // Ratings below 4 must not produce recommendations.
                createRating(similarUser, lowRatedMovie, 3);

                List<RecommendationResponse> results = recommendationRepository.findPersonalized(
                                currentUser,
                                "action",
                                2024,
                                0,
                                10);

                assertEquals(1, results.size());

                RecommendationResponse recommendation = results.get(0);

                assertEquals(candidateMovie, recommendation.id());
                assertEquals("Recommended Action Movie", recommendation.title());
                assertEquals(2024, recommendation.releaseYear());
                assertTrue(recommendation.genres().contains("Action"));

                assertEquals(5.0, recommendation.averageRating(), 0.001);
                assertEquals(1L, recommendation.ratingCount());
                assertEquals(1L, recommendation.similarUsersCount());
                assertTrue(recommendation.relevanceScore() > 0.0);

                long total = recommendationRepository.countPersonalized(
                                currentUser,
                                "action",
                                2024);

                assertEquals(1L, total);
        }

        @Test
        void personalizedRecommendationsShouldSupportPagination() {
                String currentUser = testPrefix + "current-user";
                String similarUser = testPrefix + "similar-user";

                String sharedMovie = testPrefix + "shared";
                String candidateA = testPrefix + "candidate-a";
                String candidateB = testPrefix + "candidate-b";

                createUser(currentUser);
                createUser(similarUser);

                createMovie(sharedMovie, "Shared Movie", 2024, List.of("Drama"));
                createMovie(candidateA, "Candidate A", 2024, List.of("Action"));
                createMovie(candidateB, "Candidate B", 2024, List.of("Action"));

                createRating(currentUser, sharedMovie, 5);
                createRating(similarUser, sharedMovie, 5);

                createRating(similarUser, candidateA, 5);
                createRating(similarUser, candidateB, 5);

                long total = recommendationRepository.countPersonalized(
                                currentUser,
                                "",
                                -1);

                List<RecommendationResponse> firstPage = recommendationRepository.findPersonalized(
                                currentUser, "", -1, 0, 1);

                List<RecommendationResponse> secondPage = recommendationRepository.findPersonalized(
                                currentUser, "", -1, 1, 1);

                assertEquals(2L, total);
                assertEquals(1, firstPage.size());
                assertEquals(1, secondPage.size());

                assertNotEquals(
                                firstPage.get(0).id(),
                                secondPage.get(0).id());

                Set<String> returnedIds = Set.of(
                                firstPage.get(0).id(),
                                secondPage.get(0).id());

                assertEquals(Set.of(candidateA, candidateB), returnedIds);
        }

        @Test
        void popularRecommendationsShouldReturnRatingsAndApplyFilters() {
                String userOne = testPrefix + "user-one";
                String userTwo = testPrefix + "user-two";

                String actionMovie = testPrefix + "action-movie";
                String comedyMovie = testPrefix + "comedy-movie";

                createUser(userOne);
                createUser(userTwo);

                createMovie(
                                actionMovie,
                                "Popular Action Movie",
                                2024,
                                List.of("Action", "Adventure"));

                createMovie(
                                comedyMovie,
                                "Comedy Movie",
                                2023,
                                List.of("Comedy"));

                createRating(userOne, actionMovie, 5);
                createRating(userTwo, actionMovie, 3);

                createRating(userOne, comedyMovie, 5);

                List<RecommendationResponse> results = recommendationRepository.findPopular(
                                "action",
                                2024,
                                0,
                                10);

                assertEquals(1, results.size());

                RecommendationResponse recommendation = results.get(0);

                assertEquals(actionMovie, recommendation.id());
                assertEquals("Popular Action Movie", recommendation.title());
                assertEquals(2024, recommendation.releaseYear());

                assertEquals(4.0, recommendation.averageRating(), 0.001);
                assertEquals(2L, recommendation.ratingCount());

                assertTrue(recommendation.genres().contains("Action"));
                assertTrue(recommendation.genres().contains("Adventure"));

                assertEquals(0L, recommendation.similarUsersCount());

                assertEquals(
                                1L,
                                recommendationRepository.countPopular("action", 2024));
        }

        @Test
        void popularRecommendationsShouldSupportPaginationAndCounting() {
                String uniqueGenre = testPrefix + "PaginationGenre";

                createMovie(
                                testPrefix + "movie-a",
                                "A Movie",
                                2024,
                                List.of(uniqueGenre));

                createMovie(
                                testPrefix + "movie-b",
                                "B Movie",
                                2024,
                                List.of(uniqueGenre));

                createMovie(
                                testPrefix + "movie-c",
                                "C Movie",
                                2024,
                                List.of(uniqueGenre));

                long total = recommendationRepository.countPopular(
                                uniqueGenre, -1);

                List<RecommendationResponse> firstPage = recommendationRepository.findPopular(
                                uniqueGenre, -1, 0, 2);

                List<RecommendationResponse> secondPage = recommendationRepository.findPopular(
                                uniqueGenre, -1, 2, 2);

                assertEquals(3L, total);
                assertEquals(2, firstPage.size());
                assertEquals(1, secondPage.size());

                assertEquals("A Movie", firstPage.get(0).title());
                assertEquals("B Movie", firstPage.get(1).title());
                assertEquals("C Movie", secondPage.get(0).title());

                assertEquals(0L, firstPage.get(0).similarUsersCount());
        }

        private void createUser(String userId) {
                try (Session session = driver.session()) {
                        session.run("""
                                        CREATE (:User {
                                            id: $userId,
                                            email: $email
                                        })
                                        """,
                                        Values.parameters(
                                                        "userId", userId,
                                                        "email", userId + "@integration.test"))
                                        .consume();
                }
        }

        private void createMovie(
                        String movieId,
                        String title,
                        int releaseYear,
                        List<String> genreNames) {

                try (Session session = driver.session()) {
                        session.run("""
                                        CREATE (m:Movie {
                                            id: $movieId,
                                            title: $title,
                                            releaseYear: $releaseYear
                                        })
                                        """,
                                        Values.parameters(
                                                        "movieId", movieId,
                                                        "title", title,
                                                        "releaseYear", releaseYear))
                                        .consume();

                        for (int i = 0; i < genreNames.size(); i++) {
                                String genreId = movieId + "-genre-" + i;

                                session.run("""
                                                MATCH (m:Movie {id: $movieId})
                                                CREATE (g:Genre {
                                                    id: $genreId,
                                                    name: $genreName
                                                })
                                                CREATE (m)-[:HAS_GENRE]->(g)
                                                """,
                                                Values.parameters(
                                                                "movieId", movieId,
                                                                "genreId", genreId,
                                                                "genreName", genreNames.get(i)))
                                                .consume();
                        }
                }
        }

        private void createRating(
                        String userId,
                        String movieId,
                        int rating) {

                try (Session session = driver.session()) {
                        session.run("""
                                        MATCH (u:User {id: $userId})
                                        MATCH (m:Movie {id: $movieId})
                                        CREATE (u)-[:RATED {
                                            rating: $rating
                                        }]->(m)
                                        """,
                                        Values.parameters(
                                                        "userId", userId,
                                                        "movieId", movieId,
                                                        "rating", rating))
                                        .consume();
                }
        }
}

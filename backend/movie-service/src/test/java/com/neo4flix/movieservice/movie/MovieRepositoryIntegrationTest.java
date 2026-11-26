package com.neo4flix.movieservice.movie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

import com.neo4flix.movieservice.movie.dto.MovieDetailsRecord;
import com.neo4flix.movieservice.movie.dto.MovieSearchRecord;

@SpringBootTest
@ActiveProfiles("dev")
class MovieRepositoryIntegrationTest {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private Driver driver;

    private String testPrefix;

    @BeforeEach
    void setUp() {
        testPrefix = "it-" + UUID.randomUUID() + "-";
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
    void searchMoviesShouldReturnGenresAndRatingInformation() {
        String movieId = testPrefix + "movie-1";
        String title = testPrefix + "The Integration Movie";

        createMovie(
                movieId,
                title,
                2024,
                List.of("Integration Action", "Integration Drama"),
                8);

        List<MovieSearchRecord> results = movieRepository.searchMovies(
                title,
                null,
                null,
                "title",
                "ASC",
                0,
                10);

        assertEquals(2, results.size());

        assertEquals(movieId, results.get(0).id());
        assertEquals(title, results.get(0).title());

        assertEquals("Integration Action", results.get(0).genre());
        assertEquals("Integration Drama", results.get(1).genre());

        assertEquals(8.0, results.get(0).averageRating(), 0.001);
        assertEquals(1L, results.get(0).ratingCount());
    }

    @Test
    void searchMoviesShouldFilterByTitleGenreAndYear() {
        String matchingId = testPrefix + "matching";
        String otherId = testPrefix + "other";

        String title = testPrefix + "Filtered Movie";

        createMovie(
                matchingId,
                title,
                2024,
                List.of("Unique Integration Genre"),
                9);

        createMovie(
                otherId,
                title + " Other",
                2023,
                List.of("Different Integration Genre"),
                7);

        List<MovieSearchRecord> results = movieRepository.searchMovies(
                title,
                "Unique Integration Genre",
                2024,
                "title",
                "ASC",
                0,
                10);

        assertEquals(1, results.size());
        assertEquals(matchingId, results.get(0).id());

        long count = movieRepository.countMovies(
                title,
                "Unique Integration Genre",
                2024);

        assertEquals(1L, count);
    }

    @Test
    void searchMoviesShouldSupportPaginationAndSorting() {
        String titleA = testPrefix + "A Movie";
        String titleB = testPrefix + "B Movie";
        String titleC = testPrefix + "C Movie";

        createMovie(
                testPrefix + "movie-a",
                titleA,
                2024,
                List.of("Genre A"),
                8);

        createMovie(
                testPrefix + "movie-b",
                titleB,
                2024,
                List.of("Genre B"),
                7);

        createMovie(
                testPrefix + "movie-c",
                titleC,
                2024,
                List.of("Genre C"),
                9);

        List<MovieSearchRecord> firstPage = movieRepository.searchMovies(
                testPrefix,
                null,
                null,
                "title",
                "ASC",
                0,
                2);

        List<MovieSearchRecord> secondPage = movieRepository.searchMovies(
                testPrefix,
                null,
                null,
                "title",
                "ASC",
                2,
                2);

        assertEquals(2, firstPage.size());
        assertEquals(1, secondPage.size());

        assertEquals(titleA, firstPage.get(0).title());
        assertEquals(titleB, firstPage.get(1).title());
        assertEquals(titleC, secondPage.get(0).title());

        assertEquals(
                3L,
                movieRepository.countMovies(testPrefix, null, null));
    }

    @Test
    void findMovieDetailsShouldReturnRatingAndWatchlistInformation() {
        String movieId = testPrefix + "details-movie";
        String userId = movieId + "-user";

        createMovie(
                movieId,
                testPrefix + "Details Movie",
                2025,
                List.of("Details Genre"),
                9);

        try (Session session = driver.session()) {
            session.run("""
                    MATCH (u:User {id: $userId})
                    MATCH (m:Movie {id: $movieId})
                    CREATE (u)-[:WATCHLIST]->(m)
                    """,
                    Values.parameters(
                            "userId", userId,
                            "movieId", movieId))
                    .consume();
        }

        List<MovieDetailsRecord> results =
                movieRepository.findMovieDetails(movieId, userId);

        assertFalse(results.isEmpty());

        MovieDetailsRecord details = results.get(0);

        assertEquals(movieId, details.id());
        assertEquals(testPrefix + "Details Movie", details.title());
        assertEquals("Details Genre", details.genre());

        assertEquals(9.0, details.averageRating(), 0.001);
        assertEquals(1L, details.ratingCount());

        assertEquals(9, details.myRating());
        assertEquals(Boolean.TRUE, details.inWatchlist());
    }

    @Test
    void findMovieDetailsShouldReturnEmptyListForUnknownMovie() {
        List<MovieDetailsRecord> results =
                movieRepository.findMovieDetails(
                        testPrefix + "missing-movie",
                        testPrefix + "missing-user");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void deleteWithRelationshipsShouldDeleteMovieAndItsRelationships() {
        String movieId = testPrefix + "delete-movie";
        String userId = movieId + "-user";

        createMovie(
                movieId,
                testPrefix + "Movie To Delete",
                2024,
                List.of("Delete Genre"),
                8);

        try (Session session = driver.session()) {
            session.run("""
                    MATCH (u:User {id: $userId})
                    MATCH (m:Movie {id: $movieId})
                    CREATE (u)-[:WATCHLIST]->(m)
                    """,
                    Values.parameters(
                            "userId", userId,
                            "movieId", movieId))
                    .consume();
        }

        movieRepository.deleteWithRelationships(movieId);

        try (Session session = driver.session()) {
            long movieCount = session.run("""
                    MATCH (m:Movie {id: $movieId})
                    RETURN count(m) AS count
                    """,
                    Values.parameters("movieId", movieId))
                    .single()
                    .get("count")
                    .asLong();

            long remainingRelationships = session.run("""
                    MATCH (n)-[r]->(m:Movie {id: $movieId})
                    RETURN count(r) AS count
                    """,
                    Values.parameters("movieId", movieId))
                    .single()
                    .get("count")
                    .asLong();

            assertEquals(0L, movieCount);
            assertEquals(0L, remainingRelationships);
        }
    }

    private void createMovie(
            String movieId,
            String title,
            int releaseYear,
            List<String> genreNames,
            int rating) {

        List<Map<String, Object>> genres = new ArrayList<>();

        for (int i = 0; i < genreNames.size(); i++) {
            Map<String, Object> genre = new HashMap<>();
            genre.put("id", movieId + "-genre-" + i);
            genre.put("name", genreNames.get(i));
            genres.add(genre);
        }

        String userId = movieId + "-user";

        try (Session session = driver.session()) {
            session.run("""
                    CREATE (m:Movie {
                        id: $movieId,
                        title: $title,
                        releaseYear: $releaseYear
                    })
                    WITH m
                    UNWIND $genres AS genreData
                    CREATE (g:Genre {
                        id: genreData.id,
                        name: genreData.name
                    })
                    CREATE (m)-[:HAS_GENRE]->(g)
                    WITH DISTINCT m
                    CREATE (u:User {
                        id: $userId,
                        email: $email
                    })
                    CREATE (u)-[:RATED {rating: $rating}]->(m)
                    """,
                    Values.parameters(
                            "movieId", movieId,
                            "title", title,
                            "releaseYear", releaseYear,
                            "genres", genres,
                            "userId", userId,
                            "email", userId + "@integration.test",
                            "rating", rating))
                    .consume();
        }
    }
}

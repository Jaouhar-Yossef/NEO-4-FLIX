package com.neo4flix.movieservice.movie;

import com.neo4flix.movieservice.common.exception.MovieNotFoundException;
import com.neo4flix.movieservice.genre.Genre;
import com.neo4flix.movieservice.genre.GenreRepository;
import com.neo4flix.movieservice.movie.dto.MovieDetailsRecord;
import com.neo4flix.movieservice.movie.dto.MovieDetailsResponse;
import com.neo4flix.movieservice.movie.dto.MoviePageResponse;
import com.neo4flix.movieservice.movie.dto.MovieRequest;
import com.neo4flix.movieservice.movie.dto.MovieSearchRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MovieServiceTest {

    private MovieRepository movieRepository;
    private GenreRepository genreRepository;
    private MovieService movieService;

    @BeforeEach
    void setUp() {
        movieRepository = mock(MovieRepository.class);
        genreRepository = mock(GenreRepository.class);
        movieService = new MovieService(movieRepository, genreRepository);
    }

    private MovieRequest validRequest(String title, int year, String... genres) {
        MovieRequest request = new MovieRequest();
        request.setTitle(title);
        request.setReleaseYear(year);
        request.setGenres(List.of(genres));
        return request;
    }

    @Test
    void createMovieShouldReuseExistingGenres() {
        Genre action = new Genre("genre-1", "Action");
        MovieRequest request = validRequest("  The Matrix  ", 1999, " Action ");

        when(genreRepository.findByName("Action")).thenReturn(Optional.of(action));
        when(movieRepository.save(any(Movie.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Movie result = movieService.createMovie(request);

        assertNotNull(result.getId());
        assertTrue(result.getId().startsWith("movie-"));
        assertEquals("The Matrix", result.getTitle());
        assertEquals(1999, result.getReleaseYear());
        assertEquals(Set.of(action), result.getGenres());

        verify(genreRepository).findByName("Action");
        verify(genreRepository, never()).save(any(Genre.class));
        verify(movieRepository).save(any(Movie.class));
    }

    @Test
    void createMovieShouldSaveNewGenres() {
        Genre drama = new Genre("genre-new", "Drama");
        MovieRequest request = validRequest("Inception", 2010, "Drama");

        when(genreRepository.findByName("Drama")).thenReturn(Optional.empty());
        when(genreRepository.save(any(Genre.class))).thenReturn(drama);
        when(movieRepository.save(any(Movie.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Movie result = movieService.createMovie(request);

        assertEquals("Inception", result.getTitle());
        assertTrue(result.getGenres().contains(drama));

        verify(genreRepository).save(any(Genre.class));
        verify(movieRepository).save(any(Movie.class));
    }

    @Test
    void updateMovieShouldUpdateExistingMovie() {
        Movie movie = new Movie();
        movie.setId("movie-001");
        movie.setTitle("Old title");
        movie.setReleaseYear(2000);

        Genre action = new Genre("genre-1", "Action");
        MovieRequest request = validRequest("New title", 2020, "Action");

        when(movieRepository.findById("movie-001")).thenReturn(Optional.of(movie));
        when(genreRepository.findByName("Action")).thenReturn(Optional.of(action));
        when(movieRepository.save(movie)).thenReturn(movie);

        Movie result = movieService.updateMovie("movie-001", request);

        assertEquals("New title", result.getTitle());
        assertEquals(2020, result.getReleaseYear());
        assertEquals(Set.of(action), result.getGenres());

        verify(movieRepository).findById("movie-001");
        verify(movieRepository).save(movie);
    }

    @Test
    void updateMovieShouldThrowWhenMovieDoesNotExist() {
        MovieRequest request = validRequest("Updated title", 2020, "Action");

        when(movieRepository.findById("missing"))
                .thenReturn(Optional.empty());

        MovieNotFoundException exception = assertThrows(
                MovieNotFoundException.class,
                () -> movieService.updateMovie("missing", request));

        assertEquals("Movie not found with id: missing", exception.getMessage());

        verify(movieRepository, never()).save(any(Movie.class));
        verifyNoInteractions(genreRepository);
    }

    @Test
    void searchMoviesShouldGroupGenresForSameMovie() {
        List<MovieSearchRecord> records = List.of(
                new MovieSearchRecord("movie-001", "The Matrix", 1999,
                        "Action", 4.8, 100L),
                new MovieSearchRecord("movie-001", "The Matrix", 1999,
                        "Sci-Fi", 4.8, 100L),
                new MovieSearchRecord("movie-002", "Inception", 2010,
                        "Sci-Fi", 4.6, 80L)
        );

        when(movieRepository.searchMovies(
                "Matrix", "Action", 1999, "title", "ASC", 0, 10))
                .thenReturn(records);
        when(movieRepository.countMovies("Matrix", "Action", 1999))
                .thenReturn(1L);

        MoviePageResponse result = movieService.searchMovies(
                " Matrix ", " Action ", 1999, 0, 10, "title", "ASC");

        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(1L, result.getTotalElements());

        assertEquals(2, result.getContent().size());

        assertEquals("movie-001", result.getContent().get(0).getId());
        assertEquals(
                List.of("Action", "Sci-Fi"),
                result.getContent().get(0).getGenres());

        verify(movieRepository).searchMovies(
                "Matrix", "Action", 1999, "title", "ASC", 0, 10);
        verify(movieRepository).countMovies("Matrix", "Action", 1999);
    }

    @Test
    void searchMoviesShouldRejectNegativePage() {
        assertThrows(
                IllegalArgumentException.class,
                () -> movieService.searchMovies(
                        null, null, null, -1, 20, "title", "ASC"));

        verifyNoInteractions(movieRepository);
    }

    @Test
    void searchMoviesShouldRejectInvalidPageSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> movieService.searchMovies(
                        null, null, null, 0, 0, "title", "ASC"));

        assertThrows(
                IllegalArgumentException.class,
                () -> movieService.searchMovies(
                        null, null, null, 0, 101, "title", "ASC"));

        verifyNoInteractions(movieRepository);
    }

    @Test
    void searchMoviesShouldRejectInvalidYear() {
        assertThrows(
                IllegalArgumentException.class,
                () -> movieService.searchMovies(
                        null, null, 1887, 0, 20, "title", "ASC"));

        assertThrows(
                IllegalArgumentException.class,
                () -> movieService.searchMovies(
                        null, null, 2101, 0, 20, "title", "ASC"));

        verifyNoInteractions(movieRepository);
    }

    @Test
    void searchMoviesShouldRejectInvalidSortField() {
        assertThrows(
                IllegalArgumentException.class,
                () -> movieService.searchMovies(
                        null, null, null, 0, 20, "rating", "ASC"));

        verifyNoInteractions(movieRepository);
    }

    @Test
    void searchMoviesShouldRejectInvalidSortDirection() {
        assertThrows(
                IllegalArgumentException.class,
                () -> movieService.searchMovies(
                        null, null, null, 0, 20, "title", "SIDEWAYS"));

        verifyNoInteractions(movieRepository);
    }

    @Test
    void deleteMovieShouldDeleteMovieWithRelationships() {
        when(movieRepository.existsById("movie-001")).thenReturn(true);

        movieService.deleteMovie("movie-001");

        verify(movieRepository).existsById("movie-001");
        verify(movieRepository).deleteWithRelationships("movie-001");
    }

    @Test
    void deleteMovieShouldThrowWhenMovieDoesNotExist() {
        when(movieRepository.existsById("missing")).thenReturn(false);

        assertThrows(
                MovieNotFoundException.class,
                () -> movieService.deleteMovie("missing"));

        verify(movieRepository, never()).deleteWithRelationships(anyString());
    }

    @Test
    void getMovieDetailsShouldBuildResponseWithDistinctGenres() {
        List<MovieDetailsRecord> records = List.of(
                new MovieDetailsRecord(
                        "movie-001", "The Matrix", 1999,
                        "Action", 4.8, 100L, 5, true),
                new MovieDetailsRecord(
                        "movie-001", "The Matrix", 1999,
                        "Sci-Fi", 4.8, 100L, 5, true),
                new MovieDetailsRecord(
                        "movie-001", "The Matrix", 1999,
                        "Action", 4.8, 100L, 5, true)
        );

        when(movieRepository.findMovieDetails("movie-001", "user-001"))
                .thenReturn(records);

        MovieDetailsResponse result =
                movieService.getMovieDetails("movie-001", "user-001");

        assertEquals("movie-001", result.getId());
        assertEquals("The Matrix", result.getTitle());
        assertEquals(1999, result.getReleaseYear());
        assertEquals(4.8, result.getAverageRating());
        assertEquals(100L, result.getRatingCount());
        assertEquals(5, result.getMyRating());
        assertTrue(result.isInWatchlist());
        assertEquals(2, result.getGenres().size());
        assertTrue(result.getGenres().containsAll(
                List.of("Action", "Sci-Fi")));

        verify(movieRepository).findMovieDetails("movie-001", "user-001");
    }

    @Test
    void getMovieDetailsShouldThrowWhenMovieDoesNotExist() {
        when(movieRepository.findMovieDetails("missing", null))
                .thenReturn(List.of());

        assertThrows(
                MovieNotFoundException.class,
                () -> movieService.getMovieDetails("missing", null));
    }
}

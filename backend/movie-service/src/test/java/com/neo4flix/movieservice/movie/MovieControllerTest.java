package com.neo4flix.movieservice.movie;

import com.neo4flix.movieservice.common.exception.MovieNotFoundException;
import com.neo4flix.movieservice.movie.dto.MovieDetailsResponse;
import com.neo4flix.movieservice.movie.dto.MoviePageResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovieController.class)
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @Test
    void searchMoviesShouldReturnPage() throws Exception {
        when(movieService.searchMovies(
                null, null, null, 0, 20, "title", "ASC"))
                .thenReturn(new MoviePageResponse(List.of(), 0, 20, 0));

        mockMvc.perform(get("/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(movieService).searchMovies(
                null, null, null, 0, 20, "title", "ASC");
    }

    @Test
    void searchMoviesShouldPassQueryParameters() throws Exception {
        when(movieService.searchMovies(
                "Matrix", "Action", 1999, 1, 5, "title", "DESC"))
                .thenReturn(new MoviePageResponse(List.of(), 1, 5, 0));

        mockMvc.perform(get("/movies/search")
                        .param("title", "Matrix")
                        .param("genre", "Action")
                        .param("year", "1999")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "title")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5));

        verify(movieService).searchMovies(
                "Matrix", "Action", 1999, 1, 5, "title", "DESC");
    }

    @Test
    void searchMoviesShouldReturnBadRequestForInvalidYearParameter()
            throws Exception {
        mockMvc.perform(get("/movies").param("year", "invalid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(movieService);
    }

    @Test
    void getMovieDetailsShouldPassUserHeader() throws Exception {
        when(movieService.getMovieDetails("movie-001", "user-001"))
                .thenReturn(new MovieDetailsResponse());

        mockMvc.perform(get("/movies/movie-001")
                        .header("X-User-Id", "user-001"))
                .andExpect(status().isOk());

        verify(movieService).getMovieDetails("movie-001", "user-001");
    }

    @Test
    void getMovieDetailsShouldAllowMissingUserHeader() throws Exception {
        when(movieService.getMovieDetails("movie-001", null))
                .thenReturn(new MovieDetailsResponse());

        mockMvc.perform(get("/movies/movie-001"))
                .andExpect(status().isOk());

        verify(movieService).getMovieDetails("movie-001", null);
    }

    @Test
    void getMovieDetailsShouldReturnNotFoundWhenMovieDoesNotExist()
            throws Exception {
        when(movieService.getMovieDetails("missing", null))
                .thenThrow(new MovieNotFoundException("missing"));

        mockMvc.perform(get("/movies/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Movie Not Found"))
                .andExpect(jsonPath("$.detail")
                        .value("Movie not found with id: missing"));
    }

    @Test
    void createMovieShouldReturnCreatedMovie() throws Exception {
        Movie movie = new Movie(
                "movie-001",
                "The Matrix",
                1999,
                Set.of()
        );

        when(movieService.createMovie(any())).thenReturn(movie);

        mockMvc.perform(post("/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "The Matrix",
                                  "releaseYear": 1999,
                                  "genres": ["Action", "Sci-Fi"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("movie-001"))
                .andExpect(jsonPath("$.title").value("The Matrix"))
                .andExpect(jsonPath("$.releaseYear").value(1999));

        verify(movieService).createMovie(any());
    }

    @Test
    void createMovieShouldRejectBlankTitle() throws Exception {
        mockMvc.perform(post("/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "releaseYear": 1999,
                                  "genres": ["Action"]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(movieService);
    }

    @Test
    void createMovieShouldRejectYearOutsideAllowedRange() throws Exception {
        mockMvc.perform(post("/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "The Matrix",
                                  "releaseYear": 1887,
                                  "genres": ["Action"]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(movieService);
    }

    @Test
    void createMovieShouldRejectEmptyGenres() throws Exception {
        mockMvc.perform(post("/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "The Matrix",
                                  "releaseYear": 1999,
                                  "genres": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(movieService);
    }

    @Test
    void createMovieShouldRejectBlankGenre() throws Exception {
        mockMvc.perform(post("/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "The Matrix",
                                  "releaseYear": 1999,
                                  "genres": [" "]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(movieService);
    }

    @Test
    void createMovieShouldRejectTitleLongerThan200Characters()
            throws Exception {
        String longTitle = "A".repeat(201);

        mockMvc.perform(post("/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "releaseYear": 1999,
                                  "genres": ["Action"]
                                }
                                """.formatted(longTitle)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(movieService);
    }

    @Test
    void updateMovieShouldReturnUpdatedMovie() throws Exception {
        Movie movie = new Movie(
                "movie-001",
                "Updated Matrix",
                2000,
                Set.of()
        );

        when(movieService.updateMovie(eq("movie-001"), any()))
                .thenReturn(movie);

        mockMvc.perform(put("/movies/movie-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Matrix",
                                  "releaseYear": 2000,
                                  "genres": ["Action"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("movie-001"))
                .andExpect(jsonPath("$.title").value("Updated Matrix"))
                .andExpect(jsonPath("$.releaseYear").value(2000));

        verify(movieService).updateMovie(eq("movie-001"), any());
    }

    @Test
    void updateMovieShouldRejectInvalidRequest() throws Exception {
        mockMvc.perform(put("/movies/movie-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "releaseYear": 2020,
                                  "genres": ["Action"]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(movieService);
    }

    @Test
    void updateMovieShouldReturnNotFoundWhenMovieDoesNotExist()
            throws Exception {
        when(movieService.updateMovie(eq("missing"), any()))
                .thenThrow(new MovieNotFoundException("missing"));

        mockMvc.perform(put("/movies/missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated",
                                  "releaseYear": 2020,
                                  "genres": ["Action"]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Movie Not Found"));
    }

    @Test
    void deleteMovieShouldReturnOk() throws Exception {
        doNothing().when(movieService).deleteMovie("movie-001");

        mockMvc.perform(delete("/movies/movie-001"))
                .andExpect(status().isOk());

        verify(movieService).deleteMovie("movie-001");
    }

    @Test
    void deleteMovieShouldReturnNotFoundWhenMovieDoesNotExist()
            throws Exception {
        doThrow(new MovieNotFoundException("missing"))
                .when(movieService).deleteMovie("missing");

        mockMvc.perform(delete("/movies/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Movie Not Found"));
    }
}

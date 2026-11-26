
package com.neo4flix.ratingservice.rating;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.neo4flix.ratingservice.exception.GlobalExceptionHandler;
import com.neo4flix.ratingservice.rating.RatingDtos.CreateRatingRequest;
import com.neo4flix.ratingservice.rating.RatingDtos.MyRatingResponse;
import com.neo4flix.ratingservice.rating.RatingDtos.MovieRatingResponse;
import com.neo4flix.ratingservice.rating.RatingDtos.RatingResponse;
import com.neo4flix.ratingservice.rating.RatingDtos.UpdateRatingRequest;

@WebMvcTest(RatingController.class)
@Import(GlobalExceptionHandler.class)
class RatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RatingService ratingService;

    private final RatingResponse ratingResponse =
            new RatingResponse("movie-1", "user-1", 5, null, null);

    @Test
    void createRatingReturns201() throws Exception {
        when(ratingService.create(eq("user-1"), any(CreateRatingRequest.class)))
                .thenReturn(ratingResponse);

        mockMvc.perform(post("/ratings")
                        .header("X-User-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "movieId": "movie-1",
                                  "rating": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movieId").value("movie-1"))
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void createRatingRejectsBlankMovieId() throws Exception {
        mockMvc.perform(post("/ratings")
                        .header("X-User-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "movieId": " ",
                                  "rating": 5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void createRatingRejectsRatingBelowOne() throws Exception {
        mockMvc.perform(post("/ratings")
                        .header("X-User-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "movieId": "movie-1",
                                  "rating": 0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRatingRejectsRatingAboveFive() throws Exception {
        mockMvc.perform(post("/ratings")
                        .header("X-User-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "movieId": "movie-1",
                                  "rating": 6
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRatingRejectsMissingRating() throws Exception {
        mockMvc.perform(post("/ratings")
                        .header("X-User-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "movieId": "movie-1"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRatingPreservesNotFoundStatus() throws Exception {
        when(ratingService.create(eq("user-1"), any(CreateRatingRequest.class)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Movie not found"));

        mockMvc.perform(post("/ratings")
                        .header("X-User-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "movieId": "missing-movie",
                                  "rating": 4
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Movie not found"));
    }

    @Test
    void createRatingPreservesConflictStatus() throws Exception {
        when(ratingService.create(eq("user-1"), any(CreateRatingRequest.class)))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT, "Rating already exists"));

        mockMvc.perform(post("/ratings")
                        .header("X-User-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "movieId": "movie-1",
                                  "rating": 4
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void findMineReturns200() throws Exception {
        when(ratingService.findMine("user-1")).thenReturn(List.of(
                new MyRatingResponse(
                        "movie-1", "Movie One", 5, 4.5, 10L, null, null)));

        mockMvc.perform(get("/ratings/me")
                        .header("X-User-Id", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movieId").value("movie-1"))
                .andExpect(jsonPath("$[0].movieTitle").value("Movie One"))
                .andExpect(jsonPath("$[0].rating").value(5));
    }

    @Test
    void findByMovieReturns200() throws Exception {
        when(ratingService.findByMovie("movie-1")).thenReturn(List.of(
                new MovieRatingResponse(
                        "user-1", "User One", 4, null, null)));

        mockMvc.perform(get("/ratings/movie/movie-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("user-1"))
                .andExpect(jsonPath("$[0].displayName").value("User One"))
                .andExpect(jsonPath("$[0].rating").value(4));
    }

    @Test
    void updateRatingReturns200() throws Exception {
        when(ratingService.update(
                eq("user-1"), eq("movie-1"), any(UpdateRatingRequest.class)))
                .thenReturn(new RatingResponse(
                        "movie-1", "user-1", 3, null, null));

        mockMvc.perform(put("/ratings/movie-1")
                        .header("X-User-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(3))
                .andExpect(jsonPath("$.movieId").value("movie-1"));
    }

    @Test
    void updateRatingRejectsInvalidValue() throws Exception {
        mockMvc.perform(put("/ratings/movie-1")
                        .header("X-User-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 8
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteRatingReturns204() throws Exception {
        doNothing().when(ratingService).delete("user-1", "movie-1");

        mockMvc.perform(delete("/ratings/movie-1")
                        .header("X-User-Id", "user-1"))
                .andExpect(status().isNoContent());

        verify(ratingService).delete("user-1", "movie-1");
    }

    @Test
    void deleteRatingPreservesNotFoundStatus() throws Exception {
        doThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Rating not found"))
                .when(ratingService).delete("user-1", "movie-1");

        mockMvc.perform(delete("/ratings/movie-1")
                        .header("X-User-Id", "user-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}

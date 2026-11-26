
package com.neo4flix.recommendationservice.recommendation;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.neo4flix.recommendationservice.common.exception.GlobalExceptionHandler;
import com.neo4flix.recommendationservice.recommendation.RecommendationDtos.RecommendationPageResponse;
import com.neo4flix.recommendationservice.recommendation.RecommendationDtos.RecommendationResponse;

@WebMvcTest(RecommendationController.class)
@Import(GlobalExceptionHandler.class)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    private RecommendationPageResponse sampleResponse(boolean personalized) {
        RecommendationResponse movie = new RecommendationResponse(
                "movie-1",
                "Interstellar",
                2014,
                List.of("Sci-Fi", "Drama"),
                4.8,
                120L,
                0.92,
                3L
        );

        return new RecommendationPageResponse(
                List.of(movie),
                0,
                20,
                1L,
                1,
                personalized
        );
    }

    @Test
    void returnsPersonalizedRecommendations() throws Exception {
        when(recommendationService.getRecommendations(
                "user-1", null, null, 0, 20))
                .thenReturn(sampleResponse(true));

        mockMvc.perform(get("/recommendations")
                        .header("X-User-Id", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalized").value(true))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value("movie-1"))
                .andExpect(jsonPath("$.content[0].title").value("Interstellar"))
                .andExpect(jsonPath("$.content[0].averageRating").value(4.8));

        verify(recommendationService).getRecommendations(
                "user-1", null, null, 0, 20);
    }

    @Test
    void returnsPopularRecommendations() throws Exception {
        when(recommendationService.getRecommendations(
                "user-2", null, null, 0, 20))
                .thenReturn(sampleResponse(false));

        mockMvc.perform(get("/recommendations")
                        .header("X-User-Id", "user-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalized").value(false))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void passesFiltersAndPaginationParametersToService() throws Exception {
        when(recommendationService.getRecommendations(
                "user-1", "Action", 2024, 2, 5))
                .thenReturn(new RecommendationPageResponse(
                        List.of(), 2, 5, 0L, 0, false));

        mockMvc.perform(get("/recommendations")
                        .header("X-User-Id", "user-1")
                        .param("genre", "Action")
                        .param("year", "2024")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(recommendationService).getRecommendations(
                "user-1", "Action", 2024, 2, 5);
    }

    @Test
    void returnsBadRequestWhenUserHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/recommendations"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.detail")
                        .value("Required header is missing: X-User-Id"));

        verifyNoInteractions(recommendationService);
    }

    @Test
    void returnsBadRequestWhenYearIsNotNumeric() throws Exception {
        mockMvc.perform(get("/recommendations")
                        .header("X-User-Id", "user-1")
                        .param("year", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Invalid Request"));

        verifyNoInteractions(recommendationService);
    }

    @Test
    void preservesUnauthorizedStatusFromService() throws Exception {
        when(recommendationService.getRecommendations(
                "user-1", null, null, 0, 20))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User ID is required"));

        mockMvc.perform(get("/recommendations")
                        .header("X-User-Id", "user-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value("User ID is required"));
    }

    @Test
    void preservesNotFoundStatusFromService() throws Exception {
        when(recommendationService.getRecommendations(
                "missing-user", null, null, 0, 20))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(get("/recommendations")
                        .header("X-User-Id", "missing-user"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("User not found"));
    }

    @Test
    void preservesBadRequestStatusFromService() throws Exception {
        when(recommendationService.getRecommendations(
                "user-1", null, null, -1, 20))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Page must be zero or greater"));

        mockMvc.perform(get("/recommendations")
                        .header("X-User-Id", "user-1")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.detail")
                        .value("Page must be zero or greater"));
    }
}

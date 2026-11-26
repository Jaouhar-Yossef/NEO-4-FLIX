
package com.neo4flix.recommendationservice.recommendation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.neo4flix.recommendationservice.recommendation.RecommendationDtos.RecommendationPageResponse;
import com.neo4flix.recommendationservice.recommendation.RecommendationDtos.RecommendationResponse;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    private final RecommendationResponse movie = new RecommendationResponse(
            "movie-1",
            "Interstellar",
            2014,
            List.of("Sci-Fi", "Drama"),
            4.8,
            120L,
            0.92,
            3L
    );

    @Test
    void returnsPersonalizedRecommendationsWhenUserHasRatings() {
        when(recommendationRepository.userExists("user-1")).thenReturn(true);
        when(recommendationRepository.hasRatings("user-1")).thenReturn(true);
        when(recommendationRepository.countPersonalized(
                "user-1", "Drama", 2020)).thenReturn(21L);
        when(recommendationRepository.findPersonalized(
                "user-1", "Drama", 2020, 10, 10))
                .thenReturn(List.of(movie));

        RecommendationPageResponse result =
                recommendationService.getRecommendations(
                        "user-1", " Drama ", 2020, 1, 10);

        assertTrue(result.personalized());
        assertEquals(List.of(movie), result.content());
        assertEquals(1, result.page());
        assertEquals(10, result.size());
        assertEquals(21L, result.totalElements());
        assertEquals(3, result.totalPages());

        verify(recommendationRepository).findPersonalized(
                "user-1", "Drama", 2020, 10, 10);
        verify(recommendationRepository, never()).findPopular(
                anyString(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void returnsPopularRecommendationsWhenUserHasNoRatings() {
        when(recommendationRepository.userExists("user-1")).thenReturn(true);
        when(recommendationRepository.hasRatings("user-1")).thenReturn(false);
        when(recommendationRepository.countPopular("", -1)).thenReturn(3L);
        when(recommendationRepository.findPopular("", -1, 0, 20))
                .thenReturn(List.of(movie));

        RecommendationPageResponse result =
                recommendationService.getRecommendations(
                        "user-1", null, null, 0, 20);

        assertFalse(result.personalized());
        assertEquals(List.of(movie), result.content());
        assertEquals(3L, result.totalElements());
        assertEquals(1, result.totalPages());

        verify(recommendationRepository).findPopular("", -1, 0, 20);
        verify(recommendationRepository, never()).findPersonalized(
                anyString(), anyString(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void normalizesGenreAndUsesYearFilterForPopularRecommendations() {
        when(recommendationRepository.userExists("user-1")).thenReturn(true);
        when(recommendationRepository.hasRatings("user-1")).thenReturn(false);
        when(recommendationRepository.countPopular("Action", 2024))
                .thenReturn(0L);
        when(recommendationRepository.findPopular("Action", 2024, 0, 10))
                .thenReturn(List.of());

        RecommendationPageResponse result =
                recommendationService.getRecommendations(
                        "user-1", "  Action  ", 2024, 0, 10);

        assertFalse(result.personalized());
        assertTrue(result.content().isEmpty());
        assertEquals(0L, result.totalElements());
        assertEquals(0, result.totalPages());

        verify(recommendationRepository).countPopular("Action", 2024);
        verify(recommendationRepository).findPopular("Action", 2024, 0, 10);
    }

    @Test
    void calculatesTotalPagesUsingCeiling() {
        when(recommendationRepository.userExists("user-1")).thenReturn(true);
        when(recommendationRepository.hasRatings("user-1")).thenReturn(false);
        when(recommendationRepository.countPopular("", -1)).thenReturn(25L);
        when(recommendationRepository.findPopular("", -1, 0, 10))
                .thenReturn(List.of(movie));

        RecommendationPageResponse result =
                recommendationService.getRecommendations(
                        "user-1", null, null, 0, 10);

        assertEquals(3, result.totalPages());
    }

    @Test
    void rejectsNullUserId() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> recommendationService.getRecommendations(
                        null, null, null, 0, 20));

        assertEquals(401, exception.getStatusCode().value());
        verifyNoInteractions(recommendationRepository);
    }

    @Test
    void rejectsBlankUserId() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> recommendationService.getRecommendations(
                        "  ", null, null, 0, 20));

        assertEquals(401, exception.getStatusCode().value());
        verifyNoInteractions(recommendationRepository);
    }

    @Test
    void rejectsNegativePage() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> recommendationService.getRecommendations(
                        "user-1", null, null, -1, 20));

        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(recommendationRepository);
    }

    @Test
    void rejectsSizeBelowOne() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> recommendationService.getRecommendations(
                        "user-1", null, null, 0, 0));

        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(recommendationRepository);
    }

    @Test
    void rejectsSizeAboveMaximum() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> recommendationService.getRecommendations(
                        "user-1", null, null, 0, 101));

        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(recommendationRepository);
    }

    @Test
    void rejectsGenreLongerThan100Characters() {
        String genre = "a".repeat(101);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> recommendationService.getRecommendations(
                        "user-1", genre, null, 0, 20));

        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(recommendationRepository);
    }

    @Test
    void rejectsPageOffsetExceedingIntegerMaximum() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> recommendationService.getRecommendations(
                        "user-1", null, null, Integer.MAX_VALUE, 100));

        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(recommendationRepository);
    }

    @Test
    void rejectsUnknownUser() {
        when(recommendationRepository.userExists("missing-user"))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> recommendationService.getRecommendations(
                        "missing-user", null, null, 0, 20));

        assertEquals(404, exception.getStatusCode().value());
        verify(recommendationRepository, never()).hasRatings(anyString());
        verify(recommendationRepository, never()).findPopular(
                anyString(), anyInt(), anyInt(), anyInt());
    }
}

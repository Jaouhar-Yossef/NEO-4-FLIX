package com.neo4flix.ratingservice.rating;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.neo4flix.ratingservice.rating.RatingDtos.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @InjectMocks
    private RatingService ratingService;

    private final String userId = "user-test-1";
    private final String movieId = "movie-test-1";

    private RatingResponse response;

    @BeforeEach
    void setUp() {
        response = new RatingResponse(
                movieId, userId, 5, null, null);
    }

    @Test
    void createShouldCreateRatingSuccessfully() {
        when(ratingRepository.userExists(userId)).thenReturn(true);
        when(ratingRepository.movieExists(movieId)).thenReturn(true);
        when(ratingRepository.ratingExists(userId, movieId)).thenReturn(false);
        when(ratingRepository.create(userId, movieId, 5))
                .thenReturn(Optional.of(response));

        RatingResponse result = ratingService.create(
                userId, new CreateRatingRequest(movieId, 5));

        assertEquals(movieId, result.movieId());
        assertEquals(userId, result.userId());
        assertEquals(5, result.rating());

        verify(ratingRepository).create(userId, movieId, 5);
    }

    @Test
    void createShouldRejectMissingUser() {
        when(ratingRepository.userExists(userId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ratingService.create(
                        userId, new CreateRatingRequest(movieId, 5)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(ratingRepository, never()).create(any(), any(), any());
    }

    @Test
    void createShouldRejectMissingMovie() {
        when(ratingRepository.userExists(userId)).thenReturn(true);
        when(ratingRepository.movieExists(movieId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ratingService.create(
                        userId, new CreateRatingRequest(movieId, 5)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(ratingRepository, never()).create(any(), any(), any());
    }

    @Test
    void createShouldRejectDuplicateRating() {
        when(ratingRepository.userExists(userId)).thenReturn(true);
        when(ratingRepository.movieExists(movieId)).thenReturn(true);
        when(ratingRepository.ratingExists(userId, movieId)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ratingService.create(
                        userId, new CreateRatingRequest(movieId, 5)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(ratingRepository, never()).create(any(), any(), any());
    }

    @Test
    void createShouldRejectEmptyUserId() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ratingService.create(
                        " ", new CreateRatingRequest(movieId, 5)));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(ratingRepository);
    }

    @Test
    void createShouldRejectEmptyRepositoryResult() {
        when(ratingRepository.userExists(userId)).thenReturn(true);
        when(ratingRepository.movieExists(movieId)).thenReturn(true);
        when(ratingRepository.ratingExists(userId, movieId)).thenReturn(false);
        when(ratingRepository.create(userId, movieId, 5))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ratingService.create(
                        userId, new CreateRatingRequest(movieId, 5)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void findMineShouldReturnUserRatings() {
        List<MyRatingResponse> ratings = List.of(
                new MyRatingResponse(movieId, "Test Movie", 5,
                        4.5, 2L, null, null));

        when(ratingRepository.userExists(userId)).thenReturn(true);
        when(ratingRepository.findByUser(userId)).thenReturn(ratings);

        var result = ratingService.findMine(userId);

        assertEquals(1, result.size());
        assertEquals(5, result.iterator().next().rating());
    }

    @Test
    void findMineShouldRejectUnknownUser() {
        when(ratingRepository.userExists(userId)).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> ratingService.findMine(userId));

        verify(ratingRepository, never()).findByUser(any());
    }

    @Test
    void findByMovieShouldReturnMovieRatings() {
        List<MovieRatingResponse> ratings = List.of(
                new MovieRatingResponse(userId, "Test User", 4, null, null));

        when(ratingRepository.movieExists(movieId)).thenReturn(true);
        when(ratingRepository.findByMovie(movieId)).thenReturn(ratings);

        var result = ratingService.findByMovie(movieId);

        assertEquals(1, result.size());
        assertEquals(4, result.iterator().next().rating());
    }

    @Test
    void findByMovieShouldRejectUnknownMovie() {
        when(ratingRepository.movieExists(movieId)).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> ratingService.findByMovie(movieId));

        verify(ratingRepository, never()).findByMovie(any());
    }

    @Test
    void updateShouldUpdateOwnRating() {
        when(ratingRepository.userExists(userId)).thenReturn(true);
        when(ratingRepository.movieExists(movieId)).thenReturn(true);
        when(ratingRepository.update(userId, movieId, 4))
                .thenReturn(Optional.of(
                        new RatingResponse(movieId, userId, 4, null, null)));

        RatingResponse result = ratingService.update(
                userId, movieId, new UpdateRatingRequest(4));

        assertEquals(4, result.rating());
        verify(ratingRepository).update(userId, movieId, 4);
    }

    @Test
    void updateShouldRejectRatingNotOwnedByUser() {
        when(ratingRepository.userExists(userId)).thenReturn(true);
        when(ratingRepository.movieExists(movieId)).thenReturn(true);
        when(ratingRepository.update(userId, movieId, 4))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ratingService.update(
                        userId, movieId, new UpdateRatingRequest(4)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void deleteShouldDeleteOwnRating() {
        when(ratingRepository.userExists(userId)).thenReturn(true);
        when(ratingRepository.movieExists(movieId)).thenReturn(true);
        when(ratingRepository.delete(userId, movieId))
                .thenReturn(Optional.of(response));

        assertDoesNotThrow(() -> ratingService.delete(userId, movieId));

        verify(ratingRepository).delete(userId, movieId);
    }

    @Test
    void deleteShouldRejectRatingNotOwnedByUser() {
        when(ratingRepository.userExists(userId)).thenReturn(true);
        when(ratingRepository.movieExists(movieId)).thenReturn(true);
        when(ratingRepository.delete(userId, movieId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ratingService.delete(userId, movieId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}

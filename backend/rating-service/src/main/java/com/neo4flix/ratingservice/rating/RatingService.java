package com.neo4flix.ratingservice.rating;

import java.util.Collection;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.neo4flix.ratingservice.rating.RatingDtos.CreateRatingRequest;
import com.neo4flix.ratingservice.rating.RatingDtos.UpdateRatingRequest;
import com.neo4flix.ratingservice.rating.RatingDtos.RatingResponse;
import com.neo4flix.ratingservice.rating.RatingDtos.MyRatingResponse;
import com.neo4flix.ratingservice.rating.RatingDtos.MovieRatingResponse;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;

    public RatingService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    public RatingResponse create(String userId, CreateRatingRequest request) {
        validateUser(userId);

        if (!ratingRepository.movieExists(request.movieId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Movie not found");
        }

        if (ratingRepository.ratingExists(userId, request.movieId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "You have already rated this movie");
        }

        return ratingRepository.create(
                userId, request.movieId(), request.rating()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User or movie not found"));
    }

    public Collection<MyRatingResponse> findMine(String userId) {
        validateUser(userId);
        return ratingRepository.findByUser(userId);
    }

    public Collection<MovieRatingResponse> findByMovie(String movieId) {
        validateMovie(movieId);
        return ratingRepository.findByMovie(movieId);
    }

    public RatingResponse update(
            String userId, String movieId, UpdateRatingRequest request) {

        validateUser(userId);
        validateMovie(movieId);

        return ratingRepository.update(
                userId, movieId, request.rating()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Rating not found"));
    }

    public void delete(String userId, String movieId) {
        validateUser(userId);
        validateMovie(movieId);

        ratingRepository.delete(userId, movieId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Rating not found"));
    }

    private void validateUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }

        if (!ratingRepository.userExists(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found");
        }
    }

    private void validateMovie(String movieId) {
        if (movieId == null || movieId.isBlank()
                || !ratingRepository.movieExists(movieId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Movie not found");
        }
    }
}

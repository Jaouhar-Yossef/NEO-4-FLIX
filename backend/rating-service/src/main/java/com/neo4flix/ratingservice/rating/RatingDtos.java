package com.neo4flix.ratingservice.rating;

import java.time.ZonedDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class RatingDtos {

    private RatingDtos() {
    }

    public record CreateRatingRequest(
            @NotBlank(message = "Movie ID is required")
            String movieId,

            @NotNull(message = "Rating is required")
            @Min(value = 1, message = "Rating must be at least 1")
            @Max(value = 5, message = "Rating must be at most 5")
            Integer rating
    ) {
    }

    public record UpdateRatingRequest(
            @NotNull(message = "Rating is required")
            @Min(value = 1, message = "Rating must be at least 1")
            @Max(value = 5, message = "Rating must be at most 5")
            Integer rating
    ) {
    }

    public record RatingResponse(
            String movieId,
            String userId,
            Integer rating,
            ZonedDateTime createdAt,
            ZonedDateTime updatedAt
    ) {
    }

    public record MyRatingResponse(
            String movieId,
            String movieTitle,
            Integer rating,
            Double movieAverageRating,
            Long movieRatingCount,
            ZonedDateTime createdAt,
            ZonedDateTime updatedAt
    ) {
    }

    public record MovieRatingResponse(
            String userId,
            String displayName,
            Integer rating,
            ZonedDateTime createdAt,
            ZonedDateTime updatedAt
    ) {
    }
}


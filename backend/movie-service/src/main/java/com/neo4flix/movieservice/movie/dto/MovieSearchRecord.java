package com.neo4flix.movieservice.movie.dto;

public record MovieSearchRecord(
        String id,
        String title,
        Integer releaseYear,
        String genre,
        Double averageRating,
        Long ratingCount
) {
}

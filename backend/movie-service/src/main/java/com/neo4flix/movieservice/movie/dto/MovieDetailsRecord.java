package com.neo4flix.movieservice.movie.dto;

public record MovieDetailsRecord(
        String id,
        String title,
        Integer releaseYear,
        String genre,
        Double averageRating,
        Long ratingCount,
        Integer myRating,
        Boolean inWatchlist
) {
}

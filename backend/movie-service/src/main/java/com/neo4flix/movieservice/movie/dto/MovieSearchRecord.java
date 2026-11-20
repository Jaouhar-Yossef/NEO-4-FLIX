package com.neo4flix.movieservice.movie.dto;

import java.util.List;

public record MovieSearchRecord(
        String id,
        String title,
        Integer releaseYear,
        List<String> genres,
        Double averageRating,
        Long ratingCount
) {
}

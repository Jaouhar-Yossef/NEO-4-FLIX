package com.neo4flix.movieservice.movie.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieSummary {

    private String id;

    private String title;

    private Integer releaseYear;

    private List<String> genres;

    private Double averageRating;

    private Long ratingCount;
}

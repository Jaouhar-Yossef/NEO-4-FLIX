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
public class MoviePageResponse {

    private List<MovieSummary> content;

    private int page;

    private int size;

    private long totalElements;
}

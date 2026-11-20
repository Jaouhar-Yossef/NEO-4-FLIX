package com.neo4flix.movieservice.movie.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MovieRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotNull
    @Min(1888)
    @Max(2100)
    private Integer releaseYear;

    @NotEmpty
    private List<@NotBlank String> genres;
}

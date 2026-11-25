package com.neo4flix.movieservice.movie;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neo4flix.movieservice.movie.dto.MovieDetailsResponse;
import com.neo4flix.movieservice.movie.dto.MoviePageResponse;
import com.neo4flix.movieservice.movie.dto.MovieRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping({ "", "/search" })
    public MoviePageResponse searchMovies(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        return movieService.searchMovies(
                title,
                genre,
                year,
                page,
                size,
                sortBy,
                direction);
    }

    @GetMapping("/{id}")
    public MovieDetailsResponse getMovieDetails(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        return movieService.getMovieDetails(id, userId);
    }

    @PostMapping
    public Movie createMovie(@Valid @RequestBody MovieRequest request) {
        return movieService.createMovie(request);
    }

    @PutMapping("/{id}")
    public Movie updateMovie(
            @PathVariable String id,
            @Valid @RequestBody MovieRequest request) {

        return movieService.updateMovie(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteMovie(@PathVariable String id) {
        movieService.deleteMovie(id);
    }
}

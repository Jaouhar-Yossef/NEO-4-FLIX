package com.neo4flix.movieservice.movie;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neo4flix.movieservice.genre.Genre;
import com.neo4flix.movieservice.genre.GenreRepository;
import com.neo4flix.movieservice.movie.dto.MovieRequest;
import com.neo4flix.movieservice.movie.dto.MovieSearchRecord;
import com.neo4flix.movieservice.movie.dto.MovieSummary;
import com.neo4flix.movieservice.movie.dto.MoviePageResponse;
import com.neo4flix.movieservice.common.exception.MovieNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;

    @Transactional
    public Movie createMovie(MovieRequest request) {

        Set<Genre> genres = new HashSet<>();

        for (String genreName : request.getGenres()) {
            String normalizedName = genreName.trim();

            Genre genre = genreRepository.findByName(normalizedName)
                    .orElseGet(() -> genreRepository.save(
                            new Genre(
                                    UUID.randomUUID().toString(),
                                    normalizedName)));

            genres.add(genre);
        }

        Movie movie = new Movie();
        movie.setId("movie-" + UUID.randomUUID());
        movie.setTitle(request.getTitle().trim());
        movie.setReleaseYear(request.getReleaseYear());
        movie.setGenres(genres);

        return movieRepository.save(movie);
    }

    @Transactional
    public Movie updateMovie(String id, MovieRequest request) {

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException(id));

        Set<Genre> genres = new HashSet<>();

        for (String genreName : request.getGenres()) {
            String normalizedName = genreName.trim();

            Genre genre = genreRepository.findByName(normalizedName)
                    .orElseGet(() -> genreRepository.save(
                            new Genre(
                                    UUID.randomUUID().toString(),
                                    normalizedName)));

            genres.add(genre);
        }

        movie.setTitle(request.getTitle().trim());
        movie.setReleaseYear(request.getReleaseYear());
        movie.setGenres(genres);

        return movieRepository.save(movie);
    }


    @Transactional(readOnly = true)
    public MoviePageResponse searchMovies(
            String title,
            String genre,
            Integer year,
            int page,
            int size) {

        if (page < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }

        String normalizedTitle = (title == null || title.isBlank())
                ? null : title.trim();

        String normalizedGenre = (genre == null || genre.isBlank())
                ? null : genre.trim();

        int skip = Math.multiplyExact(page, size);

        List<MovieSearchRecord> records = movieRepository.searchMovies(
                normalizedTitle,
                normalizedGenre,
                year,
                skip,
                size
        );

        List<MovieSummary> movies = records.stream()
                .map(record -> new MovieSummary(
                        record.id(),
                        record.title(),
                        record.releaseYear(),
                        record.genres(),
                        record.averageRating(),
                        record.ratingCount()
                ))
                .toList();

        long totalElements = movieRepository.countMovies(
                normalizedTitle,
                normalizedGenre,
                year
        );

        return new MoviePageResponse(
                movies,
                page,
                size,
                totalElements
        );
    }

    @Transactional
    public void deleteMovie(String id) {

        if (!movieRepository.existsById(id)) {
            throw new MovieNotFoundException(id);
        }

        movieRepository.deleteWithRelationships(id);
    }

}

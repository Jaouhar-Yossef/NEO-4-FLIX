package com.neo4flix.movieservice.movie;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.neo4flix.movieservice.movie.dto.MovieDetailsRecord;
import com.neo4flix.movieservice.movie.dto.MovieDetailsResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neo4flix.movieservice.common.exception.MovieNotFoundException;
import com.neo4flix.movieservice.genre.Genre;
import com.neo4flix.movieservice.genre.GenreRepository;
import com.neo4flix.movieservice.movie.dto.MoviePageResponse;
import com.neo4flix.movieservice.movie.dto.MovieRequest;
import com.neo4flix.movieservice.movie.dto.MovieSearchRecord;
import com.neo4flix.movieservice.movie.dto.MovieSummary;

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
            int size,
            String sortBy,
            String direction) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Size must be between 1 and 100");
        }

        if (year != null && (year < 1888 || year > 2100)) {
            throw new IllegalArgumentException(
                    "Year must be between 1888 and 2100");
        }

        String normalizedSortBy = (sortBy == null || sortBy.isBlank())
                ? "title"
                : sortBy.trim();

        if (!normalizedSortBy.equals("title")
                && !normalizedSortBy.equals("releaseYear")) {
            throw new IllegalArgumentException(
                    "sortBy must be either title or releaseYear");
        }

        String normalizedDirection = (direction == null || direction.isBlank())
                ? "ASC"
                : direction.trim().toUpperCase();

        if (!normalizedDirection.equals("ASC")
                && !normalizedDirection.equals("DESC")) {
            throw new IllegalArgumentException(
                    "direction must be either ASC or DESC");
        }

        String normalizedTitle = (title == null || title.isBlank())
                ? null
                : title.trim();

        String normalizedGenre = (genre == null || genre.isBlank())
                ? null
                : genre.trim();

        int skip = Math.multiplyExact(page, size);

        List<MovieSearchRecord> records = movieRepository.searchMovies(
                normalizedTitle,
                normalizedGenre,
                year,
                normalizedSortBy,
                normalizedDirection,
                skip,
                size);

        // Group rows by movie ID because each row contains one genre.
        Map<String, MovieSummary> movieMap = new LinkedHashMap<>();

        for (MovieSearchRecord record : records) {

            MovieSummary movie = movieMap.computeIfAbsent(
                    record.id(),
                    id -> new MovieSummary(
                            record.id(),
                            record.title(),
                            record.releaseYear(),
                            new ArrayList<>(),
                            record.averageRating(),
                            record.ratingCount()));

            if (record.genre() != null
                    && !movie.getGenres().contains(record.genre())) {
                movie.getGenres().add(record.genre());
            }
        }

        List<MovieSummary> movies = new ArrayList<>(movieMap.values());

        long totalElements = movieRepository.countMovies(
                normalizedTitle,
                normalizedGenre,
                year);

        return new MoviePageResponse(
                movies,
                page,
                size,
                totalElements);
    }

    @Transactional
    public void deleteMovie(String id) {

        if (!movieRepository.existsById(id)) {
            throw new MovieNotFoundException(id);
        }

        movieRepository.deleteWithRelationships(id);
    }

    @Transactional(readOnly = true)
    public MovieDetailsResponse getMovieDetails(String id, String userId) {

        List<MovieDetailsRecord> records =
                movieRepository.findMovieDetails(id, userId);

        if (records.isEmpty()) {
            throw new MovieNotFoundException(id);
        }

        MovieDetailsRecord first = records.get(0);

        MovieDetailsResponse response = new MovieDetailsResponse();

        response.setId(first.id());
        response.setTitle(first.title());
        response.setReleaseYear(first.releaseYear());
        response.setAverageRating(first.averageRating());
        response.setRatingCount(first.ratingCount());
        response.setMyRating(first.myRating());
        response.setInWatchlist(Boolean.TRUE.equals(first.inWatchlist()));

        List<String> genres = records.stream()
                .map(MovieDetailsRecord::genre)
                .filter(genre -> genre != null)
                .distinct()
                .toList();

        response.setGenres(new ArrayList<>(genres));

        return response;
    }
}

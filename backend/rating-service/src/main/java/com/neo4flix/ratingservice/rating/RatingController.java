package com.neo4flix.ratingservice.rating;

import java.util.Collection;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neo4flix.ratingservice.rating.RatingDtos.CreateRatingRequest;
import com.neo4flix.ratingservice.rating.RatingDtos.UpdateRatingRequest;
import com.neo4flix.ratingservice.rating.RatingDtos.RatingResponse;
import com.neo4flix.ratingservice.rating.RatingDtos.MyRatingResponse;
import com.neo4flix.ratingservice.rating.RatingDtos.MovieRatingResponse;

@RestController
@RequestMapping("/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    // POST /ratings
    @PostMapping
    public ResponseEntity<RatingResponse> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateRatingRequest request) {

        RatingResponse response = ratingService.create(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /ratings/me
    @GetMapping("/me")
    public ResponseEntity<Collection<MyRatingResponse>> findMine(
            @RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.ok(ratingService.findMine(userId));
    }

    // GET /ratings/movie/{movieId}
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<Collection<MovieRatingResponse>> findByMovie(
            @PathVariable String movieId) {

        return ResponseEntity.ok(ratingService.findByMovie(movieId));
    }

    // PUT /ratings/{movieId}
    @PutMapping("/{movieId}")
    public ResponseEntity<RatingResponse> update(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String movieId,
            @Valid @RequestBody UpdateRatingRequest request) {

        return ResponseEntity.ok(
                ratingService.update(userId, movieId, request)
        );
    }

    // DELETE /ratings/{movieId}
    @DeleteMapping("/{movieId}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String movieId) {

        ratingService.delete(userId, movieId);

        return ResponseEntity.noContent().build();
    }
}


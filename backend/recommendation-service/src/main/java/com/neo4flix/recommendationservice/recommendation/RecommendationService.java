package com.neo4flix.recommendationservice.recommendation;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.neo4flix.recommendationservice.recommendation.RecommendationDtos.RecommendationPageResponse;
import com.neo4flix.recommendationservice.recommendation.RecommendationDtos.RecommendationResponse;

@Service
public class RecommendationService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final RecommendationRepository recommendationRepository;

    public RecommendationService(RecommendationRepository recommendationRepository) {
        this.recommendationRepository = recommendationRepository;
    }

    public RecommendationPageResponse getRecommendations(
            String userId,
            String genre,
            Integer year,
            int page,
            int size) {

        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated user is required"
            );
        }

        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be zero or greater"
            );
        }

        if (size < 1 || size > MAX_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Size must be between 1 and " + MAX_SIZE
            );
        }

        String normalizedGenre = genre == null ? "" : genre.trim();

        if (normalizedGenre.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Genre must not exceed 100 characters"
            );
        }

        int filterYear = year == null ? -1 : year;

        long offset = (long) page * size;

        if (offset > Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Requested page is too large"
            );
        }

        if (!recommendationRepository.userExists(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }

        boolean personalized = recommendationRepository.hasRatings(userId);

        long totalElements;
        List<RecommendationResponse> recommendations;

        if (personalized) {
            totalElements = recommendationRepository.countPersonalized(
                    userId, normalizedGenre, filterYear
            );

            recommendations = recommendationRepository.findPersonalized(
                    userId,
                    normalizedGenre,
                    filterYear,
                    (int) offset,
                    size
            );
        } else {
            totalElements = recommendationRepository.countPopular(
                    normalizedGenre, filterYear
            );

            recommendations = recommendationRepository.findPopular(
                    normalizedGenre,
                    filterYear,
                    (int) offset,
                    size
            );
        }

        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / size);

        return new RecommendationPageResponse(
                recommendations,
                page,
                size,
                totalElements,
                totalPages,
                personalized
        );
    }
}

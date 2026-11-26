package com.neo4flix.recommendationservice.recommendation;

import java.util.List;

public final class RecommendationDtos {

    private RecommendationDtos() {
    }

    public record RecommendationResponse(
            String id,
            String title,
            Integer releaseYear,
            List<String> genres,
            Double averageRating,
            Long ratingCount,
            Double relevanceScore,
            Long similarUsersCount
    ) {
    }

    public record RecommendationPageResponse(
            List<RecommendationResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean personalized
    ) {
    }
}

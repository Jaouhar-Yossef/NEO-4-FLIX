package com.neo4flix.userservice.user;

import java.time.ZonedDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserDtos {

	private UserDtos() {
	}

	public record ProfileResponse(String id, String email, String displayName, boolean twoFactorEnabled) {
		static ProfileResponse of(User user) {
			return new ProfileResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.isTwoFactorEnabled());
		}
	}

	public record MovieSummary(String id, String title, Integer releaseYear, List<String> genres,
			Double averageRating, long ratingCount) {
	}

	public record MyRatingResponse(MovieSummary movie, int score, ZonedDateTime updatedAt) {
	}

	public record UpdateProfileRequest(
			@NotBlank(message = "displayName is required")
			@Size(max = 50, message = "displayName must be at most 50 characters")
			String displayName) {
	}
}

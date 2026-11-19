package com.neo4flix.userservice.user;

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

	public record UpdateProfileRequest(
			@NotBlank(message = "displayName is required")
			@Size(max = 50, message = "displayName must be at most 50 characters")
			String displayName) {
	}
}

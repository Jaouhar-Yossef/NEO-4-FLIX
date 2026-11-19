package com.neo4flix.userservice.user;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.neo4flix.userservice.user.UserDtos.*;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository users;

	public ProfileResponse getProfile(String userId) {
		return ProfileResponse.of(currentUser(userId));
	}

	public ProfileResponse updateProfile(String userId, UpdateProfileRequest req) {
		User user = currentUser(userId);
		user.setDisplayName(req.displayName().trim());
		return ProfileResponse.of(users.save(user));
	}

	public void deleteAccount(String userId) {
		users.deleteWithRelationships(userId);
	}

	private User currentUser(String userId) {
		return users.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
	}
}

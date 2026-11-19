package com.neo4flix.userservice.user;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.neo4flix.userservice.user.UserDtos.*;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping
	public ProfileResponse me(@AuthenticationPrincipal String userId) {
		return userService.getProfile(userId);
	}

	@PutMapping
	public ProfileResponse update(@AuthenticationPrincipal String userId, @Valid @RequestBody UpdateProfileRequest body) {
		return userService.updateProfile(userId, body);
	}

	@GetMapping("/ratings")
	public List<MyRatingResponse> ratings(@AuthenticationPrincipal String userId) {
		return userService.getRatings(userId);
	}

	@DeleteMapping
	public ResponseEntity<Void> delete(@AuthenticationPrincipal String userId) {
		userService.deleteAccount(userId);
		return ResponseEntity.noContent().build();
	}
}

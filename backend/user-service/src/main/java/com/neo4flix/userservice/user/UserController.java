package com.neo4flix.userservice.user;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

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
}

package com.neo4flix.userservice.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.neo4flix.userservice.user.UserDtos.*;

class UserServiceTest {

	private final UserRepository users = mock(UserRepository.class);
	private final RatingHistoryRepository ratingHistory = mock(RatingHistoryRepository.class);
	private final UserService service = new UserService(users, ratingHistory);

	@Test
	void updateTrimsDisplayName() {
		User alice = new User();
		alice.setId("user-1");
		alice.setDisplayName("Alice");
		when(users.findById("user-1")).thenReturn(Optional.of(alice));
		when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));

		ProfileResponse res = service.updateProfile("user-1", new UpdateProfileRequest("  Alice B.  "));

		assertThat(res.displayName()).isEqualTo("Alice B.");
	}

	@Test
	void unknownUserGives404() {
		when(users.findById("gone")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getProfile("gone"))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("404");
	}

	@Test
	void ratingsComeFromRatingHistory() {
		when(ratingHistory.findByUser("user-1")).thenReturn(List.of());

		assertThat(service.getRatings("user-1")).isEmpty();
		verify(ratingHistory).findByUser("user-1");
	}

	@Test
	void deleteRemovesUserWithRelationships() {
		service.deleteAccount("user-1");

		verify(users).deleteWithRelationships("user-1");
	}
}

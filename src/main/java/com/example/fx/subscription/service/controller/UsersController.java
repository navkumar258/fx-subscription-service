package com.example.fx.subscription.service.controller;

import com.example.fx.subscription.service.dto.user.*;
import com.example.fx.subscription.service.exception.UserNotFoundException;
import com.example.fx.subscription.service.model.FxUser;
import com.example.fx.subscription.service.service.FxUsersService;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.fx.subscription.service.util.LogSanitizer.sanitizeForLog;

@RestController
@RequestMapping("/api/v1/users")
@Observed(name = "users.controller")
public class UsersController {

	private static final Logger LOGGER = LoggerFactory.getLogger(UsersController.class);
	private final FxUsersService fxUsersService;

	public UsersController(FxUsersService fxUsersService) {
		this.fxUsersService = fxUsersService;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('ADMIN')")
	public UserListResponse getAllUsers(
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		Page<FxUser> usersPage = fxUsersService.findAllUsers(pageable);
		List<UserSummaryResponse> userResponses = usersPage.getContent().stream()
				.map(UserSummaryResponse::fromFxUser)
				.toList();

		LOGGER.info("Retrieved {} users from page {} of {}",
				usersPage.getNumberOfElements(), usersPage.getNumber(), usersPage.getTotalPages());

		return new UserListResponse(
				userResponses,
				usersPage.getTotalElements(),
				usersPage.getTotalPages(),
				usersPage.getNumber(),
				usersPage.getSize()
		);
	}

	@GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id.toString()")
	public UserDetailResponse getUserById(@PathVariable String id) {
		FxUser fxUser = fxUsersService.findUserById(id)
				.orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id, id));

		LOGGER.atInfo().log("Retrieved user: userId={}", sanitizeForLog(id));

		return UserDetailResponse.fromFxUser(fxUser);
	}

	@GetMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('ADMIN')")
	public UserListResponse searchUsers(
			@RequestParam(required = false) String email,
			@RequestParam(required = false) String mobile,
			@RequestParam(defaultValue = "true") boolean enabled,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		Page<FxUser> usersPage = fxUsersService.searchUsers(email, mobile, enabled, pageable);
		List<UserSummaryResponse> userResponses = usersPage.getContent().stream()
				.map(UserSummaryResponse::fromFxUser)
				.toList();

		return new UserListResponse(
				userResponses,
				usersPage.getTotalElements(),
				usersPage.getTotalPages(),
				usersPage.getNumber(),
				usersPage.getSize()
		);
	}

	@PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id.toString()")
	public UserUpdateResponse updateUser(
			@PathVariable String id,
			@Valid @RequestBody UserUpdateRequest userUpdateRequest) {
		FxUser updatedUser = fxUsersService.updateUser(id, userUpdateRequest);

		LOGGER.atInfo().log("User updated successfully: userId={}", sanitizeForLog(id));

		return UserUpdateResponse.fromFxUser(updatedUser);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('ADMIN')")
	public void deleteUser(@PathVariable String id) {
		fxUsersService.deleteUser(id);
		LOGGER.atInfo().log("User deleted successfully: userId={}", sanitizeForLog(id));
	}

	@GetMapping(path = "/{id}/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id.toString()")
	public UserSubscriptionsResponse getUserSubscriptions(@PathVariable String id) {
		UserSubscriptionsResponse response = fxUsersService.getUserSubscriptions(id);

		LOGGER.atInfo().log("Retrieved subscriptions for user: userId={}, count={}", sanitizeForLog(id), response.totalCount());

		return response;
	}
}
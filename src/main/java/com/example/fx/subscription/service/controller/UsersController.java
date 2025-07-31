package com.example.fx.subscription.service.controller;

import com.example.fx.subscription.service.dto.user.*;
import com.example.fx.subscription.service.exception.UserNotFoundException;
import com.example.fx.subscription.service.model.FxUser;
import com.example.fx.subscription.service.service.FxUsersService;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

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
  public ResponseEntity<UserListResponse> getAllUsers(
          @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<FxUser> usersPage = fxUsersService.findAllUsers(pageable);
    List<UserSummaryResponse> userResponses = usersPage.getContent().stream()
            .map(UserSummaryResponse::fromFxUser)
            .toList();

    UserListResponse response = new UserListResponse(
            userResponses,
            usersPage.getTotalElements(),
            usersPage.getTotalPages(),
            usersPage.getNumber(),
            usersPage.getSize()
    );

    LOGGER.info("Retrieved {} users from page {} of {}",
            usersPage.getNumberOfElements(), usersPage.getNumber(), usersPage.getTotalPages());

    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id.toString()")
  public ResponseEntity<UserDetailResponse> getUserById(@PathVariable String id) {
    FxUser fxUser = fxUsersService.findUserById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id, id));

    LOGGER.info("Retrieved user: userId={}", id);

    UserDetailResponse response = UserDetailResponse.fromFxUser(fxUser);
    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserListResponse> searchUsers(
          @RequestParam(required = false) String email,
          @RequestParam(required = false) String mobile,
          @RequestParam(defaultValue = "true") boolean enabled,
          @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<FxUser> usersPage = fxUsersService.searchUsers(email, mobile, enabled, pageable);
    List<UserSummaryResponse> userResponses = usersPage.getContent().stream()
            .map(UserSummaryResponse::fromFxUser)
            .toList();

    UserListResponse response = new UserListResponse(
            userResponses,
            usersPage.getTotalElements(),
            usersPage.getTotalPages(),
            usersPage.getNumber(),
            usersPage.getSize()
    );

    LOGGER.info("Search results: {} users found with email={}, mobile={}, enabled={}",
            usersPage.getTotalElements(), email, mobile, enabled);

    return ResponseEntity.ok(response);
  }

  @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id.toString()")
  public ResponseEntity<UserUpdateResponse> updateUser(
          @PathVariable String id,
          @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
    FxUser updatedUser = fxUsersService.updateUser(id, userUpdateRequest);
    UserUpdateResponse response = UserUpdateResponse.fromFxUser(updatedUser);

    LOGGER.info("User updated successfully: userId={}", id);

    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteUser(@PathVariable String id) {
    fxUsersService.deleteUser(id);
    LOGGER.info("User deleted successfully: userId={}", id);

    return ResponseEntity.noContent().build();
  }

  @GetMapping(path = "/{id}/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id.toString()")
  public ResponseEntity<UserSubscriptionsResponse> getUserSubscriptions(@PathVariable String id) {
    UserSubscriptionsResponse response = fxUsersService.getUserSubscriptions(id);

    LOGGER.info("Retrieved subscriptions for user: userId={}, count={}", id, response.totalCount());

    return ResponseEntity.ok(response);
  }
} 
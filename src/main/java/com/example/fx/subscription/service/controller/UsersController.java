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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
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
          @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
    
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

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Retrieved {} users from page {} of {}", 
          usersPage.getNumberOfElements(), usersPage.getNumber(), usersPage.getTotalPages());
    }

    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id.toString()")
  public ResponseEntity<UserDetailResponse> getUserById(@PathVariable String id) {
    Optional<FxUser> user = fxUsersService.findUserById(id);

    if (user.isPresent()) {
      UserDetailResponse response = UserDetailResponse.fromFxUser(user.get());
      
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Retrieved user: userId={}", id);
      }
      
      return ResponseEntity.ok(response);
    }

    throw new UserNotFoundException("User not found with ID: " + id, id);
  }

  @GetMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserListResponse> searchUsers(
          @RequestParam(required = false) String email,
          @RequestParam(required = false) String mobile,
          @RequestParam(defaultValue = "true") boolean enabled,
          @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
    
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

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Search results: {} users found with email={}, mobile={}, enabled={}", 
          usersPage.getTotalElements(), email, mobile, enabled);
    }

    return ResponseEntity.ok(response);
  }

  @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id.toString()")
  public ResponseEntity<UserUpdateResponse> updateUser(
          @PathVariable String id,
          @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
    
    Optional<FxUser> existingUser = fxUsersService.findUserById(id);
    
    if (existingUser.isPresent()) {
      FxUser updatedUser = fxUsersService.updateUser(id, userUpdateRequest);
      UserUpdateResponse response = UserUpdateResponse.fromFxUser(updatedUser);
      
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("User updated successfully: userId={}", id);
      }
      
      return ResponseEntity.ok(response);
    }

    throw new UserNotFoundException("User not found with ID: " + id, id);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteUser(@PathVariable String id) {
    Optional<FxUser> existingUser = fxUsersService.findUserById(id);
    
    if (existingUser.isPresent()) {
      fxUsersService.deleteUser(id);
      
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("User deleted successfully: userId={}", id);
      }
      
      return ResponseEntity.noContent().build(); // 204 No Content
    }

    throw new UserNotFoundException("User not found with ID: " + id, id);
  }

  @GetMapping(path = "/{id}/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id.toString()")
  public ResponseEntity<UserSubscriptionsResponse> getUserSubscriptions(@PathVariable String id) {
    Optional<FxUser> user = fxUsersService.findUserById(id);
    
    if (user.isPresent()) {
      UserSubscriptionsResponse response = fxUsersService.getUserSubscriptions(id);
      
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Retrieved subscriptions for user: userId={}, count={}", 
            id, response.subscriptions().size());
      }
      
      return ResponseEntity.ok(response);
    }

    throw new UserNotFoundException("User not found with ID: " + id, id);
  }
} 
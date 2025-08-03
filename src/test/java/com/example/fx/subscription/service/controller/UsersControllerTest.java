package com.example.fx.subscription.service.controller;

import com.example.fx.subscription.service.ai.tool.FxSubscriptionTool;
import com.example.fx.subscription.service.dto.user.*;
import com.example.fx.subscription.service.exception.UserNotFoundException;
import com.example.fx.subscription.service.model.FxUser;
import com.example.fx.subscription.service.model.UserRole;
import com.example.fx.subscription.service.service.FxUsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@WebMvcTest(UsersController.class)
class UsersControllerTest {

    @MockitoBean
    private FxSubscriptionTool fxSubscriptionTool;

    @MockitoBean
    private FxUsersService fxUsersService;

    private UsersController usersController;

    @BeforeEach
    void setUp() {
        usersController = new UsersController(fxUsersService);
    }

    @Test
    void getAllUsers_ShouldReturnUserList() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        List<FxUser> users = createTestUsers();
        Page<FxUser> usersPage = new PageImpl<>(users, pageable, users.size());

        when(fxUsersService.findAllUsers(pageable)).thenReturn(usersPage);

        // When
        ResponseEntity<UserListResponse> response = usersController.getAllUsers(pageable);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(users.size(), response.getBody().users().size());
        assertEquals(users.size(), response.getBody().totalElements());

        verify(fxUsersService).findAllUsers(pageable);
    }

    @Test
    void getUserById_WithValidId_ShouldReturnUser() {
        // Given
        String userId = UUID.randomUUID().toString();
        FxUser user = createTestUser(userId, "test@example.com");

        when(fxUsersService.findUserById(userId)).thenReturn(Optional.of(user));

        // When
        ResponseEntity<UserDetailResponse> response = usersController.getUserById(userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userId, response.getBody().id().toString());
        assertEquals("test@example.com", response.getBody().email());

        verify(fxUsersService).findUserById(userId);
    }

    @Test
    void getUserById_WithInvalidId_ShouldThrowUserNotFoundException() {
        // Given
        String userId = UUID.randomUUID().toString();

        when(fxUsersService.findUserById(userId)).thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> 
            usersController.getUserById(userId));

        assertEquals("User not found with ID: " + userId, exception.getMessage());
        assertEquals(userId, exception.getUserId());

        verify(fxUsersService).findUserById(userId);
    }

    @Test
    void searchUsers_WithValidCriteria_ShouldReturnFilteredUsers() {
        // Given
        String email = "test@example.com";
        String mobile = "+1234567890";
        boolean enabled = true;
        Pageable pageable = PageRequest.of(0, 20);
        
        List<FxUser> users = createTestUsers();
        Page<FxUser> usersPage = new PageImpl<>(users, pageable, users.size());

        when(fxUsersService.searchUsers(email, mobile, enabled, pageable)).thenReturn(usersPage);

        // When
        ResponseEntity<UserListResponse> response = usersController.searchUsers(email, mobile, enabled, pageable);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(users.size(), response.getBody().users().size());

        verify(fxUsersService).searchUsers(email, mobile, enabled, pageable);
    }

    @Test
    void updateUser_WithValidData_ShouldUpdateUser() {
        // Given
        String userId = UUID.randomUUID().toString();
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "updated@example.com", "+9876543210", "device-token-123");
        
        FxUser updatedUser = createTestUser(userId, "updated@example.com");

        when(fxUsersService.findUserById(userId)).thenReturn(Optional.of(updatedUser));
        when(fxUsersService.updateUser(userId, updateRequest)).thenReturn(updatedUser);

        // When
        ResponseEntity<UserUpdateResponse> response = usersController.updateUser(userId, updateRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userId, response.getBody().userId().toString());
        assertEquals("updated@example.com", response.getBody().user().email());

        verify(fxUsersService).updateUser(userId, updateRequest);
    }

    @Test
    void updateUser_WithInvalidId_ShouldThrowUserNotFoundException() {
        // Given
        String userId = UUID.randomUUID().toString();
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "updated@example.com", "+9876543210", "device-token-123");

        when(fxUsersService.updateUser(anyString(), any(UserUpdateRequest.class))).thenThrow(
                new UserNotFoundException("User not found with ID: " + userId, userId));

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> 
            usersController.updateUser(userId, updateRequest));

        assertEquals("User not found with ID: " + userId, exception.getMessage());
        assertEquals(userId, exception.getUserId());

        verify(fxUsersService).updateUser(anyString(), any());
    }

    @Test
    void deleteUser_WithValidId_ShouldDeleteUser() {
        // Given
        String userId = UUID.randomUUID().toString();
        FxUser existingUser = createTestUser(userId, "test@example.com");

        when(fxUsersService.findUserById(userId)).thenReturn(Optional.of(existingUser));
        doNothing().when(fxUsersService).deleteUser(userId);

        // When
        ResponseEntity<Void> response = usersController.deleteUser(userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        verify(fxUsersService).deleteUser(userId);
    }

    @Test
    void getUserSubscriptions_WithValidId_ShouldReturnSubscriptions() {
        // Given
        String userId = UUID.randomUUID().toString();
        UserSubscriptionsResponse subscriptionsResponse = new UserSubscriptionsResponse(
                UUID.fromString(userId),
                new ArrayList<>(),
                0
        );

        when(fxUsersService.getUserSubscriptions(userId)).thenReturn(subscriptionsResponse);

        // When
        ResponseEntity<UserSubscriptionsResponse> response = usersController.getUserSubscriptions(userId);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(subscriptionsResponse, response.getBody());

        verify(fxUsersService).getUserSubscriptions(userId);
    }

    @Test
    void getUserSubscriptions_WithInvalidId_ShouldThrowUserNotFoundException() {
        // Given
        String userId = UUID.randomUUID().toString();

        when(fxUsersService.getUserSubscriptions(userId)).thenThrow(
                new UserNotFoundException("User not found with ID: " + userId, userId));

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () ->
                usersController.getUserSubscriptions(userId));

        assertEquals("User not found with ID: " + userId, exception.getMessage());
        assertEquals(userId, exception.getUserId());
    }

    // Helper methods
    private List<FxUser> createTestUsers() {
        return List.of(
                createTestUser(UUID.randomUUID().toString(), "user1@example.com"),
                createTestUser(UUID.randomUUID().toString(), "user2@example.com"),
                createTestUser(UUID.randomUUID().toString(), "user3@example.com")
        );
    }

    private FxUser createTestUser(String id, String email) {
        FxUser user = new FxUser();
        user.setId(UUID.fromString(id));
        user.setEmail(email);
        user.setMobile("+1234567890");
        user.setPassword("encodedPassword");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }
} 
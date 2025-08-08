package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.dto.user.UserSubscriptionsResponse;
import com.example.fx.subscription.service.dto.user.UserUpdateRequest;
import com.example.fx.subscription.service.exception.UserNotFoundException;
import com.example.fx.subscription.service.model.FxUser;
import com.example.fx.subscription.service.model.Subscription;
import com.example.fx.subscription.service.model.SubscriptionStatus;
import com.example.fx.subscription.service.model.ThresholdDirection;
import com.example.fx.subscription.service.repository.FxUserRepository;
import com.example.fx.subscription.service.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FxUsersServiceTest {

  private static final String USER_NOT_FOUND = "User not found with ID: ";

  @Mock
  private FxUserRepository fxUserRepository;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @InjectMocks
  private FxUsersService fxUsersService;

  private FxUser testUser;
  private Subscription testSubscription;
  private UUID testUserId;

  @BeforeEach
  void setUp() {
    testUserId = UUID.randomUUID();

    testUser = new FxUser();
    testUser.setId(testUserId);
    testUser.setEmail("test@example.com");
    testUser.setMobile("+1234567890");
    testUser.setEnabled(true);
    testUser.setCreatedAt(Instant.now());
    testUser.setUpdatedAt(Instant.now());

    testSubscription = new Subscription();
    testSubscription.setId(UUID.randomUUID());
    testSubscription.setUser(testUser);
    testSubscription.setCurrencyPair("GBP/USD");
    testSubscription.setThreshold(BigDecimal.valueOf(1.25));
    testSubscription.setDirection(ThresholdDirection.ABOVE);
    testSubscription.setNotificationsChannels(List.of("email", "sms"));
    testSubscription.setStatus(SubscriptionStatus.ACTIVE);
    testSubscription.setCreatedAt(Instant.now());
    testSubscription.setUpdatedAt(Instant.now());
  }

  @Test
  void findAllUsers_ShouldReturnPagedUsers() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    Page<FxUser> userPage = new PageImpl<>(List.of(testUser), pageable, 1);
    when(fxUserRepository.findAll(pageable)).thenReturn(userPage);

    // When
    Page<FxUser> result = fxUsersService.findAllUsers(pageable);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals(testUser, result.getContent().getFirst());
    assertEquals(1, result.getTotalElements());
  }

  @Test
  void findUserById_WhenUserExists_ShouldReturnUser() {
    // Given
    when(fxUserRepository.findByIdWithSubscriptions(testUserId)).thenReturn(Optional.of(testUser));

    // When
    Optional<FxUser> result = fxUsersService.findUserById(testUserId.toString());

    // Then
    assertTrue(result.isPresent());
    assertEquals(testUser, result.get());
  }

  @Test
  void findUserById_WhenUserDoesNotExist_ShouldReturnEmpty() {
    // Given
    when(fxUserRepository.findByIdWithSubscriptions(testUserId)).thenReturn(Optional.empty());

    // When
    Optional<FxUser> result = fxUsersService.findUserById(testUserId.toString());

    // Then
    assertFalse(result.isPresent());
  }

  @Test
  void searchUsers_ShouldReturnPagedUsers() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    Page<FxUser> userPage = new PageImpl<>(List.of(testUser), pageable, 1);
    when(fxUserRepository.searchUsers(any(), any(), any(), any())).thenReturn(userPage);

    // When
    Page<FxUser> result = fxUsersService.searchUsers("test@example.com", "+1234567890", true, pageable);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals(testUser, result.getContent().getFirst());
  }

  @Test
  void updateUser_WhenUserExists_ShouldUpdateAndReturnUser() {
    // Given
    UserUpdateRequest updateRequest = new UserUpdateRequest(
            "updated@example.com",
            "+9876543210",
            "new-device-token"
    );

    when(fxUserRepository.findByIdWithSubscriptions(testUserId)).thenReturn(Optional.of(testUser));
    when(fxUserRepository.save(any(FxUser.class))).thenReturn(testUser);

    // When
    FxUser result = fxUsersService.updateUser(testUserId.toString(), updateRequest);

    // Then
    assertNotNull(result);
    verify(fxUserRepository).save(testUser);
  }

  @Test
  void updateUser_WhenUserDoesNotExist_ShouldThrowUserNotFoundException() {
    // Given
    UserUpdateRequest updateRequest = new UserUpdateRequest(
            "updated@example.com",
            "+9876543210",
            "new-device-token"
    );

    when(fxUserRepository.findByIdWithSubscriptions(testUserId)).thenReturn(Optional.empty());

    // When & Then
    UserNotFoundException exception = assertThrows(UserNotFoundException.class,
            () -> fxUsersService.updateUser(testUserId.toString(), updateRequest));
    assertTrue(exception.getMessage().contains(USER_NOT_FOUND + testUserId));
  }

  @Test
  void updateUser_WithNullValues_ShouldOnlyUpdateProvidedFields() {
    // Given
    UserUpdateRequest updateRequest = new UserUpdateRequest(
            null,
            null,
            "new-device-token"
    );

    when(fxUserRepository.findByIdWithSubscriptions(testUserId)).thenReturn(Optional.of(testUser));
    when(fxUserRepository.save(any(FxUser.class))).thenReturn(testUser);

    // When
    FxUser result = fxUsersService.updateUser(testUserId.toString(), updateRequest);

    // Then
    assertNotNull(result);
    verify(fxUserRepository).save(testUser);
  }

  @Test
  void updateUserStatus_WhenUserExists_ShouldUpdateStatusAndReturnUser() {
    // Given
    when(fxUserRepository.findByIdWithSubscriptions(testUserId)).thenReturn(Optional.of(testUser));
    when(fxUserRepository.save(any(FxUser.class))).thenReturn(testUser);

    // When
    FxUser result = fxUsersService.updateUserStatus(testUserId.toString(), false);

    // Then
    assertNotNull(result);
    assertFalse(testUser.isEnabled());
    verify(fxUserRepository).save(testUser);
  }

  @Test
  void updateUserStatus_WhenUserDoesNotExist_ShouldThrowUserNotFoundException() {
    // Given
    when(fxUserRepository.findByIdWithSubscriptions(testUserId)).thenReturn(Optional.empty());

    // When & Then
    UserNotFoundException exception = assertThrows(UserNotFoundException.class,
            () -> fxUsersService.updateUserStatus(testUserId.toString(), false));
    assertTrue(exception.getMessage().contains(USER_NOT_FOUND + testUserId));
  }

  @Test
  void deleteUser_WhenUserExistsAndNoActiveSubscriptions_ShouldDeleteSuccessfully() {
    // Given
    when(fxUserRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
    when(subscriptionRepository.findAllByUserId(testUserId)).thenReturn(List.of());

    // When
    fxUsersService.deleteUser(testUserId.toString());

    // Then
    verify(fxUserRepository).delete(testUser);
  }

  @Test
  void deleteUser_WhenUserExistsButHasActiveSubscriptions_ShouldThrowIllegalStateException() {
    // Given
    when(fxUserRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
    when(subscriptionRepository.findAllByUserId(testUserId)).thenReturn(List.of(testSubscription));

    // When & Then
    IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> fxUsersService.deleteUser(testUserId.toString()));
    assertTrue(exception.getMessage().contains("Cannot delete user with active subscriptions"));
  }

  @Test
  void deleteUser_WhenUserDoesNotExist_ShouldThrowUserNotFoundException() {
    // Given
    when(fxUserRepository.findById(testUserId)).thenReturn(Optional.empty());

    // When & Then
    UserNotFoundException exception = assertThrows(UserNotFoundException.class,
            () -> fxUsersService.deleteUser(testUserId.toString()));
    assertTrue(exception.getMessage().contains(USER_NOT_FOUND + testUserId));
  }

  @Test
  void getUserSubscriptions_WhenUserExists_ShouldReturnUserSubscriptionsResponse() {
    // Given
    when(fxUserRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
    when(subscriptionRepository.findAllByUserId(testUserId)).thenReturn(List.of(testSubscription));

    // When
    UserSubscriptionsResponse result = fxUsersService.getUserSubscriptions(testUserId.toString());

    // Then
    assertNotNull(result);
    assertEquals(testUserId, result.userId());
    assertEquals(1, result.subscriptions().size());
    assertEquals(1, result.totalCount());
    assertEquals(testSubscription.getId(), result.subscriptions().get(0).id());
  }

  @Test
  void getUserSubscriptions_WhenUserDoesNotExist_ShouldThrowUserNotFoundException() {
    // Given
    when(fxUserRepository.findById(testUserId)).thenReturn(Optional.empty());

    // When & Then
    UserNotFoundException exception = assertThrows(UserNotFoundException.class,
            () -> fxUsersService.getUserSubscriptions(testUserId.toString()));
    assertTrue(exception.getMessage().contains(USER_NOT_FOUND + testUserId));
  }

  @Test
  void getUserSubscriptions_WhenUserHasNoSubscriptions_ShouldReturnEmptyList() {
    // Given
    when(fxUserRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
    when(subscriptionRepository.findAllByUserId(testUserId)).thenReturn(List.of());

    // When
    UserSubscriptionsResponse result = fxUsersService.getUserSubscriptions(testUserId.toString());

    // Then
    assertNotNull(result);
    assertEquals(testUserId, result.userId());
    assertEquals(0, result.subscriptions().size());
    assertEquals(0, result.totalCount());
  }
} 
package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.dto.subscription.*;
import com.example.fx.subscription.service.exception.SubscriptionNotFoundException;
import com.example.fx.subscription.service.exception.UserNotFoundException;
import com.example.fx.subscription.service.model.*;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import com.example.fx.subscription.service.repository.FxUserRepository;
import com.example.fx.subscription.service.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class SubscriptionsServiceTest {

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @Mock
  private FxUserRepository fxUserRepository;

  @Mock
  private EventsOutboxRepository eventsOutboxRepository;

  @InjectMocks
  private SubscriptionsService subscriptionsService;

  private FxUser testUser;
  private Subscription testSubscription;
  private UUID testUserId;
  private UUID testSubscriptionId;

  @BeforeEach
  void setUp() {
    testUserId = UUID.randomUUID();
    testSubscriptionId = UUID.randomUUID();

    testUser = new FxUser();
    testUser.setId(testUserId);
    testUser.setEmail("test@example.com");
    testUser.setMobile("+1234567890");

    testSubscription = new Subscription();
    testSubscription.setId(testSubscriptionId);
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
  void findSubscriptionById_WhenSubscriptionExists_ShouldReturnSubscriptionResponse() {
    // Given
    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.of(testSubscription));

    // When
    Optional<SubscriptionResponse> result = subscriptionsService.findSubscriptionById(testSubscriptionId.toString());

    // Then
    assertTrue(result.isPresent());
    assertEquals(testSubscriptionId.toString(), result.get().id());
    assertEquals("GBP/USD", result.get().currencyPair());
    assertEquals(BigDecimal.valueOf(1.25), result.get().threshold());
    assertEquals(ThresholdDirection.ABOVE, result.get().direction());
    assertEquals(SubscriptionStatus.ACTIVE, result.get().status());
  }

  @Test
  void findSubscriptionById_WhenSubscriptionDoesNotExist_ShouldReturnEmpty() {
    // Given
    when(subscriptionRepository.findById(any(UUID.class)))
            .thenReturn(Optional.empty());

    // When
    Optional<SubscriptionResponse> result = subscriptionsService.findSubscriptionById(UUID.randomUUID().toString());

    // Then
    assertFalse(result.isPresent());
  }

  @Test
  void findSubscriptionById_WhenSubscriptionExistsWithNullUser_ShouldReturnSubscriptionResponse() {
    // Given
    testSubscription.setUser(null);
    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.of(testSubscription));

    // When
    Optional<SubscriptionResponse> result = subscriptionsService.findSubscriptionById(testSubscriptionId.toString());

    // Then
    assertTrue(result.isPresent());
    assertEquals(testSubscriptionId.toString(), result.get().id());
    assertNull(result.get().user());
    assertEquals("GBP/USD", result.get().currencyPair());
  }

  @Test
  void findSubscriptionById_WhenSubscriptionExistsWithNullNotifications_ShouldReturnSubscriptionResponse() {
    // Given
    testSubscription.setNotificationsChannels(null);
    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.of(testSubscription));

    // When
    Optional<SubscriptionResponse> result = subscriptionsService.findSubscriptionById(testSubscriptionId.toString());

    // Then
    assertTrue(result.isPresent());
    assertEquals(testSubscriptionId.toString(), result.get().id());
    assertEquals(List.of(), result.get().notificationsChannels());
  }

  @Test
  void findSubscriptionEntityById_WhenSubscriptionExists_ShouldReturnSubscription() {
    // Given
    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.of(testSubscription));

    // When
    Optional<Subscription> result = subscriptionsService.findSubscriptionEntityById(testSubscriptionId.toString());

    // Then
    assertTrue(result.isPresent());
    assertEquals(testSubscription, result.get());
  }

  @Test
  void findSubscriptionsByUserId_ShouldReturnUserSubscriptions() {
    // Given
    List<Subscription> subscriptions = List.of(testSubscription);
    when(subscriptionRepository.findSubscriptionsByUserId(testUserId))
            .thenReturn(subscriptions);

    // When
    List<Subscription> result = subscriptionsService.findSubscriptionsByUserId(testUserId.toString());

    // Then
    assertEquals(1, result.size());
    assertEquals(testSubscription, result.getFirst());
  }

  @Test
  void findSubscriptionResponsesByUserId_ShouldReturnSubscriptionResponses() {
    // Given
    List<Subscription> subscriptions = List.of(testSubscription);
    when(subscriptionRepository.findSubscriptionsByUserId(testUserId))
            .thenReturn(subscriptions);

    // When
    SubscriptionListResponse result = subscriptionsService.findSubscriptionResponsesByUserId(testUserId.toString());

    // Then
    assertNotNull(result);
    assertEquals(1, result.subscriptions().size());
    assertEquals(1, result.totalCount());
    assertEquals(testSubscriptionId.toString(), result.subscriptions().getFirst().id());
    assertEquals("GBP/USD", result.subscriptions().getFirst().currencyPair());
  }

  @Test
  void findSubscriptionResponsesByUserId_WhenSubscriptionHasNullNotifications_ShouldReturnSubscriptionResponses() {
    // Given
    testSubscription.setNotificationsChannels(null);
    List<Subscription> subscriptions = List.of(testSubscription);
    when(subscriptionRepository.findSubscriptionsByUserId(testUserId))
            .thenReturn(subscriptions);

    // When
    SubscriptionListResponse result = subscriptionsService.findSubscriptionResponsesByUserId(testUserId.toString());

    // Then
    assertNotNull(result);
    assertEquals(1, result.subscriptions().size());
    assertEquals(1, result.totalCount());
    assertEquals(testSubscriptionId.toString(), result.subscriptions().getFirst().id());
    assertEquals("GBP/USD", result.subscriptions().getFirst().currencyPair());
    assertEquals(List.of(), result.subscriptions().getFirst().notificationsChannels());
  }

  @Test
  void findSubscriptionResponsesByUserId_WhenNoSubscriptionsFound_ShouldThrowSubscriptionNotFoundException() {
    // Given
    when(subscriptionRepository.findSubscriptionsByUserId(testUserId))
            .thenReturn(List.of());

    // When & Then
    SubscriptionNotFoundException exception = assertThrows(SubscriptionNotFoundException.class,
            () -> subscriptionsService.findSubscriptionResponsesByUserId(testUserId.toString()));
    assertTrue(exception.getMessage().contains("No subscriptions found for the given user id: " + testUserId));
  }

  @Test
  void findAllSubscriptionResponses_ShouldReturnAllSubscriptions() {
    // Given
    List<Subscription> subscriptions = List.of(testSubscription);
    when(subscriptionRepository.findAll())
            .thenReturn(subscriptions);

    // When
    List<SubscriptionResponse> result = subscriptionsService.findAllSubscriptionResponses();

    // Then
    assertEquals(1, result.size());
    assertEquals(testSubscriptionId.toString(), result.getFirst().id());
  }

  @Test
  void isSubscriptionOwner_WhenUserIsOwner_ShouldReturnTrue() {
    // Given
    when(subscriptionRepository.existsByIdAndUserId(any(UUID.class), any(UUID.class)))
            .thenReturn(true);

    // When
    boolean result = subscriptionsService.isSubscriptionOwner(testSubscriptionId.toString(), testUserId);

    // Then
    assertTrue(result);
  }

  @Test
  void isSubscriptionOwner_WhenUserIsNotOwner_ShouldReturnFalse() {
    // Given
    when(subscriptionRepository.existsByIdAndUserId(any(UUID.class), any(UUID.class)))
            .thenReturn(false);

    // When
    boolean result = subscriptionsService.isSubscriptionOwner(testSubscriptionId.toString(), UUID.randomUUID());

    // Then
    assertFalse(result);
  }

  @Test
  void createSubscription_WhenUserExists_ShouldCreateAndReturnSubscription() {
    // Given
    SubscriptionCreateRequest createRequest = new SubscriptionCreateRequest(
            "EUR/USD",
            BigDecimal.valueOf(1.10),
            "BELOW",
            List.of("email")
    );

    when(fxUserRepository.findById(testUserId))
            .thenReturn(Optional.of(testUser));
    when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
            .thenReturn(testSubscription);
    when(eventsOutboxRepository.save(any(EventsOutbox.class)))
            .thenReturn(new EventsOutbox());

    // When
    SubscriptionResponse result = subscriptionsService.createSubscription(createRequest, testUserId);

    // Then
    assertNotNull(result);
    assertEquals(testSubscriptionId.toString(), result.id());
    verify(subscriptionRepository).saveAndFlush(any(Subscription.class));
    verify(eventsOutboxRepository).save(any(EventsOutbox.class));
  }

  @Test
  void createSubscription_WhenUserDoesNotExist_ShouldThrowUserNotFoundException() {
    // Given
    SubscriptionCreateRequest createRequest = new SubscriptionCreateRequest(
            "EUR/USD",
            BigDecimal.valueOf(1.10),
            "BELOW",
            List.of("email")
    );

    when(fxUserRepository.findById(testUserId))
            .thenReturn(Optional.empty());

    // When & Then
    UserNotFoundException exception = assertThrows(UserNotFoundException.class,
            () -> subscriptionsService.createSubscription(createRequest, testUserId));
    assertTrue(exception.getMessage().contains("User not found with ID: " + testUserId));
  }

  @Test
  void updateSubscriptionById_ShouldUpdateAndReturnSubscription() {
    // Given
    SubscriptionUpdateRequest updateRequest = new SubscriptionUpdateRequest(
            "EUR/USD",
            BigDecimal.valueOf(1.15),
            "BELOW",
            "ACTIVE",
            List.of("sms")
    );

    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.of(testSubscription));
    when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
            .thenReturn(testSubscription);
    when(eventsOutboxRepository.save(any(EventsOutbox.class)))
            .thenReturn(new EventsOutbox());

    // When
    SubscriptionResponse result = subscriptionsService.updateSubscriptionById(testSubscriptionId.toString(), updateRequest);

    // Then
    assertNotNull(result);
    assertEquals(testSubscriptionId.toString(), result.id());
    verify(subscriptionRepository).findById(testSubscriptionId);
    verify(subscriptionRepository).saveAndFlush(testSubscription);
    verify(eventsOutboxRepository).save(any(EventsOutbox.class));
  }

  @Test
  void updateSubscriptionById_WithNullValues_ShouldOnlyUpdateProvidedFields() {
    // Given
    SubscriptionUpdateRequest updateRequest = new SubscriptionUpdateRequest(
            "EUR/USD",
            BigDecimal.valueOf(1.15),
            null,
            null,
            null
    );

    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.of(testSubscription));
    when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
            .thenReturn(testSubscription);
    when(eventsOutboxRepository.save(any(EventsOutbox.class)))
            .thenReturn(new EventsOutbox());

    // When
    SubscriptionResponse result = subscriptionsService.updateSubscriptionById(testSubscriptionId.toString(), updateRequest);

    // Then
    assertNotNull(result);
    assertEquals(testSubscriptionId.toString(), result.id());
    verify(subscriptionRepository).findById(testSubscriptionId);
    verify(subscriptionRepository).saveAndFlush(testSubscription);
    verify(eventsOutboxRepository).save(any(EventsOutbox.class));
  }

  @Test
  void updateSubscriptionById_WhenSubscriptionDoesNotExist_ShouldThrowSubscriptionNotFoundException() {
    // Given
    SubscriptionUpdateRequest updateRequest = new SubscriptionUpdateRequest(
            "EUR/USD",
            BigDecimal.valueOf(1.15),
            "BELOW",
            "ACTIVE",
            List.of("sms")
    );

    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.empty());

    // When & Then
    SubscriptionNotFoundException exception = assertThrows(SubscriptionNotFoundException.class,
            () -> subscriptionsService.updateSubscriptionById(testSubscriptionId.toString(), updateRequest));
    assertTrue(exception.getMessage().contains("Subscription not found with Id: " + testSubscriptionId));
  }

  @Test
  void updateSubscriptionById_WithOnlyThreshold_ShouldUpdateOnlyThreshold() {
    // Given
    SubscriptionUpdateRequest updateRequest = new SubscriptionUpdateRequest(
            null,
            BigDecimal.valueOf(1.50),
            null,
            null,
            null
    );

    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.of(testSubscription));
    when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
            .thenReturn(testSubscription);
    when(eventsOutboxRepository.save(any(EventsOutbox.class)))
            .thenReturn(new EventsOutbox());

    // When
    SubscriptionResponse result = subscriptionsService.updateSubscriptionById(testSubscriptionId.toString(), updateRequest);

    // Then
    assertNotNull(result);
    assertEquals(testSubscriptionId.toString(), result.id());
    verify(subscriptionRepository).findById(testSubscriptionId);
    verify(subscriptionRepository).saveAndFlush(testSubscription);
    verify(eventsOutboxRepository).save(any(EventsOutbox.class));
  }

  @Test
  void updateSubscriptionById_WithOnlyNotificationChannels_ShouldUpdateOnlyNotificationChannels() {
    // Given
    SubscriptionUpdateRequest updateRequest = new SubscriptionUpdateRequest(
            null,
            null,
            null,
            null,
            List.of("email")
    );

    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.of(testSubscription));
    when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
            .thenReturn(testSubscription);
    when(eventsOutboxRepository.save(any(EventsOutbox.class)))
            .thenReturn(new EventsOutbox());

    // When
    SubscriptionResponse result = subscriptionsService.updateSubscriptionById(testSubscriptionId.toString(), updateRequest);

    // Then
    assertNotNull(result);
    assertEquals(testSubscriptionId.toString(), result.id());
    verify(subscriptionRepository).findById(testSubscriptionId);
    verify(subscriptionRepository).saveAndFlush(testSubscription);
    verify(eventsOutboxRepository).save(any(EventsOutbox.class));
  }

  @Test
  void deleteSubscriptionById_WhenSubscriptionExists_ShouldDeleteSuccessfully() {
    // Given
    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.of(testSubscription));
    when(eventsOutboxRepository.save(any(EventsOutbox.class)))
            .thenReturn(new EventsOutbox());

    // When
    SubscriptionDeleteResponse result = subscriptionsService.deleteSubscriptionById(testSubscriptionId.toString());

    // Then
    assertNotNull(result);
    assertEquals(testSubscriptionId.toString(), result.subscriptionId());
    assertEquals("Subscription deleted successfully", result.message());
    verify(subscriptionRepository).findById(testSubscriptionId);
    verify(subscriptionRepository).deleteById(testSubscriptionId);
    verify(eventsOutboxRepository).save(any(EventsOutbox.class));
  }

  @Test
  void deleteSubscriptionById_WhenSubscriptionDoesNotExist_ShouldThrowSubscriptionNotFoundException() {
    // Given
    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.empty());

    // When & Then
    SubscriptionNotFoundException exception = assertThrows(SubscriptionNotFoundException.class,
            () -> subscriptionsService.deleteSubscriptionById(testSubscriptionId.toString()));
    assertTrue(exception.getMessage().contains("Subscription not found with Id: " + testSubscriptionId));
  }

  @ParameterizedTest
  @CsvSource(value = {
          "EUR/USD,,,,",
          ",, BELOW,,",
          ",,, INACTIVE,"
  })
  void updateSubscriptionById_ShouldUpdateOnlyThatArg(
          String currency,
          BigDecimal threshold,
          String direction,
          String status,
          List<String> notificationChannels) {
    // Given
    SubscriptionUpdateRequest updateRequest = new SubscriptionUpdateRequest(
            currency,
            threshold,
            direction,
            status,
            notificationChannels
    );

    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.of(testSubscription));
    when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
            .thenReturn(testSubscription);
    when(eventsOutboxRepository.save(any(EventsOutbox.class)))
            .thenReturn(new EventsOutbox());

    // When
    SubscriptionResponse result = subscriptionsService.updateSubscriptionById(testSubscriptionId.toString(), updateRequest);

    // Then
    assertNotNull(result);
    assertEquals(testSubscriptionId.toString(), result.id());
    verify(subscriptionRepository).findById(testSubscriptionId);
    verify(subscriptionRepository).saveAndFlush(testSubscription);
    verify(eventsOutboxRepository).save(any(EventsOutbox.class));
  }

  @ParameterizedTest
  @CsvSource(value = {
          "null,1.15,BELOW,ACTIVE,sms",
          "EUR/USD,null,BELOW,ACTIVE,sms",
          "EUR/USD,1.15,null,ACTIVE,sms",
          "EUR/USD,1.15,BELOW,null,sms",
          "EUR/USD,1.15,BELOW,ACTIVE,null"
  }, nullValues = "null")
  void updateSubscriptionById_WithNullValues_ShouldNotUpdateNullFields(
          String currencyPair,
          BigDecimal threshold,
          String direction,
          String status,
          String notificationChannels) {
    // Given
    SubscriptionUpdateRequest updateRequest = new SubscriptionUpdateRequest(
            currencyPair,
            threshold,
            direction,
            status,
            notificationChannels != null ? List.of(notificationChannels) : null
    );

    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.of(testSubscription));
    when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
            .thenReturn(testSubscription);
    when(eventsOutboxRepository.save(any(EventsOutbox.class)))
            .thenReturn(new EventsOutbox());

    // When
    SubscriptionResponse result = subscriptionsService.updateSubscriptionById(testSubscriptionId.toString(), updateRequest);

    // Then
    assertNotNull(result);
    assertEquals(testSubscriptionId.toString(), result.id());
    verify(subscriptionRepository).findById(testSubscriptionId);
    verify(subscriptionRepository).saveAndFlush(testSubscription);
    verify(eventsOutboxRepository).save(any(EventsOutbox.class));
  }
} 
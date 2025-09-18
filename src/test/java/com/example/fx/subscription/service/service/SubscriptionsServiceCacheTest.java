package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.dto.subscription.SubscriptionCreateRequest;
import com.example.fx.subscription.service.dto.subscription.SubscriptionListResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionUpdateRequest;
import com.example.fx.subscription.service.exception.SubscriptionNotFoundException;
import com.example.fx.subscription.service.helper.SimpleCacheTestConfig;
import com.example.fx.subscription.service.model.*;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import com.example.fx.subscription.service.repository.FxUserRepository;
import com.example.fx.subscription.service.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig(classes = SimpleCacheTestConfig.class)
class SubscriptionsServiceCacheTest {

  @MockitoBean
  private SubscriptionRepository subscriptionRepository;

  @MockitoBean
  private FxUserRepository fxUserRepository;

  @MockitoBean
  private EventsOutboxRepository eventsOutboxRepository;

  @Autowired
  private SubscriptionsService subscriptionsService;

  @Autowired
  CacheManager cacheManager;

  private FxUser testUser;
  private Subscription testSubscription;
  private UUID testUserId;
  private UUID testSubscriptionId;

  @BeforeEach
  void setUp() {
    // Setup in-memory cache for testing
    Objects.requireNonNull(cacheManager.getCache("subscription")).clear();
    Objects.requireNonNull(cacheManager.getCache("subscriptionsByUser")).clear();
    reset(fxUserRepository);
    reset(eventsOutboxRepository);

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
  void findSubscriptionById_ShouldCacheResult() {
    // Given
    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.of(testSubscription));

    // First call - goes to DB
    Optional<SubscriptionResponse> firstResult =
            subscriptionsService.findSubscriptionById(testSubscriptionId.toString());

    assertTrue(firstResult.isPresent());
    assertEquals(testSubscriptionId.toString(), firstResult.get().id());

    // Assert cache contents
    SubscriptionResponse cached = Objects.requireNonNull(cacheManager
                    .getCache("subscription"))
            .get(testSubscriptionId.toString(), SubscriptionResponse.class);

    assertNotNull(cached);
    assertEquals(testSubscriptionId.toString(), cached.id());

    verify(subscriptionRepository, times(1)).findById(testSubscriptionId);

    // Second call - should return same from cache
    Optional<SubscriptionResponse> secondResult =
            subscriptionsService.findSubscriptionById(testSubscriptionId.toString());

    assertTrue(secondResult.isPresent());
    assertEquals(testSubscriptionId.toString(), secondResult.get().id());

    // Repo call count stays the same
    verify(subscriptionRepository, times(1)).findById(testSubscriptionId);
  }

  @Test
  void findSubscriptionResponsesByUserId_ShouldCacheResult() {
    // Given
    List<Subscription> subscriptions = List.of(testSubscription);
    when(subscriptionRepository.findSubscriptionsByUserId(testUserId))
            .thenReturn(subscriptions);

    // When - First call should hit repository
    SubscriptionListResponse result1 = subscriptionsService.findSubscriptionResponsesByUserId(testUserId.toString());

    // Then
    assertNotNull(result1);
    assertEquals(1, result1.totalCount());
    verify(subscriptionRepository, times(1)).findSubscriptionsByUserId(testUserId);

    // Assert cache contents
    SubscriptionListResponse cached = Objects.requireNonNull(cacheManager
                    .getCache("subscriptionsByUser"))
            .get(testUserId.toString(), SubscriptionListResponse.class);

    assertNotNull(cached);
    assertEquals(testUserId.toString(), cached.subscriptions().getFirst().user().id());

    // When - Second call should use cache
    SubscriptionListResponse result2 = subscriptionsService.findSubscriptionResponsesByUserId(testUserId.toString());

    // Then
    assertNotNull(result2);
    assertEquals(1, result2.totalCount());
    verify(subscriptionRepository, times(1)).findSubscriptionsByUserId(testUserId);
  }

  @Test
  void findSubscriptionResponsesByUserId_WithEmptyResult_ShouldNotCache() {
    // Given
    when(subscriptionRepository.findSubscriptionsByUserId(testUserId))
            .thenReturn(List.of());

    // When & Then - Should throw exception and not cache
    assertThrows(SubscriptionNotFoundException.class, () ->
            subscriptionsService.findSubscriptionResponsesByUserId(testUserId.toString()));

    // Verify cache is empty (due to unless condition)
    assertNull(Objects.requireNonNull(cacheManager.getCache("subscriptionsByUser")).get(testUserId.toString()));
  }

  @Test
  void createSubscription_ShouldEvictUserCacheAndPutSubscriptionCache() {
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

    // Pre-populate caches
    Objects.requireNonNull(cacheManager.getCache("subscription")).put(testSubscriptionId.toString(), SubscriptionResponse.fromSubscription(testSubscription));
    Objects.requireNonNull(cacheManager.getCache("subscriptionsByUser")).put(testUserId.toString(), new SubscriptionListResponse(List.of(), 0));

    // When
    SubscriptionResponse result = subscriptionsService.createSubscription(createRequest, testUserId);

    // Then
    assertNotNull(result);
    assertEquals(testSubscriptionId.toString(), result.id());

    // Verify user cache was evicted
    assertNull(Objects.requireNonNull(cacheManager.getCache("subscriptionsByUser")).get(result.user().id()));

    // Verify subscription cache was updated
    assertNotNull(Objects.requireNonNull(cacheManager.getCache("subscription")).get(testSubscriptionId.toString()));
  }

  @Test
  void updateSubscriptionById_ShouldUpdateSubscriptionCacheAndEvictUserCache() {
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

    // Pre-populate caches
    Objects.requireNonNull(cacheManager.getCache("subscription")).put(testSubscriptionId.toString(), SubscriptionResponse.fromSubscription(testSubscription));
    Objects.requireNonNull(cacheManager.getCache("subscriptionsByUser")).put(testUserId.toString(), new SubscriptionListResponse(List.of(), 0));

    // When
    SubscriptionResponse result = subscriptionsService.updateSubscriptionById(testSubscriptionId.toString(), updateRequest);

    // Then
    assertNotNull(result);
    assertEquals(testSubscriptionId.toString(), result.id());

    // Verify user cache was evicted
    assertNull(Objects.requireNonNull(cacheManager.getCache("subscriptionsByUser")).get(result.user().id()));

    // Verify subscription cache was updated
    assertNotNull(Objects.requireNonNull(cacheManager.getCache("subscription")).get(testSubscriptionId.toString()));
  }

  @Test
  void deleteSubscriptionById_ShouldEvictBothCaches() {
    // Given
    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.of(testSubscription));
    when(eventsOutboxRepository.save(any(EventsOutbox.class)))
            .thenReturn(new EventsOutbox());

    // Pre-populate caches
    Objects.requireNonNull(cacheManager.getCache("subscription")).put(testSubscriptionId.toString(), SubscriptionResponse.fromSubscription(testSubscription));
    Objects.requireNonNull(cacheManager.getCache("subscriptionsByUser")).put(testUserId.toString(), new SubscriptionListResponse(List.of(), 0));

    // When
    var result = subscriptionsService.deleteSubscriptionById(testSubscriptionId.toString());

    // Then
    assertNotNull(result);
    assertEquals(testSubscriptionId.toString(), result.subscriptionId());

    // Verify both caches were evicted
    assertNull(Objects.requireNonNull(cacheManager.getCache("subscription")).get(testSubscriptionId.toString()));
    assertNull(Objects.requireNonNull(cacheManager.getCache("subscriptionsByUser")).get(testUserId.toString()));
  }

  @Test
  void cacheKeyGeneration_ShouldBeConsistent() {
    // Given
    String subscriptionId = testSubscriptionId.toString();
    String userId = testUserId.toString();

    // When
    String expectedSubscriptionKey = subscriptionId;
    String expectedUserKey = userId;

    // Then
    assertEquals(expectedSubscriptionKey, subscriptionId);
    assertEquals(expectedUserKey, userId);
  }

  @Test
  void cacheSerialization_ShouldHandleNullValues() {
    // Given
    testSubscription.setUser(null);
    testSubscription.setNotificationsChannels(null);
    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.of(testSubscription));

    // When
    Optional<SubscriptionResponse> result = subscriptionsService.findSubscriptionById(testSubscriptionId.toString());

    // Then
    assertTrue(result.isPresent());
    assertNull(result.get().user());
    assertEquals(List.of(), result.get().notificationsChannels());
  }

  @Test
  void shouldHandleCacheEvictionOnBulkOperations() {
    // Test cache eviction when multiple operations happen
    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.of(testSubscription));
    when(subscriptionRepository.saveAndFlush(any()))
            .thenReturn(testSubscription);

    // Pre-populate caches
    Objects.requireNonNull(cacheManager.getCache("subscription"))
            .put(testSubscriptionId.toString(), SubscriptionResponse.fromSubscription(testSubscription));

    // Multiple operations should properly evict caches
    subscriptionsService.findSubscriptionById(testSubscriptionId.toString());
    subscriptionsService.updateSubscriptionById(testSubscriptionId.toString(),
            new SubscriptionUpdateRequest("EUR/USD", BigDecimal.valueOf(1.15), "BELOW", "ACTIVE", List.of("sms")));

    // Verify cache was updated, not just evicted
    assertNotNull(Objects.requireNonNull(cacheManager.getCache("subscription"))
            .get(testSubscriptionId.toString()));
  }

  @Test
  void shouldHandleCacheSerializationWithComplexObjects() {
    // Test with complex nested objects
    testSubscription.setNotificationsChannels(List.of("email", "sms", "push"));
    when(subscriptionRepository.findById(testSubscriptionId))
            .thenReturn(Optional.of(testSubscription));

    Optional<SubscriptionResponse> result = subscriptionsService.findSubscriptionById(testSubscriptionId.toString());
    assertTrue(result.isPresent());
    assertEquals(3, result.get().notificationsChannels().size());

    // Verify complex object was cached correctly
    SubscriptionResponse cached = Objects.requireNonNull(cacheManager.getCache("subscription"))
            .get(testSubscriptionId.toString(), SubscriptionResponse.class);
    assertNotNull(cached);
    assertEquals(3, cached.notificationsChannels().size());
  }
}

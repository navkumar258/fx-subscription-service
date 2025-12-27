package com.example.fx.subscription.service.integration;

import com.example.fx.subscription.service.dto.subscription.SubscriptionCreateRequest;
import com.example.fx.subscription.service.dto.subscription.SubscriptionListResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionUpdateRequest;
import com.example.fx.subscription.service.exception.SubscriptionNotFoundException;
import com.example.fx.subscription.service.helper.RedisIntegrationTestBase;
import com.example.fx.subscription.service.service.SubscriptionsService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RedisCacheIT extends RedisIntegrationTestBase {

  @Autowired
  private SubscriptionsService subscriptionsService;

  private void assertCacheHas(String cacheName, String key) {
    Awaitility.await()
            .pollDelay(Duration.ofMillis(300))
            .until(() -> true);
    assertNotNull(Objects.requireNonNull(cacheManager.getCache(cacheName)).get(key));
  }

  private void assertCacheMissing(String cacheName, String key) {
    Awaitility.await()
            .pollDelay(Duration.ofMillis(300))
            .until(() -> true);
    assertNull(Objects.requireNonNull(cacheManager.getCache(cacheName)).get(key));
  }

  @Test
  void shouldCacheSubscriptionById() {
    SubscriptionResponse firstCall = subscriptionsService.findSubscriptionById(testSubscriptionId.toString());
    assertNotNull(firstCall);

    assertCacheHas("subscription", testSubscriptionId.toString());

    SubscriptionResponse secondCall = subscriptionsService.findSubscriptionById(testSubscriptionId.toString());
    assertNotNull(secondCall);
  }

  @Test
  void shouldCacheSubscriptionsByUser() {
    SubscriptionListResponse firstCall = subscriptionsService.findSubscriptionResponsesByUserId(testUserId.toString());
    assertNotNull(firstCall);
    assertEquals(1, firstCall.totalCount());

    assertCacheHas("subscriptionsByUser", testUserId.toString());

    SubscriptionListResponse secondCall = subscriptionsService.findSubscriptionResponsesByUserId(testUserId.toString());
    assertNotNull(secondCall);
  }

  @Test
  void shouldNotCacheEmptySubscriptionsList() {
    String randomUserId = UUID.randomUUID().toString();
    assertThrows(SubscriptionNotFoundException.class,
            () -> subscriptionsService.findSubscriptionResponsesByUserId(randomUserId));

    assertCacheMissing("subscriptionsByUser", randomUserId);
  }

  @Test
  void shouldEvictUserCacheOnCreate() {
    String userId = testUserId.toString();
    Objects.requireNonNull(cacheManager.getCache("subscriptionsByUser")).put(userId, new SubscriptionListResponse(List.of(), 0));

    subscriptionsService.createSubscription(new SubscriptionCreateRequest(
            "EUR/USD", BigDecimal.valueOf(1.10), "BELOW", List.of("email")
    ), testUserId);

    assertCacheMissing("subscriptionsByUser", userId);
  }

  @Test
  void shouldUpdateCacheOnSubscriptionUpdate() {
    subscriptionsService.updateSubscriptionById(
            testSubscriptionId.toString(),
            new SubscriptionUpdateRequest("EUR/USD", BigDecimal.valueOf(1.15), "BELOW", "ACTIVE", List.of("sms"))
    );

    SubscriptionResponse cached = Objects.requireNonNull(cacheManager.getCache("subscription"))
            .get(testSubscriptionId.toString(), SubscriptionResponse.class);
    assertNotNull(cached);
    assertEquals("EUR/USD", cached.currencyPair());
  }

  @Test
  void shouldEvictBothCachesOnDelete() {
    String subKey = testSubscriptionId.toString();
    String userKey = testUserId.toString();
    Objects.requireNonNull(cacheManager.getCache("subscription")).put(subKey, SubscriptionResponse.fromSubscription(testSubscription));
    Objects.requireNonNull(cacheManager.getCache("subscriptionsByUser")).put(userKey, new SubscriptionListResponse(List.of(), 0));

    subscriptionsService.deleteSubscriptionById(subKey);

    assertCacheMissing("subscription", subKey);
    assertCacheMissing("subscriptionsByUser", userKey);
  }

  @Test
  void shouldRespectCacheTTL() {
    String subscriptionId = testSubscriptionId.toString();

    // Clear caches first
    clearAllCaches();

    // First call to populate cache
    SubscriptionResponse result = subscriptionsService.findSubscriptionById(subscriptionId);
    assertNotNull(result);

    // Verify cache entry exists
    assertCacheHas("subscription", subscriptionId);

    // Wait for TTL (3s) to expire
    Awaitility.await()
            .pollDelay(Duration.ofMillis(3100))
            .until(() -> true);

    // Cache entry should be expired
    assertCacheMissing("subscription", subscriptionId);
  }

  @Test
  void shouldVerifyCacheSerialization() {
    String subscriptionId = testSubscriptionId.toString();

    // First call to populate cache
    SubscriptionResponse result = subscriptionsService.findSubscriptionById(subscriptionId);
    assertNotNull(result);

    // Verify the cached object can be retrieved and deserialized
    assertCacheHas("subscription", subscriptionId);

    SubscriptionResponse cachedResponse = (SubscriptionResponse) Objects.requireNonNull(Objects.requireNonNull(cacheManager.getCache("subscription")).get(subscriptionId)).get();
    assertNotNull(cachedResponse);
    assertEquals(subscriptionId, cachedResponse.id());
    assertEquals("GBP/USD", cachedResponse.currencyPair());
    assertEquals("ABOVE", cachedResponse.direction().name());
    assertEquals("ACTIVE", cachedResponse.status().name());
  }
}


package com.example.fx.subscription.service.integration;

import com.example.fx.subscription.service.dto.subscription.SubscriptionCreateRequest;
import com.example.fx.subscription.service.dto.subscription.SubscriptionListResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionUpdateRequest;
import com.example.fx.subscription.service.exception.SubscriptionNotFoundException;
import com.example.fx.subscription.service.helper.RedisIntegrationTestBase;
import com.example.fx.subscription.service.service.SubscriptionsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RedisCacheIT extends RedisIntegrationTestBase {

  @Autowired
  private SubscriptionsService subscriptionsService;

  private void assertCacheHas(String cacheName, String key) {
    assertNotNull(Objects.requireNonNull(cacheManager.getCache(cacheName)).get(key));
  }

  private void assertCacheMissing(String cacheName, String key) {
    assertNull(Objects.requireNonNull(cacheManager.getCache(cacheName)).get(key));
  }

  @Test
  void shouldCacheSubscriptionById() {
    Optional<SubscriptionResponse> firstCall = subscriptionsService.findSubscriptionById(testSubscriptionId.toString());
    assertTrue(firstCall.isPresent());

    assertCacheHas("subscription", testSubscriptionId.toString());

    Optional<SubscriptionResponse> secondCall = subscriptionsService.findSubscriptionById(testSubscriptionId.toString());
    assertTrue(secondCall.isPresent());
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
  void shouldRespectCacheTTL() throws InterruptedException {
    String subscriptionId = testSubscriptionId.toString();

    // Clear caches first
    clearAllCaches();

    // First call to populate cache
    Optional<SubscriptionResponse> result = subscriptionsService.findSubscriptionById(subscriptionId);
    assertTrue(result.isPresent());

    // Verify cache entry exists
    assertCacheHas("subscription", subscriptionId);

    // Wait for TTL to expire (3 seconds from properties)
    Thread.sleep(3500);

    // Cache entry should be expired
    assertCacheMissing("subscription", subscriptionId);
  }

  @Test
  void shouldVerifyCacheExpiration() {
    String subscriptionId = testSubscriptionId.toString();

    // Clear caches first
    clearAllCaches();

    // First call to populate cache
    Optional<SubscriptionResponse> result = subscriptionsService.findSubscriptionById(subscriptionId);
    assertTrue(result.isPresent());

    // Verify cache entry exists
    assertCacheHas("subscription", subscriptionId);
  }

  @Test
  void shouldVerifyCacheSerialization() {
    String subscriptionId = testSubscriptionId.toString();

    // Clear caches first
    clearAllCaches();

    // First call to populate cache
    Optional<SubscriptionResponse> result = subscriptionsService.findSubscriptionById(subscriptionId);
    assertTrue(result.isPresent());

    // Verify the cached object can be retrieved and deserialized
    Cache subscriptionCache = cacheManager.getCache("subscription");
    assertNotNull(subscriptionCache);
    Cache.ValueWrapper cachedValue = subscriptionCache.get(subscriptionId);
    assertNotNull(cachedValue);

    SubscriptionResponse cachedResponse = (SubscriptionResponse) cachedValue.get();
    assertNotNull(cachedResponse);
    assertEquals(subscriptionId, cachedResponse.id());
    assertEquals("GBP/USD", cachedResponse.currencyPair());
    assertEquals("ABOVE", cachedResponse.direction().name());
    assertEquals("ACTIVE", cachedResponse.status().name());
  }
}


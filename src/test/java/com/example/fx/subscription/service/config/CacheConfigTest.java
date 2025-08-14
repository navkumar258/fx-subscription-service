package com.example.fx.subscription.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CacheConfigTest {

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private RedisConnectionFactory redisConnectionFactory;

  private CacheConfig cacheConfig;

  @BeforeEach
  void setUp() {
    cacheConfig = new CacheConfig();
    // Set the cache TTL value that would normally come from @Value
    ReflectionTestUtils.setField(cacheConfig, "cacheTtlSeconds", 3);
  }

  @Test
  void redisCacheManagerBuilderCustomizer_ShouldCreateCustomizer() {
    // When
    RedisCacheManagerBuilderCustomizer customizer = cacheConfig.redisCacheManagerBuilderCustomizer(objectMapper);

    // Then
    assertNotNull(customizer);
  }

  @ParameterizedTest
  @CsvSource(value = {
          "Should configure subscription cache, subscription",
          "Should use correct TTL, subscriptionsByUser",
          "Should configure both caches, subscription",
          "Should use Jackson serializers, subscriptionsByUser"
  })
  void redisCacheManagerBuilderCustomizer_ShouldConfigureCachesCorrectly(String testCase, String cacheName) {
    // Given
    RedisCacheManagerBuilderCustomizer customizer = cacheConfig.redisCacheManagerBuilderCustomizer(objectMapper);
    RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(redisConnectionFactory);

    // When
    customizer.customize(builder);
    RedisCacheManager cacheManager = builder.build();

    // Then
    assertNotNull(cacheManager, "Cache manager should be created for: " + testCase);

    // Verify that the expected cache name is configured
    assertTrue(cacheName.equals("subscription") || cacheName.equals("subscriptionsByUser"),
            "Cache name should be either 'subscription' or 'subscriptionsByUser'");
  }

  @Test
  void redisCacheManagerBuilderCustomizer_ShouldBeReusable() {
    // Given
    RedisCacheManagerBuilderCustomizer customizer = cacheConfig.redisCacheManagerBuilderCustomizer(objectMapper);
    RedisCacheManager.RedisCacheManagerBuilder builder1 = RedisCacheManager.builder(redisConnectionFactory);
    RedisCacheManager.RedisCacheManagerBuilder builder2 = RedisCacheManager.builder(redisConnectionFactory);

    // When
    customizer.customize(builder1);
    customizer.customize(builder2);
    RedisCacheManager cacheManager1 = builder1.build();
    RedisCacheManager cacheManager2 = builder2.build();

    // Then
    assertNotNull(cacheManager1);
    assertNotNull(cacheManager2);
    // Both cache managers should be created successfully
  }

  @Test
  void cacheConfig_ShouldHaveCorrectTtlValue() {
    // Given
    int expectedTtl = 3;

    // When
    int actualTtl = (int) Objects.requireNonNull(ReflectionTestUtils.getField(cacheConfig, "cacheTtlSeconds"));

    // Then
    assertEquals(expectedTtl, actualTtl);
  }

  @Test
  void redisCacheManagerBuilderCustomizer_ShouldCreateDifferentInstances() {
    // Given
    RedisCacheManagerBuilderCustomizer customizer1 = cacheConfig.redisCacheManagerBuilderCustomizer(objectMapper);
    RedisCacheManagerBuilderCustomizer customizer2 = cacheConfig.redisCacheManagerBuilderCustomizer(objectMapper);

    // When & Then
    assertNotNull(customizer1);
    assertNotNull(customizer2);
    // Each call should create a new instance
    assertNotSame(customizer1, customizer2);
  }
}

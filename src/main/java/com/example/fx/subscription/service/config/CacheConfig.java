package com.example.fx.subscription.service.config;

import com.example.fx.subscription.service.dto.subscription.SubscriptionListResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
public class CacheConfig {

  private final int cacheTtlSeconds;

  public CacheConfig(
          @Value(value = "${spring.cache.redis.time-to-live-seconds}") int cacheTtlSeconds
  ) {
    this.cacheTtlSeconds = cacheTtlSeconds;
  }

  @Bean
  public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
          ObjectMapper objectMapper
  ) {
    return builder -> {
      Jackson2JsonRedisSerializer<SubscriptionListResponse> subListSerializer =
              new Jackson2JsonRedisSerializer<>(objectMapper, SubscriptionListResponse.class);
      builder.withCacheConfiguration("subscriptionsByUser",
              RedisCacheConfiguration.defaultCacheConfig()
                      .serializeValuesWith(
                              RedisSerializationContext.SerializationPair.fromSerializer(subListSerializer)
                      )
                      .entryTtl(Duration.ofSeconds(cacheTtlSeconds))
      );

      Jackson2JsonRedisSerializer<SubscriptionResponse> subSerializer =
              new Jackson2JsonRedisSerializer<>(objectMapper, SubscriptionResponse.class);
      builder.withCacheConfiguration("subscription",
              RedisCacheConfiguration.defaultCacheConfig()
                      .serializeValuesWith(
                              RedisSerializationContext.SerializationPair.fromSerializer(subSerializer)
                      )
                      .entryTtl(Duration.ofSeconds(cacheTtlSeconds))
      );
    };
  }
}

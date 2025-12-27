package com.example.fx.subscription.service.config;

import com.example.fx.subscription.service.dto.subscription.SubscriptionListResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

  private final int cacheTtlSeconds;

  public CacheConfig(
          @Value(value = "${spring.cache.redis.time-to-live-seconds}") int cacheTtlSeconds
  ) {
    this.cacheTtlSeconds = cacheTtlSeconds;
  }

  @Bean
  public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
  ) {
    return builder -> {
      JacksonJsonRedisSerializer<SubscriptionListResponse> subListSerializer =
              new JacksonJsonRedisSerializer<>(SubscriptionListResponse.class);
      builder.withCacheConfiguration("subscriptionsByUser",
              RedisCacheConfiguration.defaultCacheConfig()
                      .serializeValuesWith(
                              RedisSerializationContext.SerializationPair.fromSerializer(subListSerializer)
                      )
                      .entryTtl(Duration.ofSeconds(cacheTtlSeconds))
                      .disableCachingNullValues()
      );

      JacksonJsonRedisSerializer<SubscriptionResponse> subSerializer =
              new JacksonJsonRedisSerializer<>(SubscriptionResponse.class);
      builder.withCacheConfiguration("subscription",
              RedisCacheConfiguration.defaultCacheConfig()
                      .serializeValuesWith(
                              RedisSerializationContext.SerializationPair.fromSerializer(subSerializer)
                      )
                      .entryTtl(Duration.ofSeconds(cacheTtlSeconds))
                      .disableCachingNullValues()
      );
    };
  }
}

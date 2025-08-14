package com.example.fx.subscription.service.helper;

import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import com.example.fx.subscription.service.repository.FxUserRepository;
import com.example.fx.subscription.service.repository.SubscriptionRepository;
import com.example.fx.subscription.service.service.SubscriptionsService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;

@TestConfiguration
@EnableCaching
public class SimpleCacheTestConfig {

  @Bean
  CacheManager cacheManager() {
    return new ConcurrentMapCacheManager("subscription", "subscriptionsByUser");
  }

  @Bean
  SubscriptionsService subscriptionService(
          SubscriptionRepository subscriptionRepository,
          FxUserRepository fxUserRepository,
          EventsOutboxRepository eventsOutboxRepository) {
    return new SubscriptionsService(subscriptionRepository, fxUserRepository, eventsOutboxRepository);
  }

}

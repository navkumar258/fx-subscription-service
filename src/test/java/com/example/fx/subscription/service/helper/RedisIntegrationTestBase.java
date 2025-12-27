package com.example.fx.subscription.service.helper;

import com.example.fx.subscription.service.model.*;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import com.example.fx.subscription.service.repository.FxUserRepository;
import com.example.fx.subscription.service.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("redis-test")
@Import(RedisTestContainerConfig.class)
public abstract class RedisIntegrationTestBase {

  @Autowired
  protected CacheManager cacheManager;

  @Autowired
  protected SubscriptionRepository subscriptionRepository;

  @Autowired
  protected FxUserRepository fxUserRepository;

  @Autowired
  protected EventsOutboxRepository eventsOutboxRepository;

  // Mock external dependencies to speed up tests
  @MockitoBean
  KafkaAdmin kafkaAdmin;

  @MockitoBean
  KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate;

  protected FxUser testUser;
  protected Subscription testSubscription;
  protected UUID testUserId;
  protected UUID testSubscriptionId;

  @BeforeEach
  void setUp() {
    clearAllCaches();
    clearAllData();
    seedDefaultTestData();
  }

  protected void clearAllCaches() {
    cacheManager.getCacheNames().forEach(name ->
            Objects.requireNonNull(cacheManager.getCache(name)).clear()
    );
  }

  protected void clearAllData() {
    eventsOutboxRepository.deleteAll();
    subscriptionRepository.deleteAll();
    fxUserRepository.deleteAll();
  }

  protected void seedDefaultTestData() {
    testUser = new FxUser();
    testUser.setEmail("test@example.com");
    testUser.setPassword("password");
    testUser.setMobile("+1234567890");
    testUser.setEnabled(true);
    testUser.setRole(UserRole.USER);

    testUser = fxUserRepository.save(testUser);
    testUserId = testUser.getId();

    testSubscription = new Subscription();
    testSubscription.setUser(testUser);
    testSubscription.setCurrencyPair("GBP/USD");
    testSubscription.setThreshold(BigDecimal.valueOf(1.25));
    testSubscription.setDirection(ThresholdDirection.ABOVE);
    testSubscription.setNotificationsChannels(List.of("email", "sms"));
    testSubscription.setStatus(SubscriptionStatus.ACTIVE);

    testSubscription = subscriptionRepository.save(testSubscription);
    testSubscriptionId = testSubscription.getId();
  }
}


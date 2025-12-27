package com.example.fx.subscription.service.repository;

import com.example.fx.subscription.service.ai.tool.FxSubscriptionTool;
import com.example.fx.subscription.service.helper.PostgresTestContainerConfig;
import com.example.fx.subscription.service.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(PostgresTestContainerConfig.class)
class SubscriptionRepositoryTest {

  @MockitoBean
  private FxSubscriptionTool fxSubscriptionTool;

  @Autowired
  private SubscriptionRepository subscriptionRepository;

  @Autowired
  private FxUserRepository fxUserRepository;

  private FxUser testUser1;
  private FxUser testUser2;
  private Subscription testSubscription1;
  private Subscription testSubscription2;
  private Subscription testSubscription3;

  @BeforeEach
  void setUp() {
    // Create test users
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    testUser1 = new FxUser();
    testUser1.setEmail("user1@example.com");
    testUser1.setMobile("+1234567890");
    testUser1.setPassword(passwordEncoder.encode("password123"));
    testUser1.setEnabled(true);
    testUser1.setRole(UserRole.USER);
    testUser1 = fxUserRepository.save(testUser1);

    testUser2 = new FxUser();
    testUser2.setEmail("user2@example.com");
    testUser2.setMobile("+0987654321");
    testUser2.setPassword(passwordEncoder.encode("password456"));
    testUser2.setEnabled(true);
    testUser2.setRole(UserRole.USER);
    testUser2 = fxUserRepository.save(testUser2);

    // Create test subscriptions
    testSubscription1 = new Subscription();
    testSubscription1.setUser(testUser1);
    testSubscription1.setCurrencyPair("GBP/USD");
    testSubscription1.setThreshold(BigDecimal.valueOf(1.25));
    testSubscription1.setDirection(ThresholdDirection.ABOVE);
    testSubscription1.setNotificationsChannels(List.of("email", "sms"));
    testSubscription1.setStatus(SubscriptionStatus.ACTIVE);

    testSubscription2 = new Subscription();
    testSubscription2.setUser(testUser1);
    testSubscription2.setCurrencyPair("EUR/USD");
    testSubscription2.setThreshold(BigDecimal.valueOf(1.10));
    testSubscription2.setDirection(ThresholdDirection.BELOW);
    testSubscription2.setNotificationsChannels(List.of("email"));
    testSubscription2.setStatus(SubscriptionStatus.ACTIVE);

    testSubscription3 = new Subscription();
    testSubscription3.setUser(testUser2);
    testSubscription3.setCurrencyPair("USD/JPY");
    testSubscription3.setThreshold(BigDecimal.valueOf(150.0));
    testSubscription3.setDirection(ThresholdDirection.ABOVE);
    testSubscription3.setNotificationsChannels(List.of("sms"));
    testSubscription3.setStatus(SubscriptionStatus.INACTIVE);
  }

  @AfterEach
  void tearDown() {
    subscriptionRepository.deleteAll();
    fxUserRepository.deleteAll();
  }

  @Test
  void save_ShouldPersistSubscription() {
    // When
    Subscription savedSubscription = subscriptionRepository.save(testSubscription1);

    // Then
    assertNotNull(savedSubscription.getId());
    assertEquals("GBP/USD", savedSubscription.getCurrencyPair());
    assertEquals(BigDecimal.valueOf(1.25), savedSubscription.getThreshold());
    assertEquals(ThresholdDirection.ABOVE, savedSubscription.getDirection());
    assertEquals(SubscriptionStatus.ACTIVE, savedSubscription.getStatus());
    assertEquals(testUser1.getId(), savedSubscription.getUser().getId());

    // Verify it's in the database
    Optional<Subscription> foundSubscription = subscriptionRepository.findById(savedSubscription.getId());
    assertTrue(foundSubscription.isPresent());
    assertEquals("GBP/USD", foundSubscription.get().getCurrencyPair());
  }

  @Test
  void findById_WhenExists_ShouldReturnSubscription() {
    // Given
    Subscription savedSubscription = subscriptionRepository.save(testSubscription1);

    // When
    Optional<Subscription> foundSubscription = subscriptionRepository.findById(savedSubscription.getId());

    // Then
    assertTrue(foundSubscription.isPresent());
    assertEquals(savedSubscription.getId(), foundSubscription.get().getId());
    assertEquals("GBP/USD", foundSubscription.get().getCurrencyPair());
    assertEquals(testUser1.getId(), foundSubscription.get().getUser().getId());
  }

  @Test
  void findById_WhenNotExists_ShouldReturnEmpty() {
    // When
    Optional<Subscription> foundSubscription = subscriptionRepository.findById(UUID.randomUUID());

    // Then
    assertFalse(foundSubscription.isPresent());
  }

  @Test
  void findAll_ShouldReturnAllSubscriptions() {
    // Given
    subscriptionRepository.save(testSubscription1);
    subscriptionRepository.save(testSubscription2);
    subscriptionRepository.save(testSubscription3);

    // When
    List<Subscription> allSubscriptions = subscriptionRepository.findAll();

    // Then
    assertEquals(3, allSubscriptions.size());
  }

  @Test
  void findSubscriptionsByUserId_ShouldReturnUserSubscriptions() {
    // Given
    subscriptionRepository.save(testSubscription1);
    subscriptionRepository.save(testSubscription2);
    subscriptionRepository.save(testSubscription3);

    // When
    List<Subscription> user1Subscriptions = subscriptionRepository.findSubscriptionsByUserId(testUser1.getId());
    List<Subscription> user2Subscriptions = subscriptionRepository.findSubscriptionsByUserId(testUser2.getId());

    // Then
    assertEquals(2, user1Subscriptions.size());
    assertEquals(1, user2Subscriptions.size());

    // Verify user1 subscriptions
    assertTrue(user1Subscriptions.stream()
            .allMatch(sub -> sub.getUser().getId().equals(testUser1.getId())));
    assertTrue(user1Subscriptions.stream()
            .anyMatch(sub -> sub.getCurrencyPair().equals("GBP/USD")));
    assertTrue(user1Subscriptions.stream()
            .anyMatch(sub -> sub.getCurrencyPair().equals("EUR/USD")));

    // Verify user2 subscription
    assertTrue(user2Subscriptions.stream()
            .allMatch(sub -> sub.getUser().getId().equals(testUser2.getId())));
    assertTrue(user2Subscriptions.stream()
            .anyMatch(sub -> sub.getCurrencyPair().equals("USD/JPY")));
  }

  @Test
  void findSubscriptionsByUserId_WhenUserHasNoSubscriptions_ShouldReturnEmptyList() {
    // Given
    FxUser newUser = new FxUser();
    newUser.setEmail("newuser@example.com");
    newUser.setMobile("+1111111111");
    newUser.setPassword("password");
    newUser.setEnabled(true);
    newUser.setRole(UserRole.USER);
    newUser = fxUserRepository.save(newUser);

    // When
    List<Subscription> userSubscriptions = subscriptionRepository.findSubscriptionsByUserId(newUser.getId());

    // Then
    assertTrue(userSubscriptions.isEmpty());
  }

  @Test
  void existsByIdAndUserId_WhenSubscriptionExistsForUser_ShouldReturnTrue() {
    // Given
    Subscription savedSubscription = subscriptionRepository.save(testSubscription1);

    // When
    boolean exists = subscriptionRepository.existsByIdAndUserId(savedSubscription.getId(), testUser1.getId());

    // Then
    assertTrue(exists);
  }

  @Test
  void existsByIdAndUserId_WhenSubscriptionExistsButNotForUser_ShouldReturnFalse() {
    // Given
    Subscription savedSubscription = subscriptionRepository.save(testSubscription1);

    // When
    boolean exists = subscriptionRepository.existsByIdAndUserId(savedSubscription.getId(), testUser2.getId());

    // Then
    assertFalse(exists);
  }

  @Test
  void existsByIdAndUserId_WhenSubscriptionDoesNotExist_ShouldReturnFalse() {
    // When
    boolean exists = subscriptionRepository.existsByIdAndUserId(UUID.randomUUID(), testUser1.getId());

    // Then
    assertFalse(exists);
  }

  @Test
  void deleteById_ShouldRemoveSubscription() {
    // Given
    Subscription savedSubscription = subscriptionRepository.save(testSubscription1);
    assertTrue(subscriptionRepository.findById(savedSubscription.getId()).isPresent());

    // When
    subscriptionRepository.deleteById(savedSubscription.getId());

    // Then
    assertFalse(subscriptionRepository.findById(savedSubscription.getId()).isPresent());
  }

  @Test
  void save_ShouldUpdateExistingSubscription() {
    // Given
    Subscription savedSubscription = subscriptionRepository.save(testSubscription1);

    // When
    savedSubscription.setThreshold(BigDecimal.valueOf(1.50));
    savedSubscription.setCurrencyPair("USD/EUR");
    Subscription updatedSubscription = subscriptionRepository.save(savedSubscription);

    // Then
    assertEquals(savedSubscription.getId(), updatedSubscription.getId());
    assertEquals(BigDecimal.valueOf(1.50), updatedSubscription.getThreshold());
    assertEquals("USD/EUR", updatedSubscription.getCurrencyPair());

    // Verify in database
    Optional<Subscription> foundSubscription = subscriptionRepository.findById(savedSubscription.getId());
    assertTrue(foundSubscription.isPresent());
    assertEquals(BigDecimal.valueOf(1.50), foundSubscription.get().getThreshold());
  }

  @Test
  void save_ShouldHandleNullNotificationsChannels() {
    // Given
    testSubscription1.setNotificationsChannels(null);

    // When
    Subscription savedSubscription = subscriptionRepository.save(testSubscription1);

    // Then
    assertNotNull(savedSubscription.getNotificationsChannels());
    assertTrue(savedSubscription.getNotificationsChannels().isEmpty());
  }

  @Test
  void save_ShouldHandleEmptyNotificationsChannels() {
    // Given
    testSubscription1.setNotificationsChannels(List.of());

    // When
    Subscription savedSubscription = subscriptionRepository.save(testSubscription1);

    // Then
    assertNotNull(savedSubscription.getNotificationsChannels());
    assertTrue(savedSubscription.getNotificationsChannels().isEmpty());
  }

  @Test
  void count_ShouldReturnCorrectNumberOfSubscriptions() {
    // Given
    subscriptionRepository.save(testSubscription1);
    subscriptionRepository.save(testSubscription2);
    subscriptionRepository.save(testSubscription3);

    // When
    long count = subscriptionRepository.count();

    // Then
    assertEquals(3, count);
  }

  @Test
  void findById_ShouldReturnSubscriptionWithUserRelationship() {
    // Given
    Subscription savedSubscription = subscriptionRepository.save(testSubscription1);

    // When
    Optional<Subscription> foundSubscription = subscriptionRepository.findById(savedSubscription.getId());

    // Then
    assertTrue(foundSubscription.isPresent());
    assertNotNull(foundSubscription.get().getUser());
    assertEquals(testUser1.getId(), foundSubscription.get().getUser().getId());
    assertEquals("user1@example.com", foundSubscription.get().getUser().getEmail());
  }
}

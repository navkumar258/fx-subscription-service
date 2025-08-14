package com.example.fx.subscription.service.repository;

import com.example.fx.subscription.service.ai.tool.FxSubscriptionTool;
import com.example.fx.subscription.service.helper.PostgresTestContainerConfig;
import com.example.fx.subscription.service.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(PostgresTestContainerConfig.class)
class FxUserRepositoryTest {

  @MockitoBean
  private FxSubscriptionTool fxSubscriptionTool;

  @Autowired
  private FxUserRepository fxUserRepository;

  @Autowired
  private SubscriptionRepository subscriptionRepository;

  private FxUser testUser1;
  private FxUser testUser2;
  private FxUser testUser3;
  private Subscription testSubscription1;
  private Subscription testSubscription2;

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

    testUser2 = new FxUser();
    testUser2.setEmail("user2@example.com");
    testUser2.setMobile("+0987654321");
    testUser2.setPassword(passwordEncoder.encode("password456"));
    testUser2.setEnabled(false);
    testUser2.setRole(UserRole.ADMIN);

    testUser3 = new FxUser();
    testUser3.setEmail("admin@example.com");
    testUser3.setMobile("+5555555555");
    testUser3.setPassword(passwordEncoder.encode("adminpass"));
    testUser3.setEnabled(true);
    testUser3.setRole(UserRole.ADMIN);

    // Save users
    testUser1 = fxUserRepository.save(testUser1);
    testUser2 = fxUserRepository.save(testUser2);
    testUser3 = fxUserRepository.save(testUser3);

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

    // Save subscriptions
    subscriptionRepository.save(testSubscription1);
    subscriptionRepository.save(testSubscription2);

    testUser1.getSubscriptions().add(testSubscription1);
    testUser1.getSubscriptions().add(testSubscription2);
    fxUserRepository.save(testUser1);
  }

  @AfterEach
  void tearDown() {
    subscriptionRepository.deleteAll();
    fxUserRepository.deleteAll();
  }

  @Test
  void save_ShouldPersistFxUser() {
    // Given
    FxUser newUser = new FxUser();
    newUser.setEmail("newuser@example.com");
    newUser.setMobile("+1111111111");
    newUser.setPassword("password");
    newUser.setEnabled(true);
    newUser.setRole(UserRole.USER);

    // When
    FxUser savedUser = fxUserRepository.save(newUser);

    // Then
    assertNotNull(savedUser.getId());
    assertEquals("newuser@example.com", savedUser.getEmail());
    assertEquals("+1111111111", savedUser.getMobile());
    assertTrue(savedUser.isEnabled());
    assertEquals(UserRole.USER, savedUser.getRole());

    // Verify it's in the database
    Optional<FxUser> foundUser = fxUserRepository.findById(savedUser.getId());
    assertTrue(foundUser.isPresent());
    assertEquals("newuser@example.com", foundUser.get().getEmail());
  }

  @Test
  void findById_WhenExists_ShouldReturnFxUser() {
    // When
    Optional<FxUser> foundUser = fxUserRepository.findById(testUser1.getId());

    // Then
    assertTrue(foundUser.isPresent());
    assertEquals(testUser1.getId(), foundUser.get().getId());
    assertEquals("user1@example.com", foundUser.get().getEmail());
    assertEquals("+1234567890", foundUser.get().getMobile());
  }

  @Test
  void findById_WhenNotExists_ShouldReturnEmpty() {
    // When
    Optional<FxUser> foundUser = fxUserRepository.findById(UUID.randomUUID());

    // Then
    assertFalse(foundUser.isPresent());
  }

  @Test
  void findAll_ShouldReturnAllUsers() {
    // When
    List<FxUser> allUsers = fxUserRepository.findAll();

    // Then
    assertEquals(3, allUsers.size());
    assertTrue(allUsers.stream().anyMatch(user -> user.getEmail().equals("user1@example.com")));
    assertTrue(allUsers.stream().anyMatch(user -> user.getEmail().equals("user2@example.com")));
    assertTrue(allUsers.stream().anyMatch(user -> user.getEmail().equals("admin@example.com")));
  }

  @Test
  void findByEmail_WhenEmailExists_ShouldReturnUser() {
    // When
    Optional<FxUser> foundUser = fxUserRepository.findByEmail("user1@example.com");

    // Then
    assertTrue(foundUser.isPresent());
    assertEquals(testUser1.getId(), foundUser.get().getId());
    assertEquals("user1@example.com", foundUser.get().getEmail());
  }

  @ParameterizedTest
  @CsvSource(value = {
          "nonexistent@example.com, Email does not exist",
          "null, Email is null",
          "USER1@EXAMPLE.COM, Email is case sensitive"
  }, nullValues = "null")
  void findByEmail_WhenEmailDoesNotMatch_ShouldReturnEmpty(String email, String testCase) {
    // When
    Optional<FxUser> foundUser = fxUserRepository.findByEmail(email);

    // Then
    assertFalse(foundUser.isPresent(), "Should return empty for: " + testCase);
  }

  @Test
  void existsByEmail_WhenEmailExists_ShouldReturnTrue() {
    // When
    boolean exists = fxUserRepository.existsByEmail("user1@example.com");

    // Then
    assertTrue(exists);
  }

  @Test
  void existsByEmail_WhenEmailDoesNotExist_ShouldReturnFalse() {
    // When
    boolean exists = fxUserRepository.existsByEmail("nonexistent@example.com");

    // Then
    assertFalse(exists);
  }

  @Test
  void existsByEmail_WhenEmailIsNull_ShouldReturnFalse() {
    // When
    boolean exists = fxUserRepository.existsByEmail(null);

    // Then
    assertFalse(exists);
  }

  @Test
  void findByIdWithSubscriptions_WhenUserExistsWithSubscriptions_ShouldReturnUserWithSubscriptions() {
    // When
    Optional<FxUser> foundUser = fxUserRepository.findByIdWithSubscriptions(testUser1.getId());

    // Then
    assertTrue(foundUser.isPresent());
    assertEquals(testUser1.getId(), foundUser.get().getId());
    assertEquals("user1@example.com", foundUser.get().getEmail());

    // Verify subscriptions are loaded
    assertNotNull(foundUser.get().getSubscriptions());
    assertEquals(2, foundUser.get().getSubscriptions().size());

    // Verify subscription details
    Set<String> currencyPairs = foundUser.get().getSubscriptions().stream()
            .map(Subscription::getCurrencyPair)
            .collect(Collectors.toSet());

    assertTrue(currencyPairs.contains("GBP/USD"));
    assertTrue(currencyPairs.contains("EUR/USD"));
  }

  @Test
  void findByIdWithSubscriptions_WhenUserExistsWithoutSubscriptions_ShouldReturnUserWithEmptySubscriptions() {
    // When
    Optional<FxUser> foundUser = fxUserRepository.findByIdWithSubscriptions(testUser2.getId());

    // Then
    assertTrue(foundUser.isPresent());
    assertEquals(testUser2.getId(), foundUser.get().getId());
    assertEquals("user2@example.com", foundUser.get().getEmail());

    // Verify subscriptions collection is empty
    assertNotNull(foundUser.get().getSubscriptions());
    assertEquals(0, foundUser.get().getSubscriptions().size());
  }

  @Test
  void findByIdWithSubscriptions_WhenUserDoesNotExist_ShouldReturnEmpty() {
    // When
    Optional<FxUser> foundUser = fxUserRepository.findByIdWithSubscriptions(UUID.randomUUID());

    // Then
    assertFalse(foundUser.isPresent());
  }

  @Test
  void searchUsers_WhenNoFilters_ShouldReturnAllUsers() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<FxUser> result = fxUserRepository.searchUsers(null, null, null, pageable);

    // Then
    assertEquals(3, result.getTotalElements());
    assertEquals(3, result.getContent().size());
    assertEquals(1, result.getTotalPages());
  }

  @Test
  void searchUsers_WhenFilteringByEmail_ShouldReturnMatchingUsers() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<FxUser> result = fxUserRepository.searchUsers("user1", null, null, pageable);

    // Then
    assertEquals(1, result.getTotalElements());
    assertEquals(1, result.getContent().size());
    assertEquals("user1@example.com", result.getContent().getFirst().getEmail());
  }

  @Test
  void searchUsers_WhenFilteringByMobile_ShouldReturnMatchingUsers() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<FxUser> result = fxUserRepository.searchUsers(null, "123456", null, pageable);

    // Then
    assertEquals(1, result.getTotalElements());
    assertEquals(1, result.getContent().size());
    assertEquals("+1234567890", result.getContent().getFirst().getMobile());
  }

  @Test
  void searchUsers_WhenFilteringByEnabled_ShouldReturnMatchingUsers() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<FxUser> result = fxUserRepository.searchUsers(null, null, true, pageable);

    // Then
    assertEquals(2, result.getTotalElements());
    assertEquals(2, result.getContent().size());
    assertTrue(result.getContent().stream().allMatch(FxUser::isEnabled));
  }

  @Test
  void searchUsers_WhenFilteringByDisabled_ShouldReturnMatchingUsers() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<FxUser> result = fxUserRepository.searchUsers(null, null, false, pageable);

    // Then
    assertEquals(1, result.getTotalElements());
    assertEquals(1, result.getContent().size());
    assertFalse(result.getContent().getFirst().isEnabled());
  }

  @Test
  void searchUsers_WhenCombiningFilters_ShouldReturnMatchingUsers() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<FxUser> result = fxUserRepository.searchUsers("admin", null, true, pageable);

    // Then
    assertEquals(1, result.getTotalElements());
    assertEquals(1, result.getContent().size());
    assertEquals("admin@example.com", result.getContent().getFirst().getEmail());
    assertTrue(result.getContent().getFirst().isEnabled());
  }

  @Test
  void searchUsers_WhenNoMatchingFilters_ShouldReturnEmptyPage() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<FxUser> result = fxUserRepository.searchUsers("nonexistent", null, null, pageable);

    // Then
    assertEquals(0, result.getTotalElements());
    assertEquals(0, result.getContent().size());
  }

  @Test
  void searchUsers_WithPagination_ShouldReturnCorrectPage() {
    // Given
    Pageable pageable = PageRequest.of(0, 2); // First page with 2 items

    // When
    Page<FxUser> result = fxUserRepository.searchUsers(null, null, null, pageable);

    // Then
    assertEquals(3, result.getTotalElements());
    assertEquals(2, result.getContent().size());
    assertEquals(2, result.getTotalPages());
    assertTrue(result.hasNext());
  }

  @Test
  void searchUsers_WithSecondPage_ShouldReturnCorrectPage() {
    // Given
    Pageable pageable = PageRequest.of(1, 2); // Second page with 2 items

    // When
    Page<FxUser> result = fxUserRepository.searchUsers(null, null, null, pageable);

    // Then
    assertEquals(3, result.getTotalElements());
    assertEquals(1, result.getContent().size());
    assertEquals(2, result.getTotalPages());
    assertFalse(result.hasNext());
  }

  @Test
  void deleteById_ShouldRemoveUser() {
    // Given
    assertTrue(fxUserRepository.findById(testUser1.getId()).isPresent());

    // When
    fxUserRepository.deleteById(testUser1.getId());

    // Then
    assertFalse(fxUserRepository.findById(testUser1.getId()).isPresent());
  }

  @Test
  void save_ShouldUpdateExistingUser() {
    // Given
    String newEmail = "updated@example.com";

    // When
    testUser1.setEmail(newEmail);
    testUser1.setEnabled(false);
    FxUser updatedUser = fxUserRepository.save(testUser1);

    // Then
    assertEquals(testUser1.getId(), updatedUser.getId());
    assertEquals(newEmail, updatedUser.getEmail());
    assertFalse(updatedUser.isEnabled());

    // Verify in database
    Optional<FxUser> foundUser = fxUserRepository.findById(testUser1.getId());
    assertTrue(foundUser.isPresent());
    assertEquals(newEmail, foundUser.get().getEmail());
  }

  @Test
  void count_ShouldReturnCorrectNumberOfUsers() {
    // When
    long count = fxUserRepository.count();

    // Then
    assertEquals(3, count);
  }

  @Test
  void searchUsers_WithPartialEmailMatch_ShouldReturnMatchingUsers() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<FxUser> result = fxUserRepository.searchUsers("user", null, null, pageable);

    // Then
    assertEquals(2, result.getTotalElements());
    assertEquals(2, result.getContent().size());
    assertTrue(result.getContent().stream()
            .allMatch(user -> user.getEmail().contains("user")));
  }

  @Test
  void searchUsers_WithPartialMobileMatch_ShouldReturnMatchingUsers() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<FxUser> result = fxUserRepository.searchUsers(null, "123", null, pageable);

    // Then
    assertEquals(1, result.getTotalElements());
    assertEquals(1, result.getContent().size());
    assertTrue(result.getContent().getFirst().getMobile().contains("123"));
  }
}

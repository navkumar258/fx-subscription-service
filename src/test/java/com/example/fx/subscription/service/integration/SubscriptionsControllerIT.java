package com.example.fx.subscription.service.integration;

import com.example.fx.subscription.service.dto.subscription.SubscriptionCreateRequest;
import com.example.fx.subscription.service.dto.subscription.SubscriptionCreateResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionUpdateRequest;
import com.example.fx.subscription.service.helper.PostgresTestContainersConfig;
import com.example.fx.subscription.service.helper.WebSecurityTestConfig;
import com.example.fx.subscription.service.model.FxUser;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.model.UserRole;
import com.example.fx.subscription.service.repository.FxUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestContainersConfig.class, WebSecurityTestConfig.class})
class SubscriptionsControllerIT {

  @Autowired
  MockMvcTester mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  FxUserRepository fxUserRepository;

  @Autowired
  PasswordEncoder passwordEncoder;

  // Mock external dependencies to speed up tests
  @MockitoBean
  KafkaAdmin kafkaAdmin;

  @MockitoBean
  KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate;

  @Value("${security.jwt.token.secret-key}")
  private String jwtSecretString;

  private SecretKey testSecretKey;

  @BeforeEach
  void setUp() {
    // Cleanup database before each test
    fxUserRepository.deleteAll();

    // Setup JWT key
    testSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretString));
  }

  @Test
  void completeSubscriptionLifecycle_ShouldWorkEndToEnd() throws Exception {
    // Create admin user FIRST, then generate JWT
    createTestUser("admin@example.com", UserRole.ADMIN);
    String adminJwt = generateTestJwtToken("admin@example.com");

    // 1. Create user FIRST, then generate JWT
    createTestUser("lifecycle_test@mail.com", UserRole.USER);
    String userJwt = generateTestJwtToken("lifecycle_test@mail.com");

    // 2. Create subscription
    String subscriptionId = createSubscription(userJwt, BigDecimal.valueOf(1.25));

    // 3. Verify subscription was created and persisted
    verifySubscriptionExists(userJwt, subscriptionId, "GBP/USD", BigDecimal.valueOf(1.25));

    // 4. Update subscription
    updateSubscription(userJwt, subscriptionId, BigDecimal.valueOf(1.15));

    // 5. Verify subscription was updated
    verifySubscriptionExists(userJwt, subscriptionId, "EUR/USD", BigDecimal.valueOf(1.15));

    // 6. Get user's subscriptions (verify it's in the list)
    verifyUserHasSubscription(userJwt);

    // 7. Delete subscription
    deleteSubscription(userJwt, subscriptionId);

    // 8. Verify subscription is deleted
    verifySubscriptionDeleted(adminJwt, subscriptionId);
  }

  @Test
  void authenticationFlow_ShouldHandleInvalidTokens() throws Exception {
    // Test invalid JWT token
    SubscriptionCreateRequest createRequest = new SubscriptionCreateRequest(
            "GBP/USD", BigDecimal.valueOf(1.20), "ABOVE", List.of("sms", "email"));

    assertThat(mockMvc.post()
            .uri("/api/v1/subscriptions")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(createRequest))
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
            .secure(true))
            .hasStatus(HttpStatus.UNAUTHORIZED);

    // Test missing JWT token
    assertThat(mockMvc.post()
            .uri("/api/v1/subscriptions")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(createRequest))
            .secure(true))
            .hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void authorizationFlow_ShouldEnforceAccessControl() throws Exception {
    // Create two users FIRST, then generate JWTs
    createTestUser("user1@example.com", UserRole.USER);
    createTestUser("user2@example.com", UserRole.USER);

    String user1Jwt = generateTestJwtToken("user1@example.com");
    String user2Jwt = generateTestJwtToken("user2@example.com");

    // User1 creates a subscription
    String subscriptionId = createSubscription(user1Jwt, BigDecimal.valueOf(1.25));

    // User2 tries to access User1's subscription - should be forbidden
    assertThat(mockMvc.get()
            .uri("/api/v1/subscriptions/" + subscriptionId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + user2Jwt)
            .secure(true))
            .hasStatus(HttpStatus.FORBIDDEN);

    // User2 tries to update User1's subscription - should be forbidden
    SubscriptionUpdateRequest updateRequest = new SubscriptionUpdateRequest(
            "EUR/USD", BigDecimal.valueOf(1.15), "BELOW", "ACTIVE", List.of("email"));

    assertThat(mockMvc.put()
            .uri("/api/v1/subscriptions/" + subscriptionId)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(updateRequest))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + user2Jwt)
            .secure(true))
            .hasStatus(HttpStatus.FORBIDDEN);

    // User2 tries to delete User1's subscription - should be forbidden
    assertThat(mockMvc.delete()
            .uri("/api/v1/subscriptions/" + subscriptionId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + user2Jwt)
            .secure(true))
            .hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void adminAuthorizationFlow_ShouldAllowAdminAccess() throws Exception {
    // Create admin user FIRST, then generate JWT
    createTestUser("admin@example.com", UserRole.ADMIN);
    String adminJwt = generateTestJwtToken("admin@example.com");

    // Create regular user and subscription
    createTestUser("user@example.com", UserRole.USER);
    String userJwt = generateTestJwtToken("user@example.com");

    String subscriptionId = createSubscription(userJwt, BigDecimal.valueOf(1.25));

    // Admin should be able to access all subscriptions
    assertThat(mockMvc.get()
            .uri("/api/v1/subscriptions/all")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.subscriptions", subscriptionsAssert ->
                    subscriptionsAssert.assertThat().asArray().hasSizeGreaterThanOrEqualTo(1));

    // Admin should be able to access any individual subscription
    assertThat(mockMvc.get()
            .uri("/api/v1/subscriptions/" + subscriptionId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK);

    // Regular user should NOT be able to access all subscriptions
    assertThat(mockMvc.get()
            .uri("/api/v1/subscriptions/all")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
            .secure(true))
            .hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void dataPersistenceFlow_ShouldPersistToDatabase() throws Exception {
    // Create user FIRST, then generate JWT
    createTestUser("persistence_test@mail.com", UserRole.USER);
    String userJwt = generateTestJwtToken("persistence_test@mail.com");

    // Create subscription
    String subscriptionId = createSubscription(userJwt, BigDecimal.valueOf(1.25));

    // Verify subscription exists in user's subscription list
    assertThat(mockMvc.get()
            .uri("/api/v1/subscriptions/my")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.subscriptions", subscriptionsAssert ->
                    subscriptionsAssert.assertThat().asArray().hasSize(1))
            .hasPathSatisfying("$.subscriptions[0].id", idAssert ->
                    idAssert.assertThat().isEqualTo(subscriptionId))
            .hasPathSatisfying("$.subscriptions[0].currencyPair", currencyPairAssert ->
                    currencyPairAssert.assertThat().isEqualTo("GBP/USD"))
            .hasPathSatisfying("$.subscriptions[0].threshold", thresholdAssert ->
                    thresholdAssert.assertThat().asNumber().isEqualTo(1.25));
  }

  // Helper methods for creating test data
  private void createTestUser(String email, UserRole role) {
    FxUser user = new FxUser();
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode("Test_Password"));
    user.setMobile("+447911123456");
    user.setRole(role);
    user.setEnabled(true);
    fxUserRepository.save(user);
  }

  private String generateTestJwtToken(String username) {
    Instant now = Instant.now();
    return Jwts.builder()
            .subject(username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
            .signWith(testSecretKey)
            .compact();
  }

  // Helper methods for subscription operations
  private String createSubscription(String jwt, BigDecimal threshold) throws Exception {
    SubscriptionCreateRequest createRequest = new SubscriptionCreateRequest(
            "GBP/USD", threshold, "ABOVE", List.of("email", "sms"));

    MockHttpServletResponse response = mockMvc.post()
            .uri("/api/v1/subscriptions")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(createRequest))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
            .secure(true)
            .exchange()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED.value());

    SubscriptionCreateResponse createResponse = objectMapper.readValue(
            response.getContentAsString(), SubscriptionCreateResponse.class);

    assertThat(createResponse.subscription().currencyPair()).isEqualTo("GBP/USD");
    assertThat(createResponse.subscription().threshold()).isEqualTo(threshold);

    return createResponse.subscriptionId().toString();
  }

  private void verifySubscriptionExists(String jwt, String subscriptionId, String expectedCurrencyPair, BigDecimal expectedThreshold) {
    assertThat(mockMvc.get()
            .uri("/api/v1/subscriptions/" + subscriptionId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
            .secure(true))
            .hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.currencyPair", currencyPairAssert ->
                    currencyPairAssert.assertThat().isEqualTo(expectedCurrencyPair))
            .hasPathSatisfying("$.threshold", thresholdAssert ->
                    thresholdAssert.assertThat().asNumber().isEqualTo(expectedThreshold.doubleValue()));
  }

  private void updateSubscription(String jwt, String subscriptionId, BigDecimal threshold) throws Exception {
    SubscriptionUpdateRequest updateRequest = new SubscriptionUpdateRequest(
            "EUR/USD", threshold, "BELOW", "ACTIVE", List.of("email"));

    assertThat(mockMvc.put()
            .uri("/api/v1/subscriptions/" + subscriptionId)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(updateRequest))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
            .secure(true))
            .hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.subscription.currencyPair", currencyPairAssert ->
                    currencyPairAssert.assertThat().isEqualTo("EUR/USD"))
            .hasPathSatisfying("$.subscription.threshold", thresholdAssert ->
                    thresholdAssert.assertThat().asNumber().isEqualTo(threshold.doubleValue()));
  }

  private void verifyUserHasSubscription(String jwt) {
    assertThat(mockMvc.get()
            .uri("/api/v1/subscriptions/my")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
            .secure(true))
            .hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.subscriptions", subscriptionsAssert ->
                    subscriptionsAssert.assertThat().asArray().hasSizeGreaterThanOrEqualTo(1))
            .hasPathSatisfying("$.totalCount", totalCountAssert ->
                    totalCountAssert.assertThat().asNumber().isEqualTo(1));
  }

  private void deleteSubscription(String jwt, String subscriptionId) {
    assertThat(mockMvc.delete()
            .uri("/api/v1/subscriptions/" + subscriptionId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
            .secure(true))
            .hasStatus(HttpStatus.NO_CONTENT);
  }

  private void verifySubscriptionDeleted(String jwt, String subscriptionId) {
    assertThat(mockMvc.get()
            .uri("/api/v1/subscriptions/" + subscriptionId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
            .secure(true))
            .hasStatus(HttpStatus.NOT_FOUND);
  }
}

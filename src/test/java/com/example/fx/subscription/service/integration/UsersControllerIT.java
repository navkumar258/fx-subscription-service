package com.example.fx.subscription.service.integration;

import com.example.fx.subscription.service.dto.subscription.SubscriptionCreateRequest;
import com.example.fx.subscription.service.dto.user.UserUpdateRequest;
import com.example.fx.subscription.service.helper.PostgresTestContainersConfig;
import com.example.fx.subscription.service.helper.WebSecurityTestConfig;
import com.example.fx.subscription.service.model.FxUser;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.model.UserRole;
import com.example.fx.subscription.service.repository.FxUserRepository;
import com.example.fx.subscription.service.repository.SubscriptionRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestContainersConfig.class, WebSecurityTestConfig.class})
class UsersControllerIT {

  @Autowired
  MockMvcTester mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  FxUserRepository fxUserRepository;

  @Autowired
  SubscriptionRepository subscriptionRepository;

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
    subscriptionRepository.deleteAll();
    fxUserRepository.deleteAll();

    // Setup JWT key
    testSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretString));
  }

  @Test
  void completeUserLifecycle_ShouldWorkEndToEnd() throws Exception {
    // 1. Create admin user
    FxUser admin = createTestUser("admin@example.com", UserRole.ADMIN);
    String adminJwt = generateTestJwtToken("admin@example.com");

    // 2. Create regular user
    FxUser user = createTestUser("user@example.com", UserRole.USER);
    String userJwt = generateTestJwtToken("user@example.com");

    // 3. Admin can view all users
    assertThat(mockMvc.get()
            .uri("/api/v1/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.users", usersAssert ->
                    usersAssert.assertThat().asArray().hasSize(2))
            .hasPathSatisfying("$.totalElements", totalAssert ->
                    totalAssert.assertThat().asNumber().isEqualTo(2))
            .hasPathSatisfying("$.users[0].email", emailAssert ->
                    emailAssert.assertThat().isEqualTo("user@example.com"))
            .hasPathSatisfying("$.users[1].email", emailAssert ->
                    emailAssert.assertThat().isEqualTo("admin@example.com"));

    // 4. User can view their own profile
    assertThat(mockMvc.get()
            .uri("/api/v1/users/" + user.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.id", idAssert ->
                    idAssert.assertThat().isEqualTo(user.getId().toString()))
            .hasPathSatisfying("$.email", emailAssert ->
                    emailAssert.assertThat().isEqualTo("user@example.com"));

    // 5. User cannot view other user's profile
    assertThat(mockMvc.get()
            .uri("/api/v1/users/" + admin.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
            .secure(true))
            .hasStatus(HttpStatus.FORBIDDEN);

    // 6. Admin can update user
    UserUpdateRequest updateRequest = new UserUpdateRequest(
            "updated@example.com", "+1234567890", "new-push-token"
    );

    assertThat(mockMvc.put()
            .uri("/api/v1/users/" + user.getId())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(updateRequest))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.user.email", emailAssert ->
                    emailAssert.assertThat().isEqualTo("updated@example.com"))
            .hasPathSatisfying("$.user.mobile", mobileAssert ->
                    mobileAssert.assertThat().isEqualTo("+1234567890"))
            .hasPathSatisfying("$.user.pushDeviceToken", tokenAssert ->
                    tokenAssert.assertThat().isEqualTo("new-push-token"));

    // 7. Verify user was updated in database
    FxUser updatedUser = fxUserRepository.findById(user.getId()).orElseThrow();
    assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
    assertThat(updatedUser.getMobile()).isEqualTo("+1234567890");
    assertThat(updatedUser.getPushDeviceToken()).isEqualTo("new-push-token");

    // 8. Admin can delete user
    assertThat(mockMvc.delete()
            .uri("/api/v1/users/" + user.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.NO_CONTENT);

    // 9. Verify user is deleted
    assertThat(mockMvc.get()
            .uri("/api/v1/users/" + user.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.NOT_FOUND);

    // 10. Verify user is actually deleted from database
    assertThat(fxUserRepository.findById(user.getId())).isEmpty();
  }

  @Test
  void authorizationFlow_ShouldEnforceAccessControl() {
    // Create users
    FxUser admin = createTestUser("admin@example.com", UserRole.ADMIN);
    FxUser user1 = createTestUser("user1@example.com", UserRole.USER);
    FxUser user2 = createTestUser("user2@example.com", UserRole.USER);

    String adminJwt = generateTestJwtToken("admin@example.com");
    String user1Jwt = generateTestJwtToken("user1@example.com");
    String user2Jwt = generateTestJwtToken("user2@example.com");

    // 1. Only admin can access /api/v1/users (get all users)
    assertThat(mockMvc.get()
            .uri("/api/v1/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK);

    assertThat(mockMvc.get()
            .uri("/api/v1/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Jwt)
            .secure(true))
            .hasStatus(HttpStatus.FORBIDDEN);

    // 2. Users can only access their own profile
    assertThat(mockMvc.get()
            .uri("/api/v1/users/" + user1.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Jwt)
            .secure(true))
            .hasStatus(HttpStatus.OK);

    assertThat(mockMvc.get()
            .uri("/api/v1/users/" + user2.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Jwt)
            .secure(true))
            .hasStatus(HttpStatus.FORBIDDEN);

    // 3. Admin can access any user's profile
    assertThat(mockMvc.get()
            .uri("/api/v1/users/" + user1.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK);

    assertThat(mockMvc.get()
            .uri("/api/v1/users/" + user2.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK);

    // 4. Only admin can delete users
    assertThat(mockMvc.delete()
            .uri("/api/v1/users/" + user1.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Jwt)
            .secure(true))
            .hasStatus(HttpStatus.FORBIDDEN);

    assertThat(mockMvc.delete()
            .uri("/api/v1/users/" + user1.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.NO_CONTENT);

    // 5. Only admin can search users
    assertThat(mockMvc.get()
            .uri("/api/v1/users/search?email=user")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + user2Jwt)
            .secure(true))
            .hasStatus(HttpStatus.FORBIDDEN);

    assertThat(mockMvc.get()
            .uri("/api/v1/users/search?email=user")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK);
  }

  @Test
  void userSearch_ShouldWorkEndToEnd() {
    // Create test users
    FxUser admin = createTestUser("admin@example.com", UserRole.ADMIN);
    FxUser user1 = createTestUser("bob@example.com", UserRole.USER);
    FxUser user2 = createTestUser("jane@example.com", UserRole.USER);
    FxUser user3 = createTestUser("john@test.com", UserRole.USER);

    String adminJwt = generateTestJwtToken("admin@example.com");

    // 1. Search by email
    assertThat(mockMvc.get()
            .uri("/api/v1/users/search?email=john")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.users", usersAssert ->
                    usersAssert.assertThat().asArray().hasSize(1))
            .hasPathSatisfying("$.totalElements", totalAssert ->
                    totalAssert.assertThat().asNumber().isEqualTo(1))
            .hasPathSatisfying("$.users[0].email", emailAssert ->
                    emailAssert.assertThat().isEqualTo("john@test.com"));

    // 2. Search by mobile
    assertThat(mockMvc.get()
            .uri("/api/v1/users/search?mobile=+1234567890")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.totalElements", totalAssert ->
                    totalAssert.assertThat().asNumber().isEqualTo(4));

    // 3. Search with enabled filter
    assertThat(mockMvc.get()
            .uri("/api/v1/users/search?enabled=true")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.totalElements", totalAssert ->
                    totalAssert.assertThat().asNumber().isEqualTo(4));

    // 4. Search with pagination
    assertThat(mockMvc.get()
            .uri("/api/v1/users/search?size=2&page=0")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.users", usersAssert ->
                    usersAssert.assertThat().asArray().hasSize(2))
            .hasPathSatisfying("$.totalElements", totalAssert ->
                    totalAssert.assertThat().asNumber().isEqualTo(4))
            .hasPathSatisfying("$.totalPages", pagesAssert ->
                    pagesAssert.assertThat().asNumber().isEqualTo(2));

    // 5. Search with no results
    assertThat(mockMvc.get()
            .uri("/api/v1/users/search?email=nonexistent")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.totalElements", totalAssert ->
                    totalAssert.assertThat().asNumber().isEqualTo(0))
            .hasPathSatisfying("$.users", usersAssert ->
                    usersAssert.assertThat().asArray().hasSize(0));
  }

  @Test
  void userSubscriptions_ShouldReturnUserSubscriptions() throws Exception {
    // Create users
    FxUser admin = createTestUser("admin@example.com", UserRole.ADMIN);
    FxUser user = createTestUser("user@example.com", UserRole.USER);

    String adminJwt = generateTestJwtToken("admin@example.com");
    String userJwt = generateTestJwtToken("user@example.com");

    // Create subscription for user
    SubscriptionCreateRequest subscriptionRequest = new SubscriptionCreateRequest(
            "EUR/USD", BigDecimal.valueOf(1.15), "ABOVE", List.of("EMAIL", "SMS")
    );

    MockHttpServletResponse response = mockMvc.post()
            .uri("/api/v1/subscriptions")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(subscriptionRequest))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
            .secure(true)
            .exchange()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED.value());

    // 1. User can view their own subscriptions
    assertThat(mockMvc.get()
            .uri("/api/v1/users/" + user.getId() + "/subscriptions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.userId", userIdAssert ->
                    userIdAssert.assertThat().isEqualTo(user.getId().toString()))
            .hasPathSatisfying("$.totalCount", countAssert ->
                    countAssert.assertThat().asNumber().isEqualTo(1))
            .hasPathSatisfying("$.subscriptions[0].currencyPair", currencyAssert ->
                    currencyAssert.assertThat().isEqualTo("EUR/USD"));

    // 2. Admin can view any user's subscriptions
    assertThat(mockMvc.get()
            .uri("/api/v1/users/" + user.getId() + "/subscriptions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.userId", userIdAssert ->
                    userIdAssert.assertThat().isEqualTo(user.getId().toString()))
            .hasPathSatisfying("$.totalCount", countAssert ->
                    countAssert.assertThat().asNumber().isEqualTo(1));

    // 3. User cannot view other user's subscriptions
    assertThat(mockMvc.get()
            .uri("/api/v1/users/" + admin.getId() + "/subscriptions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
            .secure(true))
            .hasStatus(HttpStatus.FORBIDDEN);

    // 4. User with no subscriptions returns empty list
    assertThat(mockMvc.get()
            .uri("/api/v1/users/" + admin.getId() + "/subscriptions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK)
            .bodyJson()
            .hasPathSatisfying("$.totalCount", countAssert ->
                    countAssert.assertThat().asNumber().isEqualTo(0))
            .hasPathSatisfying("$.subscriptions", subscriptionsAssert ->
                    subscriptionsAssert.assertThat().asArray().hasSize(0));
  }

  @Test
  void errorHandling_ShouldHandleInvalidRequests() throws Exception {
    FxUser admin = createTestUser("admin@example.com", UserRole.ADMIN);
    String adminJwt = generateTestJwtToken("admin@example.com");

    // 1. Invalid user ID
    assertThat(mockMvc.get()
            .uri("/api/v1/users/invalid-uuid")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);

    // 2. Non-existent user ID
    String nonExistentId = "550e8400-e29b-41d4-a716-446655440000";
    assertThat(mockMvc.get()
            .uri("/api/v1/users/" + nonExistentId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.NOT_FOUND);

    // 3. Invalid update request (invalid email)
    UserUpdateRequest invalidRequest = new UserUpdateRequest(
            "invalid-email", "+1234567890", "token"
    );

    assertThat(mockMvc.put()
            .uri("/api/v1/users/" + admin.getId())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(invalidRequest))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.BAD_REQUEST);

    // 4. Invalid update request (invalid mobile)
    UserUpdateRequest invalidMobileRequest = new UserUpdateRequest(
            "valid@email.com", "invalid-mobile", "token"
    );

    assertThat(mockMvc.put()
            .uri("/api/v1/users/" + admin.getId())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(invalidMobileRequest))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.BAD_REQUEST);

    // 5. Delete non-existent user
    assertThat(mockMvc.delete()
            .uri("/api/v1/users/" + nonExistentId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void dataPersistenceFlow_ShouldPersistToDatabase() throws Exception {
    // Create admin user
    FxUser admin = createTestUser("admin@example.com", UserRole.ADMIN);
    String adminJwt = generateTestJwtToken("admin@example.com");

    // 1. Verify user exists in database
    assertThat(fxUserRepository.findByEmail("admin@example.com")).isPresent();

    // 2. Update user and verify persistence
    UserUpdateRequest updateRequest = new UserUpdateRequest(
            "updated@example.com", "+9876543210", "persistent-token"
    );

    assertThat(mockMvc.put()
            .uri("/api/v1/users/" + admin.getId())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(updateRequest))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.OK);

    // 3. Verify changes persisted to database
    FxUser updatedUser = fxUserRepository.findById(admin.getId()).orElseThrow();
    assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
    assertThat(updatedUser.getMobile()).isEqualTo("+9876543210");
    assertThat(updatedUser.getPushDeviceToken()).isEqualTo("persistent-token");

    // 4. Delete user and verify removal from database
    adminJwt = generateTestJwtToken("updated@example.com");
    assertThat(mockMvc.delete()
            .uri("/api/v1/users/" + admin.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
            .secure(true))
            .hasStatus(HttpStatus.NO_CONTENT);

    assertThat(fxUserRepository.findById(admin.getId())).isEmpty();
  }

  // Helper methods
  private FxUser createTestUser(String email, UserRole role) {
    FxUser user = new FxUser();
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode("password"));
    user.setMobile("+1234567890");
    user.setEnabled(true);
    user.setRole(role);
    return fxUserRepository.save(user);
  }

  private String generateTestJwtToken(String username) {
    return Jwts.builder()
            .subject(username)
            .claim("roles", List.of("ROLE_" + (username.contains("admin") ? "ADMIN" : "USER")))
            .issuedAt(new java.util.Date())
            .expiration(new java.util.Date(System.currentTimeMillis() + 3600000)) // 1 hour
            .signWith(testSecretKey)
            .compact();
  }
}

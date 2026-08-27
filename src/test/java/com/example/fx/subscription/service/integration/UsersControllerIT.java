package com.example.fx.subscription.service.integration;

import com.example.fx.subscription.service.dto.subscription.SubscriptionCreateRequest;
import com.example.fx.subscription.service.dto.user.UserUpdateRequest;
import com.example.fx.subscription.service.helper.PostgresTestContainerConfig;
import com.example.fx.subscription.service.helper.WebSecurityTestConfig;
import com.example.fx.subscription.service.model.FxUser;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.model.UserRole;
import com.example.fx.subscription.service.repository.FxUserRepository;
import com.example.fx.subscription.service.repository.SubscriptionRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestContainerConfig.class, WebSecurityTestConfig.class})
class UsersControllerIT {

	private WebTestClient webTestClient;

	@Autowired
	private FxUserRepository fxUserRepository;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	// Mock external dependencies to speed up tests
	@MockitoBean
	private KafkaAdmin kafkaAdmin;

	@MockitoBean
	private KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate;

	@Value("${security.jwt.token.secret-key}")
	private String jwtSecretString;

	private SecretKey testSecretKey;

	@BeforeEach
	void setUp(@LocalServerPort int port) {
		this.webTestClient = WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + port)
				.build();

		// Cleanup database before each test
		subscriptionRepository.deleteAll();
		fxUserRepository.deleteAll();

		// Setup JWT key
		testSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretString));
	}

	@Test
	void completeUserLifecycle_ShouldWorkEndToEnd() {
		// 1. Create admin user
		FxUser admin = createTestUser("admin@example.com", UserRole.ADMIN);
		String adminJwt = generateTestJwtToken("admin@example.com");

		// 2. Create regular user
		FxUser user = createTestUser("user@example.com", UserRole.USER);
		String userJwt = generateTestJwtToken("user@example.com");

		// 3. Admin can view all users
		webTestClient.get()
				.uri("/api/v1/users")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.users").isArray()
				.jsonPath("$.users.length()").isEqualTo(2)
				.jsonPath("$.totalElements").isEqualTo(2)
				.jsonPath("$.users[0].email").isEqualTo("user@example.com")
				.jsonPath("$.users[1].email").isEqualTo("admin@example.com");

		// 4. User can view their own profile
		webTestClient.get()
				.uri("/api/v1/users/{id}", user.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.id").isEqualTo(user.getId().toString())
				.jsonPath("$.email").isEqualTo("user@example.com");

		// 5. User cannot view other user's profile
		webTestClient.get()
				.uri("/api/v1/users/{id}", admin.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
				.exchange()
				.expectStatus().isForbidden();

		// 6. Admin can update user
		UserUpdateRequest updateRequest = new UserUpdateRequest(
				"updated@example.com", "+1234567890", "new-push-token"
		);

		webTestClient.put()
				.uri("/api/v1/users/{id}", user.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.bodyValue(updateRequest)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.user.email").isEqualTo("updated@example.com")
				.jsonPath("$.user.mobile").isEqualTo("+1234567890")
				.jsonPath("$.user.pushDeviceToken").isEqualTo("new-push-token")
				.jsonPath("$.user.createdAt").isNotEmpty();

		// 7. Verify user was updated in database
		FxUser updatedUser = fxUserRepository.findById(user.getId()).orElseThrow();
		assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
		assertThat(updatedUser.getMobile()).isEqualTo("+1234567890");
		assertThat(updatedUser.getPushDeviceToken()).isEqualTo("new-push-token");

		// 8. Admin can delete user
		webTestClient.delete()
				.uri("/api/v1/users/{id}", user.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isNoContent();

		// 9. Verify user is deleted via API
		webTestClient.get()
				.uri("/api/v1/users/{id}", user.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isNotFound();

		// 10. Verify user is actually deleted from database
		assertThat(fxUserRepository.findById(user.getId())).isEmpty();
	}

	@Test
	void authorizationFlow_ShouldEnforceAccessControl() {
		// Create users
		FxUser user1 = createTestUser("user1@example.com", UserRole.USER);
		FxUser user2 = createTestUser("user2@example.com", UserRole.USER);

		String adminJwt = prepareUserSearchData();
		String user1Jwt = generateTestJwtToken("user1@example.com");
		String user2Jwt = generateTestJwtToken("user2@example.com");

		// 1. Only admin can access /api/v1/users (get all users)
		webTestClient.get()
				.uri("/api/v1/users")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isOk();

		webTestClient.get()
				.uri("/api/v1/users")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Jwt)
				.exchange()
				.expectStatus().isForbidden();

		// 2. Users can only access their own profile
		webTestClient.get()
				.uri("/api/v1/users/{id}", user1.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Jwt)
				.exchange()
				.expectStatus().isOk();

		webTestClient.get()
				.uri("/api/v1/users/{id}", user2.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Jwt)
				.exchange()
				.expectStatus().isForbidden();

		// 3. Admin can access any user's profile
		webTestClient.get()
				.uri("/api/v1/users/{id}", user1.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isOk();

		webTestClient.get()
				.uri("/api/v1/users/{id}", user2.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isOk();

		// 4. Only admin can delete users
		webTestClient.delete()
				.uri("/api/v1/users/{id}", user1.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Jwt)
				.exchange()
				.expectStatus().isForbidden();

		webTestClient.delete()
				.uri("/api/v1/users/{id}", user1.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isNoContent();

		// 5. Only admin can search users
		webTestClient.get()
				.uri("/api/v1/users/search?email=user")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + user2Jwt)
				.exchange()
				.expectStatus().isForbidden();

		webTestClient.get()
				.uri("/api/v1/users/search?email=user")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	void searchByEmail_ShouldReturnMatchingUser() {
		String adminJwt = prepareUserSearchData();

		webTestClient.get()
				.uri("/api/v1/users/search?email=john")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.users").isArray()
				.jsonPath("$.users.length()").isEqualTo(1)
				.jsonPath("$.totalElements").isEqualTo(1)
				.jsonPath("$.users[0].email").isEqualTo("john@test.com");
	}

	@Test
	void searchByMobile_ShouldReturnAllUsers() {
		String adminJwt = prepareUserSearchData();

		webTestClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/api/v1/users/search")
						.queryParam("mobile", "07711223344")
						.build())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalElements").isEqualTo(4);
	}

	@Test
	void searchByUserEnabled_ShouldReturnEnabledUsers() {
		String adminJwt = prepareUserSearchData();

		webTestClient.get()
				.uri("/api/v1/users/search?enabled=true")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalElements").isEqualTo(4);
	}

	@Test
	void searchWithPagination_ShouldReturnPagedUsers() {
		String adminJwt = prepareUserSearchData();

		webTestClient.get()
				.uri("/api/v1/users/search?size=2&page=0")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.users").isArray()
				.jsonPath("$.users.length()").isEqualTo(2)
				.jsonPath("$.totalElements").isEqualTo(4)
				.jsonPath("$.totalPages").isEqualTo(2);
	}

	@Test
	void searchWithNoResults_ShouldReturnEmpty() {
		String adminJwt = prepareUserSearchData();

		webTestClient.get()
				.uri("/api/v1/users/search?email=nonexistent")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalElements").isEqualTo(0)
				.jsonPath("$.users").isArray()
				.jsonPath("$.users.length()").isEqualTo(0);
	}

	@Test
	void userSubscriptions_ShouldReturnUserSubscriptions() {
		// Create users
		FxUser admin = createTestUser("admin@example.com", UserRole.ADMIN);
		FxUser user = createTestUser("user@example.com", UserRole.USER);

		String adminJwt = generateTestJwtToken("admin@example.com");
		String userJwt = generateTestJwtToken("user@example.com");

		// Create subscription for user via endpoint
		SubscriptionCreateRequest subscriptionRequest = new SubscriptionCreateRequest(
				"EUR/USD", BigDecimal.valueOf(1.15), "ABOVE", List.of("EMAIL", "SMS")
		);

		webTestClient.post()
				.uri("/api/v1/subscriptions")
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
				.bodyValue(subscriptionRequest)
				.exchange()
				.expectStatus().isCreated();

		// 1. User can view their own subscriptions
		webTestClient.get()
				.uri("/api/v1/users/{id}/subscriptions", user.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.userId").isEqualTo(user.getId().toString())
				.jsonPath("$.totalCount").isEqualTo(1)
				.jsonPath("$.subscriptions[0].currencyPair").isEqualTo("EUR/USD");

		// 2. Admin can view any user's subscriptions
		webTestClient.get()
				.uri("/api/v1/users/{id}/subscriptions", user.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.userId").isEqualTo(user.getId().toString())
				.jsonPath("$.totalCount").isEqualTo(1);

		// 3. User cannot view other user's subscriptions
		webTestClient.get()
				.uri("/api/v1/users/{id}/subscriptions", admin.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
				.exchange()
				.expectStatus().isForbidden();

		// 4. User with no subscriptions returns empty list
		webTestClient.get()
				.uri("/api/v1/users/{id}/subscriptions", admin.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.totalCount").isEqualTo(0)
				.jsonPath("$.subscriptions").isArray()
				.jsonPath("$.subscriptions.length()").isEqualTo(0);
	}

	@Test
	void errorHandling_ShouldHandleInvalidRequests() {
		FxUser admin = createTestUser("admin@example.com", UserRole.ADMIN);
		String adminJwt = generateTestJwtToken("admin@example.com");

		// 1. Invalid user ID format
		webTestClient.get()
				.uri("/api/v1/users/invalid-uuid")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().is5xxServerError();

		// 2. Non-existent user ID
		String nonExistentId = "550e8400-e29b-41d4-a716-446655440000";
		webTestClient.get()
				.uri("/api/v1/users/{id}", nonExistentId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isNotFound();

		// 3. Invalid update request (invalid email)
		UserUpdateRequest invalidRequest = new UserUpdateRequest(
				"invalid-email", "+1234567890", "token"
		);

		webTestClient.put()
				.uri("/api/v1/users/{id}", admin.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.bodyValue(invalidRequest)
				.exchange()
				.expectStatus().isBadRequest();

		// 4. Invalid update request (invalid mobile)
		UserUpdateRequest invalidMobileRequest = new UserUpdateRequest(
				"valid@email.com", "invalid-mobile", "token"
		);

		webTestClient.put()
				.uri("/api/v1/users/{id}", admin.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.bodyValue(invalidMobileRequest)
				.exchange()
				.expectStatus().isBadRequest();

		// 5. Delete non-existent user
		webTestClient.delete()
				.uri("/api/v1/users/{id}", nonExistentId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isNotFound();
	}

	@Test
	void dataPersistenceFlow_ShouldPersistToDatabase() {
		// Create admin user
		FxUser admin = createTestUser("admin@example.com", UserRole.ADMIN);
		String adminJwt = generateTestJwtToken("admin@example.com");

		// 1. Verify user exists in database
		assertThat(fxUserRepository.findByEmail("admin@example.com")).isPresent();

		// 2. Update user and verify persistence via endpoint
		UserUpdateRequest updateRequest = new UserUpdateRequest(
				"updated@example.com", "+9876543210", "persistent-token"
		);

		webTestClient.put()
				.uri("/api/v1/users/{id}", admin.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.bodyValue(updateRequest)
				.exchange()
				.expectStatus().isOk();

		// 3. Verify changes persisted to database
		FxUser updatedUser = fxUserRepository.findById(admin.getId()).orElseThrow();
		assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
		assertThat(updatedUser.getMobile()).isEqualTo("+9876543210");
		assertThat(updatedUser.getPushDeviceToken()).isEqualTo("persistent-token");

		// 4. Delete user and verify removal from database
		adminJwt = generateTestJwtToken("updated@example.com");
		webTestClient.delete()
				.uri("/api/v1/users/{id}", admin.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isNoContent();

		assertThat(fxUserRepository.findById(admin.getId())).isEmpty();
	}

	// Helper methods
	private FxUser createTestUser(String email, UserRole role) {
		FxUser user = new FxUser();
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode("password"));
		user.setMobile("07711223344");
		user.setEnabled(true);
		user.setRole(role);
		user.setCreatedAt(Instant.now());
		return fxUserRepository.save(user);
	}

	private String generateTestJwtToken(String username) {
		return Jwts.builder()
				.subject(username)
				.claim("roles", List.of("ROLE_" + (username.contains("admin") ? "ADMIN" : "USER")))
				.issuedAt(Date.from(Instant.now()))
				.expiration(Date.from(Instant.now().plusMillis(3600000))) // Plus 1 hour
				.signWith(testSecretKey)
				.compact();
	}

	private String prepareUserSearchData() {
		createTestUser("admin@example.com", UserRole.ADMIN);
		createTestUser("bob@example.com", UserRole.USER);
		createTestUser("jane@example.com", UserRole.USER);
		createTestUser("john@test.com", UserRole.USER);
		return generateTestJwtToken("admin@example.com");
	}
}

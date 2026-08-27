package com.example.fx.subscription.service.integration;

import com.example.fx.subscription.service.dto.subscription.SubscriptionCreateRequest;
import com.example.fx.subscription.service.dto.subscription.SubscriptionCreateResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionUpdateRequest;
import com.example.fx.subscription.service.helper.PostgresTestContainerConfig;
import com.example.fx.subscription.service.helper.WebSecurityTestConfig;
import com.example.fx.subscription.service.model.FxUser;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.model.UserRole;
import com.example.fx.subscription.service.repository.FxUserRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestContainerConfig.class, WebSecurityTestConfig.class})
class SubscriptionsControllerIT {

	private WebTestClient webTestClient;

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
	void setUp(@LocalServerPort int port) {
		// Cleanup database before each test
		fxUserRepository.deleteAll();

		// Setup JWT key
		testSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretString));

		this.webTestClient = WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + port)
				.build();
	}

	@Test
	void completeSubscriptionLifecycle_ShouldWorkEndToEnd() {
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
	void authenticationFlow_ShouldHandleInvalidTokens() {
		SubscriptionCreateRequest createRequest = new SubscriptionCreateRequest(
				"GBP/USD", BigDecimal.valueOf(1.20), "ABOVE", List.of("sms", "email"));

		// Test invalid JWT token
		webTestClient.post()
				.uri("/api/v1/subscriptions")
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
				.bodyValue(createRequest)
				.exchange()
				.expectStatus().isUnauthorized();

		// Test missing JWT token
		webTestClient.post()
				.uri("/api/v1/subscriptions")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(createRequest)
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void authorizationFlow_ShouldEnforceAccessControl() {
		// Create two users FIRST, then generate JWTs
		createTestUser("user1@example.com", UserRole.USER);
		createTestUser("user2@example.com", UserRole.USER);

		String user1Jwt = generateTestJwtToken("user1@example.com");
		String user2Jwt = generateTestJwtToken("user2@example.com");

		// User1 creates a subscription
		String subscriptionId = createSubscription(user1Jwt, BigDecimal.valueOf(1.25));

		// User2 tries to access User1's subscription - should be forbidden
		webTestClient.get()
				.uri("/api/v1/subscriptions/{id}", subscriptionId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + user2Jwt)
				.exchange()
				.expectStatus().isForbidden();

		// User2 tries to update User1's subscription - should be forbidden
		SubscriptionUpdateRequest updateRequest = new SubscriptionUpdateRequest(
				"EUR/USD", BigDecimal.valueOf(1.15), "BELOW", "ACTIVE", List.of("email"));

		webTestClient.put()
				.uri("/api/v1/subscriptions/{id}", subscriptionId)
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + user2Jwt)
				.bodyValue(updateRequest)
				.exchange()
				.expectStatus().isForbidden();

		// User2 tries to delete User1's subscription - should be forbidden
		webTestClient.delete()
				.uri("/api/v1/subscriptions/{id}", subscriptionId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + user2Jwt)
				.exchange()
				.expectStatus().isForbidden();
	}

	@Test
	void adminAuthorizationFlow_ShouldAllowAdminAccess() {
		// Create admin user FIRST, then generate JWT
		createTestUser("admin@example.com", UserRole.ADMIN);
		String adminJwt = generateTestJwtToken("admin@example.com");

		// Create regular user and subscription
		createTestUser("user@example.com", UserRole.USER);
		String userJwt = generateTestJwtToken("user@example.com");

		String subscriptionId = createSubscription(userJwt, BigDecimal.valueOf(1.25));

		// Admin should be able to access all subscriptions
		webTestClient.get()
				.uri("/api/v1/subscriptions/all")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.subscriptions").isArray()
				.jsonPath("$.subscriptions.length()").value(len -> assertThat((Integer) len).isGreaterThanOrEqualTo(1));

		// Admin should be able to access any individual subscription
		webTestClient.get()
				.uri("/api/v1/subscriptions/{id}", subscriptionId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isOk();

		// Regular user should NOT be able to access all subscriptions
		webTestClient.get()
				.uri("/api/v1/subscriptions/all")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
				.exchange()
				.expectStatus().isForbidden();
	}

	@Test
	void dataPersistenceFlow_ShouldPersistToDatabase() {
		// Create user FIRST, then generate JWT
		createTestUser("persistence_test@mail.com", UserRole.USER);
		String userJwt = generateTestJwtToken("persistence_test@mail.com");

		// Create subscription
		String subscriptionId = createSubscription(userJwt, BigDecimal.valueOf(1.25));

		// Verify subscription exists in user's subscription list
		webTestClient.get()
				.uri("/api/v1/subscriptions/my")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwt)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.subscriptions.length()").isEqualTo(1)
				.jsonPath("$.subscriptions[0].id").isEqualTo(subscriptionId)
				.jsonPath("$.subscriptions[0].currencyPair").isEqualTo("GBP/USD")
				.jsonPath("$.subscriptions[0].threshold").isEqualTo(1.25);
	}

	// Helper methods for creating test data
	private void createTestUser(String email, UserRole role) {
		FxUser user = new FxUser();
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode("Test_Password"));
		user.setMobile("+447911123456");
		user.setRole(role);
		user.setEnabled(true);
		user.setCreatedAt(Instant.now());
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
	private String createSubscription(String jwt, BigDecimal threshold) {
		SubscriptionCreateRequest createRequest = new SubscriptionCreateRequest(
				"GBP/USD", threshold, "ABOVE", List.of("email", "sms"));

		SubscriptionCreateResponse createResponse = webTestClient.post()
				.uri("/api/v1/subscriptions")
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
				.bodyValue(createRequest)
				.exchange()
				.expectStatus().isCreated()
				.expectBody(SubscriptionCreateResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(createResponse).isNotNull();
		assertThat(createResponse.subscription().currencyPair()).isEqualTo("GBP/USD");
		assertThat(createResponse.subscription().threshold()).isEqualTo(threshold);
		assertThat(createResponse.subscription().createdAt()).isNotNull();

		return createResponse.subscriptionId();
	}

	private void verifySubscriptionExists(String jwt, String subscriptionId, String expectedCurrencyPair, BigDecimal expectedThreshold) {
		webTestClient.get()
				.uri("/api/v1/subscriptions/{id}", subscriptionId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.currencyPair").isEqualTo(expectedCurrencyPair)
				.jsonPath("$.threshold").isEqualTo(expectedThreshold.doubleValue());
	}

	private void updateSubscription(String jwt, String subscriptionId, BigDecimal threshold) {
		SubscriptionUpdateRequest updateRequest = new SubscriptionUpdateRequest(
				"EUR/USD", threshold, "BELOW", "ACTIVE", List.of("email"));

		webTestClient.put()
				.uri("/api/v1/subscriptions/{id}", subscriptionId)
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
				.bodyValue(updateRequest)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.subscription.currencyPair").isEqualTo("EUR/USD")
				.jsonPath("$.subscription.threshold").isEqualTo(threshold.doubleValue())
				.jsonPath("$.subscription.updatedAt").isNotEmpty();
	}

	private void verifyUserHasSubscription(String jwt) {
		webTestClient.get()
				.uri("/api/v1/subscriptions/my")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.subscriptions").isArray()
				.jsonPath("$.subscriptions.length()").value(len -> assertThat((Integer) len).isGreaterThanOrEqualTo(1))
				.jsonPath("$.totalCount").isEqualTo(1);
	}

	private void deleteSubscription(String jwt, String subscriptionId) {
		webTestClient.delete()
				.uri("/api/v1/subscriptions/{id}", subscriptionId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
				.exchange()
				.expectStatus().isOk();
	}

	private void verifySubscriptionDeleted(String jwt, String subscriptionId) {
		webTestClient.get()
				.uri("/api/v1/subscriptions/{id}", subscriptionId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
				.exchange()
				.expectStatus().isNotFound();
	}
}

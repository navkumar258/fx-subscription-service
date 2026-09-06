package com.example.fx.subscription.service.integration;

import com.example.fx.subscription.service.dto.auth.AuthLoginResponse;
import com.example.fx.subscription.service.dto.auth.AuthRequest;
import com.example.fx.subscription.service.dto.auth.AuthSignupResponse;
import com.example.fx.subscription.service.dto.user.UserSignUpRequest;
import com.example.fx.subscription.service.helper.PostgresTestContainerConfig;
import com.example.fx.subscription.service.helper.WebSecurityTestConfig;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.repository.FxUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestContainerConfig.class, WebSecurityTestConfig.class})
class AuthenticationFlowIT {

	private WebTestClient webTestClient;

	@Autowired
	private FxUserRepository fxUserRepository;

	// Mock external dependencies to speed up tests
	@MockitoBean
	private KafkaAdmin kafkaAdmin;

	@MockitoBean
	private KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate;

	@BeforeEach
	void setUp(@LocalServerPort int port) {
		this.webTestClient = WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + port)
				.build();

		fxUserRepository.deleteAll();
	}

	@Test
	void completeUserRegistrationAndLoginFlow_ShouldWorkEndToEnd() {
		// 1. Register new user
		UserSignUpRequest signUpRequest = new UserSignUpRequest(
				"test@example.com", "password123", "+1234567890", false);

		AuthSignupResponse signupResponse = webTestClient.post()
				.uri("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(signUpRequest)
				.exchange()
				.expectStatus().isCreated()
				.expectBody(AuthSignupResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(signupResponse).isNotNull();
		assertThat(signupResponse.userId()).isNotNull();
		assertThat(signupResponse.message()).isEqualTo("User registered successfully");

		// 2. Login with registered user
		AuthRequest authRequest = new AuthRequest("test@example.com", "password123");

		AuthLoginResponse loginResponse = webTestClient.post()
				.uri("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(authRequest)
				.exchange()
				.expectStatus().isOk()
				.expectBody(AuthLoginResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(loginResponse).isNotNull();
		assertThat(loginResponse.token()).isNotNull();
		assertThat(loginResponse.message()).isEqualTo("Login successful");

		// 3. Verify user exists in database
		assertThat(fxUserRepository.findByEmail("test@example.com")).isPresent();
	}

	@Test
	void adminUserRegistrationAndPrivilegedAccess_ShouldWorkEndToEnd() {
		// 1. Register admin user
		UserSignUpRequest adminSignUpRequest = new UserSignUpRequest(
				"admin@example.com", "admin123", "+1234567890", true);

		AuthSignupResponse signupResponse = webTestClient.post()
				.uri("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(adminSignUpRequest)
				.exchange()
				.expectStatus().isCreated()
				.expectBody(AuthSignupResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(signupResponse).isNotNull();
		assertThat(signupResponse.userId()).isNotNull();
		assertThat(signupResponse.message()).isEqualTo("Admin registered successfully");

		// 2. Login as admin and extract typed response
		AuthRequest adminAuthRequest = new AuthRequest("admin@example.com", "admin123");

		AuthLoginResponse adminResponse = webTestClient.post()
				.uri("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(adminAuthRequest)
				.exchange()
				.expectStatus().isOk()
				.expectBody(AuthLoginResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(adminResponse).isNotNull();
		assertThat(adminResponse.token()).isNotNull();

		// 3. Use admin token to access privileged endpoints
		webTestClient.get()
				.uri("/api/v1/subscriptions/all")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminResponse.token())
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	void invalidCredentials_ShouldBeRejected() {
		// 1. Try to login with non-existent user
		AuthRequest invalidRequest = new AuthRequest("nonexistent@example.com", "password123");

		webTestClient.post()
				.uri("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(invalidRequest)
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void signupWithDuplicateEmail_ShouldReturnConflictOrBadRequest() {
		// Given
		UserSignUpRequest signUpRequest = new UserSignUpRequest(
				"duplicate@example.com", "password123", "+1234567890", false);

		webTestClient.post()
				.uri("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(signUpRequest)
				.exchange()
				.expectStatus().isCreated();

		// When & Then (Attempt duplicate registration)
		webTestClient.post()
				.uri("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(signUpRequest)
				.exchange()
				.expectStatus().is4xxClientError();
	}
}
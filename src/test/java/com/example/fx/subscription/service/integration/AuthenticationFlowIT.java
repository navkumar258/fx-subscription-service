package com.example.fx.subscription.service.integration;

import com.example.fx.subscription.service.dto.auth.AuthLoginResponse;
import com.example.fx.subscription.service.dto.auth.AuthRequest;
import com.example.fx.subscription.service.dto.user.UserSignUpRequest;
import com.example.fx.subscription.service.helper.PostgresTestContainersConfig;
import com.example.fx.subscription.service.helper.WebSecurityTestConfig;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.repository.FxUserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestContainersConfig.class, WebSecurityTestConfig.class})
class AuthenticationFlowIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private FxUserRepository fxUserRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  // Mock external dependencies to speed up tests
  @MockitoBean
  KafkaAdmin kafkaAdmin;

  @MockitoBean
  KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate;

  @BeforeEach
  void setUp() {
    fxUserRepository.deleteAll();
  }

  @Test
  void completeUserRegistrationAndLoginFlow_ShouldWorkEndToEnd() throws Exception {
    // 1. Register new user
    UserSignUpRequest signUpRequest = new UserSignUpRequest(
            "test@example.com", "password123", "+1234567890", false);

    mockMvc.perform(post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.message").value("User registered successfully"));

    // 2. Login with registered user
    AuthRequest authRequest = new AuthRequest("test@example.com", "password123");

    mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(authRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.message").value("Login successful"))
            .andReturn().getResponse().getContentAsString();

    // 3. Verify user exists in database
    assertThat(fxUserRepository.findByEmail("test@example.com")).isPresent();
  }

  @Test
  void adminUserRegistrationAndPrivilegedAccess_ShouldWorkEndToEnd() throws Exception {
    // 1. Register admin user
    UserSignUpRequest adminSignUpRequest = new UserSignUpRequest(
            "admin@example.com", "admin123", "+1234567890", true);

    mockMvc.perform(post("/api/v1/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(adminSignUpRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.message").value("Admin registered successfully"));

    // 2. Login as admin
    AuthRequest adminAuthRequest = new AuthRequest("admin@example.com", "admin123");

    MockHttpServletResponse adminResponse = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(adminAuthRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andReturn().getResponse();

    // 3. Use admin token to access privileged endpoints
    String adminToken = extractTokenFromResponse(adminResponse.getContentAsString());

    mockMvc.perform(get("/api/v1/subscriptions/all")
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
  }

  @Test
  void invalidCredentials_ShouldBeRejected() throws Exception {
    // 1. Try to login with non-existent user
    AuthRequest invalidRequest = new AuthRequest("nonexistent@example.com", "password123");

    mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isUnauthorized());
  }

  private String extractTokenFromResponse(String response) throws JsonProcessingException {
    return objectMapper.readValue(response, AuthLoginResponse.class)
            .token();
  }
} 
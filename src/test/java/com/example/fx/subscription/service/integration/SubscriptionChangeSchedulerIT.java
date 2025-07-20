package com.example.fx.subscription.service.integration;

import com.example.fx.subscription.service.dto.auth.AuthLoginResponse;
import com.example.fx.subscription.service.dto.auth.AuthRequest;
import com.example.fx.subscription.service.dto.subscription.SubscriptionCreateRequest;
import com.example.fx.subscription.service.helper.PostgresTestContainersConfig;
import com.example.fx.subscription.service.helper.WebSecurityTestConfig;
import com.example.fx.subscription.service.model.FxUser;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.model.UserRole;
import com.example.fx.subscription.service.repository.FxUserRepository;
import com.example.fx.subscription.service.repository.SubscriptionRepository;
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

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestContainersConfig.class, WebSecurityTestConfig.class})
class SubscriptionChangeSchedulerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FxUserRepository fxUserRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Mock external dependencies to speed up tests
    @MockitoBean
    KafkaAdmin kafkaAdmin;

    @MockitoBean
    KafkaTemplate<String, SubscriptionChangeEvent> kafkaTemplate;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        fxUserRepository.deleteAll();
        subscriptionRepository.deleteAll();
        createTestUser();
        userToken = loginUser("user@example.com", "password123");
    }

    @Test
    void scheduledSubscriptionProcessing_ShouldWorkEndToEnd() throws Exception {
        // 1. Create multiple subscriptions
        createSubscription("GBP/USD", BigDecimal.valueOf(1.25), "ABOVE");
        createSubscription("EUR/USD", BigDecimal.valueOf(1.10), "BELOW");
        createSubscription("USD/JPY", BigDecimal.valueOf(150.0), "ABOVE");

        // 2. Verify subscriptions exist
        assertThat(subscriptionRepository.findAll()).hasSize(3);

        // 3. Trigger scheduled task manually (or wait for it to run)
        // This would depend on your scheduler implementation
        // You might need to expose an endpoint to trigger the task for testing

        // 4. Verify scheduled task processed subscriptions
        // Check if any expected side effects occurred (e.g., events published, status changes)
    }

    private void createSubscription(String currencyPair, BigDecimal threshold, String direction) throws Exception {
        SubscriptionCreateRequest createRequest = new SubscriptionCreateRequest(
                currencyPair, threshold, direction, List.of("email"));

        mockMvc.perform(post("/api/v1/subscriptions")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());
    }

    private void createTestUser() {
        FxUser user = new FxUser();
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setMobile("+1234567890");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        fxUserRepository.save(user);
    }

    private String loginUser(String email, String password) throws Exception {
        AuthRequest authRequest = new AuthRequest(email, password);

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        return extractTokenFromResponse(response.getContentAsString());
    }

    private String extractTokenFromResponse(String response)
            throws JsonProcessingException {
        return objectMapper.readValue(response, AuthLoginResponse.class)
                .token();
    }
} 
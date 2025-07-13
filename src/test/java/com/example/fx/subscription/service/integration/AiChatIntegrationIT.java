package com.example.fx.subscription.service.integration;

import com.example.fx.subscription.service.dto.auth.AuthLoginResponse;
import com.example.fx.subscription.service.dto.auth.AuthRequest;
import com.example.fx.subscription.service.dto.subscription.SubscriptionCreateRequest;
import com.example.fx.subscription.service.helper.TestSecurityConfig;
import com.example.fx.subscription.service.model.FxUser;
import com.example.fx.subscription.service.model.SubscriptionChangeEvent;
import com.example.fx.subscription.service.model.UserRole;
import com.example.fx.subscription.service.repository.FxUserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class AiChatIntegrationIT {

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

    @MockitoBean
    ChatClient chatClient;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        fxUserRepository.deleteAll();
        createTestUser();
        userToken = loginUser("user@example.com", "password123");

        setupChatClientMock();
    }

    @Test
    void aiChatWithSubscriptionContext_ShouldWorkEndToEnd() throws Exception {
        // 1. Create a subscription first
        SubscriptionCreateRequest createRequest = new SubscriptionCreateRequest(
                "GBP/USD", BigDecimal.valueOf(1.25), "ABOVE", List.of("email"));

        mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // 2. Test AI chat with subscription-related query
        String chatRequest = "Show me my current subscriptions";
        String expectedResponse = "You have 1 active subscription for GBP/USD at threshold 1.25.";

        // Mock specific response for this query
        setupChatClientMockWithResponse(expectedResponse);

        mockMvc.perform(get("/api/ai/fx?query=" + chatRequest)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(expectedResponse)));

        verify(chatClient, atLeastOnce()).prompt(any(Prompt.class));
    }

    @Test
    void aiChatWithSubscriptionCreation_ShouldWorkEndToEnd() throws Exception {
        // Test AI chat that creates a subscription
        String chatRequest = "Create a subscription for EUR/USD above 1.10 with email notifications";
        String expectedResponse = "I'll create a subscription for EUR/USD at threshold 1.10 with email notifications.";

        // Mock specific response for subscription creation
        setupChatClientMockWithResponse(expectedResponse);

        mockMvc.perform(get("/api/ai/fx?query=" + chatRequest)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(expectedResponse)));

        verify(chatClient, atLeastOnce()).prompt(any(Prompt.class));
    }

    @Test
    void aiChatWithError_ShouldHandleGracefully() throws Exception {
        // Test AI chat with error response
        String chatRequest = "Invalid query";

        // Mock error response
        setupChatClientMockWithException(new RuntimeException("AI service error"));

        mockMvc.perform(get("/api/ai/fx?query=" + chatRequest)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isInternalServerError());

        verify(chatClient).prompt(any(Prompt.class));
    }

    @Test
    void aiChatWithEmptyQuery_ShouldReturnDefaultResponse() throws Exception {
        // Test AI chat with empty query
        String chatRequest = "";
        String expectedResponse = "Please provide a query to help you with FX subscriptions.";

        setupChatClientMockWithResponse(expectedResponse);

        mockMvc.perform(get("/api/ai/fx?query=" + chatRequest)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(expectedResponse)));

        verify(chatClient).prompt(any(Prompt.class));
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
        
        MockHttpServletResponse response = mockMvc.perform(post("/api/auth/login")
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

    private void setupChatClientMock() {
        setupChatClientMockWithResponse("Default AI response");
    }

    private void setupChatClientMockWithResponse(String responseContent) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec clientResponse = mock(ChatClient.CallResponseSpec.class);

        ChatResponse chatResponse = createMockChatResponse(responseContent);

        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(clientResponse);
        when(clientResponse.chatResponse()).thenReturn(chatResponse);
    }

    private void setupChatClientMockWithException(Exception exception) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);

        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(exception);
    }

    private ChatResponse createMockChatResponse(String content) {
        Generation generation = new Generation(new AssistantMessage(content));
        return new ChatResponse(List.of(generation));
    }
} 
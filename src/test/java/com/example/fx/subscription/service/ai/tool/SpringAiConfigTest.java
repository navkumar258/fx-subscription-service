package com.example.fx.subscription.service.ai.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SpringAiConfigTest {

    @Mock
    private OllamaChatModel ollamaChatModel;

    @Mock
    private FxSubscriptionTool fxSubscriptionTool;

    private SpringAiConfig springAiConfig;

    @BeforeEach
    void setUp() {
        springAiConfig = new SpringAiConfig(ollamaChatModel, fxSubscriptionTool);
    }

    @Test
    void chatClient_ShouldBeCreatedSuccessfully() {
        // When
        ChatClient chatClient = springAiConfig.chatClient();

        // Then
        assertNotNull(chatClient);
    }

    @Test
    void constructor_ShouldInjectDependencies() {
        // Given
        SpringAiConfig config = new SpringAiConfig(ollamaChatModel, fxSubscriptionTool);

        // Then
        assertNotNull(config);
    }

    @Test
    void chatClient_ShouldNotBeNull() {
        // When
        ChatClient result = springAiConfig.chatClient();

        // Then
        assertNotNull(result);
    }

    @Test
    void chatClient_ShouldReturnChatClientInstance() {
        // When
        ChatClient result = springAiConfig.chatClient();

        // Then
        assertInstanceOf(ChatClient.class, result);
    }
} 
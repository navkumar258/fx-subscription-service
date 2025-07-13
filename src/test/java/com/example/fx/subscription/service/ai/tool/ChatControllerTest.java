package com.example.fx.subscription.service.ai.tool;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private ChatClient chatClient;

    @Test
    @WithMockUser
    void fxChat_WithValidQuery_ShouldReturnChatResponse() {
        // Given
        String userQuery = "Create a subscription for user 123 with GBP/USD at 1.25 threshold";
        String expectedResponse = "I'll help you create a subscription for user 123 with GBP/USD at 1.25 threshold.";
        
        ChatResponse mockChatResponse = createMockChatResponse(expectedResponse);
        setupChatClientMock(mockChatResponse);

        // When & Then
        assertThat(mockMvc.get().uri("/api/ai/fx?query=" + userQuery))
                .hasStatusOk()
                .hasContentType(MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
                .bodyText()
                .contains(expectedResponse);

        verify(chatClient).prompt(any(Prompt.class));
    }

    @Test
    @WithMockUser
    void fxChat_WithEmptyQuery_ShouldReturnChatResponse() {
        // Given
        String userQuery = "";
        String expectedResponse = "Please provide a query to help you with FX subscriptions.";
        
        ChatResponse mockChatResponse = createMockChatResponse(expectedResponse);
        setupChatClientMock(mockChatResponse);

        // When & Then
        assertThat(mockMvc.get().uri("/api/ai/fx?query=" + userQuery))
                .hasStatusOk()
                .hasContentType(MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
                .bodyText()
                .contains(expectedResponse);

        verify(chatClient).prompt(any(Prompt.class));
    }

    @Test
    @WithMockUser
    void fxChat_WithMissingQueryParameter_ShouldReturn400() {
        // When & Then
        assertThat(mockMvc.get().uri("/api/ai/fx"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser
    void fxChat_WithInvalidEndpoint_ShouldReturn404() {
        // When & Then
        assertThat(mockMvc.get().uri("/api/ai/invalid"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockUser
    void fxChat_WithChatClientException_ShouldReturn500() {
        // Given
        String userQuery = "Create a subscription";
        setupChatClientMockWithException(new RuntimeException("Chat client error"));

        // When & Then
        assertThat(mockMvc.get().uri("/api/ai/fx?query=" + userQuery))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @WithMockUser
    void fxChat_WithNullChatResponse_ShouldHandleGracefully() {
        // Given
        String userQuery = "Create a subscription";
        setupChatClientMockWithNullResponse();

        // When & Then
        assertThat(mockMvc.get().uri("/api/ai/fx?query=" + userQuery))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        verify(chatClient).prompt(any(Prompt.class));
    }

    // Helper methods
    private void setupChatClientMock(ChatResponse chatResponse) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec clientResponse = mock(ChatClient.CallResponseSpec.class);
        
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(clientResponse);
        when(clientResponse.chatResponse()).thenReturn(chatResponse);
    }

    private void setupChatClientMockWithException(Exception exception) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(exception);
    }

    private void setupChatClientMockWithNullResponse() {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec clientResponse = mock(ChatClient.CallResponseSpec.class);
        
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(clientResponse);
        when(clientResponse.chatResponse()).thenReturn(null);
    }

    private ChatResponse createMockChatResponse(String content) {
        Generation generation = new Generation(new AssistantMessage(content));
        return new ChatResponse(List.of(generation));
    }
} 
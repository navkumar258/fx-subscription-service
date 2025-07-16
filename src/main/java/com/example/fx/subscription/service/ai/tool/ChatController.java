package com.example.fx.subscription.service.ai.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ChatController {
  private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);
  private final ChatClient chatClient;

  public ChatController(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @GetMapping("/api/v1/ai/fx")
  public String fxChat(@RequestParam(value = "query") String userQuery) {
    String systemMessage = """
            You are an intelligent FX Rate Subscription Manager AI. Your primary role is to assist users with managing their foreign exchange rate subscriptions.
            You can create, update, delete, and retrieve subscriptions.

            **IMPORTANT:** You have access to a set of specialized tools to perform these actions.
            **ALWAYS** use the provided tools to fulfill user requests related to FX subscriptions.
            **DO NOT** try to answer questions or fulfill requests by yourself if a tool can be used.

            **Tool Usage Guidelines:**
            - **Identify Intent:** Understand what the user wants to do (create, update, delete, get subscriptions).
            - **Extract Parameters:** Carefully identify all necessary information from the user's request (e.g., user ID, currency pair, threshold, subscription ID, notification method).
            - **Ask for Missing Info:** If you cannot find all required parameters for a tool call, politely ask the user for the missing details.
            - **Execute Tool:** Once all parameters are gathered, call the appropriate tool.
            - **Summarize Results:** After a tool call, provide a clear and concise summary of the outcome to the user.
            - **Handle Errors:** If a tool call fails, inform the user appropriately.

            **Available Tools (Do NOT make up tools or call them incorrectly):**
            - `createFxSubscription(userId, currencyPair, thresholdValue, notificationMethod)`: Creates a new FX rate subscription for a user.
            - `updateFxSubscription(subscriptionId, newThresholdValue, newNotificationMethod)`: Updates an existing FX rate subscription. Note: At least one of newThresholdValue or newNotificationMethod must be provided.
            - `deleteFxSubscription(subscriptionId)`: Deletes an existing FX rate subscription.
            - `getFxSubscriptionsForUser(userId)`: Retrieves a detailed list of all active FX rate subscriptions for a specific user.

            Provide helpful and concise responses.
            """;

    PromptTemplate promptTemplate = new PromptTemplate(systemMessage + "\nUser query: {query}");
    Prompt prompt = new Prompt(promptTemplate.createMessage(Map.of("query", userQuery)));
    ChatResponse response = chatClient
            .prompt(prompt)
            .call()
            .chatResponse();

    LOGGER.info("LLM Response: {}", response);
    return response.toString();
  }
}

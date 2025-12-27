package com.example.fx.subscription.service.ai.tool;

import com.example.fx.subscription.service.dto.subscription.SubscriptionCreateRequest;
import com.example.fx.subscription.service.dto.subscription.SubscriptionListResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionUpdateRequest;
import com.example.fx.subscription.service.exception.SubscriptionNotFoundException;
import com.example.fx.subscription.service.service.SubscriptionsService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class FxSubscriptionTool {
  private static final String SUBSCRIPTION = "Subscription ";
  private final SubscriptionsService subscriptionService;

  public FxSubscriptionTool(SubscriptionsService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  /**
   * AI Tool: Creates a new FX rate subscription.
   * Delegates to the core SubscriptionService after adapting parameters.
   */
  @Tool(name = "createFxSubscription",
          description = """
                  Creates a new FX rate subscription for a user with a given user id,\s
                  specified currency pair, threshold, and preferred notification method.
                  """
  )
  public String createSubscriptionTool(@ToolParam(description = "Given user id") String userId,
                                       @ToolParam(description = "Given currency pair") String currencyPair,
                                       @ToolParam(description = "Given threshold") double thresholdValue,
                                       @ToolParam(description = "Given direction i.e. above or below") String direction,
                                       @ToolParam(description = "Given notification methods") String notificationMethod) {
    SubscriptionCreateRequest newSubscription = new SubscriptionCreateRequest(
            currencyPair,
            BigDecimal.valueOf(thresholdValue),
            direction,
            Collections.singletonList(notificationMethod)
    );

    SubscriptionResponse savedSubscription = subscriptionService.createSubscription(newSubscription, UUID.fromString(userId));
    return "Subscription for " + savedSubscription.currencyPair() +
            " at threshold " + savedSubscription.threshold() +
            " with direction " + savedSubscription.direction() +
            " created successfully. Your subscription ID is: " + savedSubscription.id() +
            ". Notifications via " + savedSubscription.notificationsChannels() + ".";
  }

  /**
   * AI Tool: Updates an existing FX rate subscription.
   * Delegates to the core SubscriptionService after adapting parameters.
   */
  @Tool(name = "updateFxSubscription",
          description = """
                  Updates the threshold and/or notification method of an existing FX rate subscription using its ID.
                  At least one of newThresholdValue or newNotificationMethod must be provided.
                  """
  )
  public String updateSubscriptionTool(@ToolParam(description = "Existing subscription id") String subscriptionId,
                                       @ToolParam(description = "New currency pair") String currencyPair,
                                       @ToolParam(description = "New threshold value") double newThresholdValue,
                                       @ToolParam(description = "New direction i.e. above or below", required = false) String direction,
                                       @ToolParam(description = "New status") String status,
                                       @ToolParam(description = "New notification methods", required = false) List<String> newNotificationMethod) {
    try {
      SubscriptionUpdateRequest newSubscription = new SubscriptionUpdateRequest(
              currencyPair,
              BigDecimal.valueOf(newThresholdValue),
              direction,
              status,
              newNotificationMethod
      );

      SubscriptionResponse updatedSub = subscriptionService.updateSubscriptionById(subscriptionId, newSubscription);
      return "Subscription: " + subscriptionId
              + " updated successfully. New threshold: "
              + updatedSub.threshold()
              + ", New direction: "
              + updatedSub.direction().name()
              + ", New notification methods: "
              + updatedSub.notificationsChannels() + ".";
    } catch (SubscriptionNotFoundException _) {
      return SUBSCRIPTION + subscriptionId + " not found or no valid updates provided.";
    }
  }

  /**
   * AI Tool: Deletes an FX rate subscription.
   * Delegates to the core SubscriptionService.
   */
  @Tool(name = "deleteFxSubscription",
          description = "Deletes an existing FX rate subscription using its ID."
  )
  public String deleteSubscriptionTool(@ToolParam(description = "Subscription id to delete") String subscriptionId) {
    try {
      subscriptionService.deleteSubscriptionById(subscriptionId);
      return SUBSCRIPTION + subscriptionId + " deleted successfully.";
    } catch (SubscriptionNotFoundException _) {
      return SUBSCRIPTION + subscriptionId + " not found.";
    }
  }

  /**
   * AI Tool: Retrieves all active FX rate subscriptions for a given user.
   * Delegates to the core SubscriptionService and formats the output.
   */
  @Tool(name = "getFxSubscriptionsForUser",
          description = """
                  Retrieves a detailed list of all active FX rate subscriptions for a specific user\s
                  including IDs, currency pairs, thresholds, and notification methods.
                  """
  )
  public String getFxSubscriptionsForUserTool(@ToolParam(description = "User id to get all subscriptions") String userId) {
    SubscriptionListResponse subscriptionListResponse = subscriptionService.findSubscriptionResponsesByUserId(userId);

    if (subscriptionListResponse.totalCount() == 0) {
      return "No active subscriptions found for the user " + userId + ".";
    }

    return subscriptionListResponse.subscriptions()
            .stream()
            .map(sub -> "ID: %s, Pair: %s, Threshold: %.2f, Notify via: %s".formatted(
                    sub.id(), sub.currencyPair(), sub.threshold(), sub.notificationsChannels()))
            .collect(Collectors.joining("\n"));
  }
}

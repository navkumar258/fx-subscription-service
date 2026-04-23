package com.example.fx.subscription.service.ai.tool;

import com.example.fx.subscription.service.dto.subscription.SubscriptionCreateRequest;
import com.example.fx.subscription.service.dto.subscription.SubscriptionListResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.dto.subscription.SubscriptionUpdateRequest;
import com.example.fx.subscription.service.exception.SubscriptionNotFoundException;
import com.example.fx.subscription.service.service.SubscriptionsService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
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
  @McpTool(name = "createSubscription",
          description = """
                  Creates a new FX rate subscription for a user with the given userId,\s
                  specified currency pair, threshold, direction and preferred notification method.
                  """
  )
  public String createSubscription(@McpToolParam(description = "Given user id i.e. a valid uuid") String userId,
                                   @McpToolParam(description = "Given currency pair i.e. GBP/USD") String currencyPair,
                                   @McpToolParam(description = "Given threshold i.e. 1.30") double thresholdValue,
                                   @McpToolParam(description = "Given direction i.e. ABOVE or BELOW") String direction,
                                   @McpToolParam(description = "Given notification methods i.e. email,sms,push") String notificationMethod) {
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
  @McpTool(name = "updateSubscription",
          description = """
                  Updates the threshold, direction and/or notification methods of an existing FX rate subscription for the given subscriptionId.
                  At least one of newThresholdValue or newNotificationMethod must be provided.
                  """
  )
  public String updateSubscription(@McpToolParam(description = "Existing subscription id i.e. a valid uuid") String subscriptionId,
                                   @McpToolParam(description = "New currency pair i.e. GBP/USD") String currencyPair,
                                   @McpToolParam(description = "New threshold value i.e. 1.25") double newThresholdValue,
                                   @McpToolParam(description = "New direction i.e. ABOVE or BELOW", required = false) String direction,
                                   @McpToolParam(description = "New status i.e ACTIVE/INACTIVE/EXPIRED ") String status,
                                   @McpToolParam(description = "New notification methods i.e. sms,email,push", required = false) List<String> newNotificationMethod) {
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
  @McpTool(name = "deleteSubscription",
          description = "Deletes an existing FX rate subscription for the given subscriptionId."
  )
  public String deleteSubscription(@McpToolParam(description = "Subscription id to delete i.e. a valid uuid") String subscriptionId) {
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
  @McpTool(name = "getSubscriptionsForUser",
          description = """
                  Retrieves a detailed list of all active FX rate subscriptions for a specific userId\s
                  including Ids, currency pairs, thresholds, and notification methods.
                  """
  )
  public String getSubscriptionsForUser(@McpToolParam(description = "User id to get all subscriptions i.e. a valid uuid") String userId) {
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

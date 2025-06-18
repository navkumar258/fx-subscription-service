package com.example.fx.subscription.service.ai.tool;

import com.example.fx.subscription.service.dto.SubscriptionCreateRequest;
import com.example.fx.subscription.service.dto.SubscriptionUpdateRequest;
import com.example.fx.subscription.service.model.Subscription;
import com.example.fx.subscription.service.service.SubscriptionsService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FxSubscriptionTool {

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
    SubscriptionCreateRequest newSubscription = new SubscriptionCreateRequest();
    newSubscription.setUserId(userId);
    newSubscription.setCurrencyPair(currencyPair);
    newSubscription.setThreshold(BigDecimal.valueOf(thresholdValue));
    newSubscription.setDirection(direction);
    newSubscription.setNotificationChannels(Collections.singletonList(notificationMethod));

    Subscription savedSubscription = subscriptionService.createSubscription(newSubscription);
    return "Subscription for " + savedSubscription.getCurrencyPair() +
            " at threshold " + savedSubscription.getThreshold() +
            " with direction " + savedSubscription.getDirection() +
            " created successfully. Your subscription ID is: " + savedSubscription.getId() +
            ". Notifications via " + savedSubscription.getNotificationsChannels() + ".";
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
                                       @ToolParam(description = "New threshold value") double newThresholdValue,
                                       @ToolParam(description = "New notification methods", required = false) String newNotificationMethod) {
    Optional<Subscription> oldSubscription = subscriptionService.findSubscriptionById(subscriptionId);

    if (oldSubscription.isPresent()) {
      SubscriptionUpdateRequest newSubscription = new SubscriptionUpdateRequest();
      newSubscription.setThreshold(BigDecimal.valueOf(newThresholdValue));
      newSubscription.setNotificationChannels(Collections.singletonList(newNotificationMethod));
      Subscription updatedSub = subscriptionService.updateSubscriptionById(oldSubscription.get(), newSubscription);
      return "Subscription: " + subscriptionId
              + " updated successfully. New threshold: "
              + updatedSub.getThreshold()
              + ", New notification methods: "
              + updatedSub.getNotificationsChannels() + ".";
    }

    return "Subscription " + subscriptionId + " not found or no valid updates provided.";
  }

  /**
   * AI Tool: Deletes an FX rate subscription.
   * Delegates to the core SubscriptionService.
   */
  @Tool(name = "deleteFxSubscription",
          description = "Deletes an existing FX rate subscription using its ID."
  )
  public String deleteSubscriptionTool(@ToolParam(description = "Subscription id to delete") String subscriptionId) {
    subscriptionService.deleteSubscriptionById(subscriptionId);

    return "Subscription " + subscriptionId + " deleted successfully.";
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
    List<Subscription> subscriptions = subscriptionService.findSubscriptionsByUserId(userId);

    if (subscriptions.isEmpty()) {
      return "No active subscriptions found for user " + userId + ".";
    }

    return subscriptions.stream()
            .map(sub -> String.format("ID: %s, Pair: %s, Threshold: %.4f, Notify via: %s",
                    sub.getId(), sub.getCurrencyPair(), sub.getThreshold(), sub.getNotificationsChannels()))
            .collect(Collectors.joining("\n"));
  }
}

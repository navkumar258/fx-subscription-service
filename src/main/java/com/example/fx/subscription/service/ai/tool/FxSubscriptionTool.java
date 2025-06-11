package com.example.fx.subscription.service.ai.tool;

import com.example.fx.subscription.service.model.FXUser;
import com.example.fx.subscription.service.model.Subscription;
import com.example.fx.subscription.service.service.SubscriptionsService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
  @Tool(name = "createFxSubscription", description = "Creates a new FX rate subscription for a user with a specified currency pair, threshold, and preferred notification method.")
  public String createSubscriptionTool(String userId, String currencyPair, double thresholdValue, String notificationMethod) {
    Subscription newSubscription = new Subscription();
    newSubscription.setUser(new FXUser(UUID.fromString(userId)));
    newSubscription.setCurrencyPair(currencyPair);
    newSubscription.setThreshold(BigDecimal.valueOf(thresholdValue));
    newSubscription.setNotificationsChannels(Collections.singletonList(notificationMethod));

    Subscription savedSubscription = subscriptionService.createSubscription(newSubscription);
    return "Subscription for " + savedSubscription.getCurrencyPair() +
            " at threshold " + savedSubscription.getThreshold() +
            " created successfully. Your subscription ID is: " + savedSubscription.getId() +
            ". Notifications via " + savedSubscription.getNotificationsChannels() + ".";
  }

  /**
   * AI Tool: Updates an existing FX rate subscription.
   * Delegates to the core SubscriptionService after adapting parameters.
   */
  @Tool(name = "updateFxSubscription", description = "Updates the threshold and/or notification method of an existing FX rate subscription using its ID. At least one of newThresholdValue or newNotificationMethod must be provided.")
  public String updateSubscriptionTool(String subscriptionId, double newThresholdValue, String newNotificationMethod) {
    Optional<Subscription> oldSubscription = subscriptionService.findSubscriptionById(subscriptionId);

    if (oldSubscription.isPresent()) {
      Subscription newSubscription = new Subscription();
      newSubscription.setThreshold(BigDecimal.valueOf(newThresholdValue));
      newSubscription.setNotificationsChannels(Collections.singletonList(newNotificationMethod));
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
  @Tool(name = "deleteFxSubscription", description = "Deletes an existing FX rate subscription using its ID.")
  public String deleteSubscriptionTool(String subscriptionId) {
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
                  """)
  public String getFxSubscriptionsForUserTool(String userId) {
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

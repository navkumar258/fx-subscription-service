package com.example.fx.subscription.service.controller;

import com.example.fx.subscription.service.dto.SubscriptionCreateRequest;
import com.example.fx.subscription.service.dto.SubscriptionUpdateRequest;
import com.example.fx.subscription.service.exception.SubscriptionNotFoundException;
import com.example.fx.subscription.service.model.Subscription;
import com.example.fx.subscription.service.service.SubscriptionsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
public class SubscriptionsController {

  private static final String SUBSCRIPTION_NOT_FOUND_MESSAGE = "No Subscriptions found for the ID: %s, " +
          "please try with a different id!";
  private final SubscriptionsService subscriptionsService;

  public SubscriptionsController(SubscriptionsService subscriptionsService) {
    this.subscriptionsService = subscriptionsService;
  }

  @GetMapping(path = "/subscriptions/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> getSubscriptionById(@PathVariable String id) {
    Optional<Subscription> subscription = subscriptionsService.findSubscriptionById(id);

    if (subscription.isPresent()) {
      return ResponseEntity.ok().body(subscription);
    }

    throw new SubscriptionNotFoundException(SUBSCRIPTION_NOT_FOUND_MESSAGE.formatted(id));
  }

  @GetMapping(path = "/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> getSubscriptionsByUserId(@RequestParam String userId) {
    List<Subscription> subscriptions = subscriptionsService.findSubscriptionsByUserId(userId);

    if (CollectionUtils.isEmpty(subscriptions)) {
      throw new SubscriptionNotFoundException(
              "No Subscriptions found for the user ID: %s, please try with a different user!".formatted(userId)
      );
    }

    return ResponseEntity.ok().body(subscriptions);
  }

  @PostMapping(path = "/subscriptions", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createSubscription(@Valid @RequestBody SubscriptionCreateRequest subscriptionCreateRequest) {
    return ResponseEntity.ok().body(subscriptionsService.createSubscription(subscriptionCreateRequest));
  }

  @PutMapping(path = "/subscriptions/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Subscription> updateSubscriptionById(@PathVariable String id,
                                                             @Valid @RequestBody SubscriptionUpdateRequest subscriptionUpdateRequest) {
    Optional<Subscription> oldSubscription = subscriptionsService.findSubscriptionById(id);

    return oldSubscription
            .map(subscription ->
                    ResponseEntity
                            .ok()
                            .body(subscriptionsService.updateSubscriptionById(subscription, subscriptionUpdateRequest)))
            .orElseThrow(() -> new SubscriptionNotFoundException(SUBSCRIPTION_NOT_FOUND_MESSAGE.formatted(id)));
  }

  @DeleteMapping("/subscriptions/{id}")
  public ResponseEntity<Object> deleteSubscriptionById(@PathVariable String id) {
    Optional<Subscription> subscription = subscriptionsService.findSubscriptionById(id);

    if(subscription.isPresent()) {
      subscriptionsService.deleteSubscriptionById(id);
      return ResponseEntity.ok().build();
    }

    throw new SubscriptionNotFoundException(SUBSCRIPTION_NOT_FOUND_MESSAGE.formatted(id));
  }
}

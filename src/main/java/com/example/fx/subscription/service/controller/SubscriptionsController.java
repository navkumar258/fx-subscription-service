package com.example.fx.subscription.service.controller;

import com.example.fx.subscription.service.model.Subscription;
import com.example.fx.subscription.service.service.SubscriptionsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class SubscriptionsController {

  private final SubscriptionsService subscriptionsService;

  public SubscriptionsController(SubscriptionsService subscriptionsService) {
    this.subscriptionsService = subscriptionsService;
  }

  @GetMapping(path = "/subscriptions/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> getSubscriptionById(@PathVariable String id) {
    Optional<Subscription> subscription = subscriptionsService.findSubscriptionById(id);

    if (subscription.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok().body(subscription);
  }

  @GetMapping(path = "/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> getSubscriptionsByUserId(@RequestParam String userId) {
    List<Subscription> subscriptions = subscriptionsService.findSubscriptionsByUserId(userId);

    if (CollectionUtils.isEmpty(subscriptions)) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok().body(subscriptions);
  }

  @PostMapping(path = "/subscriptions", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createSubscription(@RequestBody Subscription subscriptionCreateRequest) {
    return ResponseEntity.ok().body(subscriptionsService.createSubscription(subscriptionCreateRequest));
  }

  @PutMapping(path = "/subscriptions/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Subscription> updateSubscriptionById(@PathVariable String id, @RequestBody Subscription subscriptionUpdateRequest) {
    Optional<Subscription> oldSubscription = subscriptionsService.findSubscriptionById(id);

    return oldSubscription
            .map(subscription ->
                    ResponseEntity
                            .ok()
                            .body(subscriptionsService.updateSubscriptionById(subscription, subscriptionUpdateRequest)))
            .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @DeleteMapping("/subscriptions/{id}")
  public ResponseEntity<Object> deleteSubscriptionById(@PathVariable String id) {
    Optional<Subscription> subscription = subscriptionsService.findSubscriptionById(id);

    if(subscription.isPresent()) {
      subscriptionsService.deleteSubscriptionById(id);
      return ResponseEntity.ok().build();
    }

    return ResponseEntity.notFound().build();
  }
}

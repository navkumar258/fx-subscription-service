package com.example.fx.subscription.service.controller;

import com.example.fx.subscription.service.dto.subscription.*;
import com.example.fx.subscription.service.exception.SubscriptionNotFoundException;
import com.example.fx.subscription.service.model.FxUser;
import com.example.fx.subscription.service.model.Subscription;
import com.example.fx.subscription.service.service.SubscriptionsService;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/subscriptions")
@Observed(name = "subscriptions.controller")
public class SubscriptionsController {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionsController.class);
  private static final String SUBSCRIPTION_NOT_FOUND_MESSAGE = "No Subscriptions found for the ID: %s, " +
          "please try with a different id!";
  private final SubscriptionsService subscriptionsService;

  public SubscriptionsController(SubscriptionsService subscriptionsService) {
    this.subscriptionsService = subscriptionsService;
  }

  @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN') or @subscriptionsService.isSubscriptionOwner(#id, authentication.principal.id)")
  public ResponseEntity<SubscriptionResponse> getSubscriptionById(@PathVariable String id) {
    Optional<SubscriptionResponse> subscription = subscriptionsService.findSubscriptionById(id);

    if (subscription.isPresent()) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Retrieved subscription: subscriptionId={}", id);
      }
      return ResponseEntity.ok(subscription.get());
    }

    throw new SubscriptionNotFoundException(SUBSCRIPTION_NOT_FOUND_MESSAGE.formatted(id), id);
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id.toString()")
  public ResponseEntity<SubscriptionListResponse> getSubscriptionsByUserId(@RequestParam String userId) {
    List<SubscriptionResponse> subscriptions = subscriptionsService.findSubscriptionResponsesByUserId(userId);

    if (CollectionUtils.isEmpty(subscriptions)) {
      String message = "No Subscriptions found for the user ID: %s, please try with a different user!".formatted(userId);
      throw new SubscriptionNotFoundException(message, userId, "SUBSCRIPTIONS_NOT_FOUND");
    }

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Retrieved {} subscriptions for user: userId={}", subscriptions.size(), userId);
    }

    return ResponseEntity.ok(new SubscriptionListResponse(subscriptions, subscriptions.size()));
  }

  @GetMapping(path = "/my", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<SubscriptionListResponse> getMySubscriptions(@AuthenticationPrincipal FxUser currentUser) {
    List<SubscriptionResponse> subscriptions = subscriptionsService.findSubscriptionResponsesByUserId(currentUser.getId().toString());

    if (CollectionUtils.isEmpty(subscriptions)) {
      String message = "No Subscriptions found for your account";
      throw new SubscriptionNotFoundException(message, currentUser.getId().toString(), "SUBSCRIPTIONS_NOT_FOUND");
    }

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Retrieved {} subscriptions for current user: userId={}", subscriptions.size(), currentUser.getId());
    }

    return ResponseEntity.ok(new SubscriptionListResponse(subscriptions, subscriptions.size()));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<SubscriptionCreateResponse> createSubscription(@AuthenticationPrincipal FxUser currentUser,
                                                                       @Valid @RequestBody SubscriptionCreateRequest subscriptionCreateRequest) {
    Subscription createdSubscription = subscriptionsService.createSubscription(subscriptionCreateRequest, currentUser.getId());
    
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Created subscription: subscriptionId={}, userId={}, currencyPair={}", 
          createdSubscription.getId(), currentUser.getId(), subscriptionCreateRequest.currencyPair());
    }
    
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(SubscriptionCreateResponse.fromSubscription(createdSubscription));
  }

  @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN') or @subscriptionsService.isSubscriptionOwner(#id, authentication.principal.id)")
  public ResponseEntity<SubscriptionUpdateResponse> updateSubscriptionById(@PathVariable String id,
                                                                           @Valid @RequestBody SubscriptionUpdateRequest subscriptionUpdateRequest) {
    Optional<Subscription> oldSubscription = subscriptionsService.findSubscriptionEntityById(id);

    return oldSubscription
            .map(subscription -> {
              Subscription updatedSubscription = subscriptionsService.updateSubscriptionById(subscription, subscriptionUpdateRequest);
              
              if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Updated subscription: subscriptionId={}, currencyPair={}", 
                    id, subscriptionUpdateRequest.currencyPair());
              }
              
              return ResponseEntity.ok(SubscriptionUpdateResponse.fromSubscription(updatedSubscription));
            })
            .orElseThrow(() -> new SubscriptionNotFoundException(SUBSCRIPTION_NOT_FOUND_MESSAGE.formatted(id), id));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or @subscriptionsService.isSubscriptionOwner(#id, authentication.principal.id)")
  public ResponseEntity<Void> deleteSubscriptionById(@PathVariable String id) {
      subscriptionsService.deleteSubscriptionById(id);

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Deleted subscription: subscriptionId={}", id);
      }
      return ResponseEntity.noContent().build();
  }

  @GetMapping(path = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SubscriptionListResponse> getAllSubscriptions() {
    List<SubscriptionResponse> allSubscriptions = subscriptionsService.findAllSubscriptionResponses();
    
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Admin retrieved all subscriptions: count={}", allSubscriptions.size());
    }
    
    return ResponseEntity.ok(new SubscriptionListResponse(allSubscriptions, allSubscriptions.size()));
  }
}

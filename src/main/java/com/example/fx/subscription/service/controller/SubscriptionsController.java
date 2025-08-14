package com.example.fx.subscription.service.controller;

import com.example.fx.subscription.service.dto.subscription.*;
import com.example.fx.subscription.service.exception.SubscriptionNotFoundException;
import com.example.fx.subscription.service.model.FxUser;
import com.example.fx.subscription.service.service.SubscriptionsService;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
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
    SubscriptionResponse response = subscriptionsService.findSubscriptionById(id)
            .orElseThrow(() -> new SubscriptionNotFoundException(SUBSCRIPTION_NOT_FOUND_MESSAGE.formatted(id), id));

    LOGGER.info("Retrieved subscription: subscriptionId={}", id);

    return ResponseEntity.ok(response);
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id.toString()")
  public ResponseEntity<SubscriptionListResponse> getSubscriptionsByUserId(@RequestParam String userId) {
    SubscriptionListResponse subscriptionListResponse = subscriptionsService.findSubscriptionResponsesByUserId(userId);

    LOGGER.info("Retrieved {} subscriptions for user: userId={}",
            subscriptionListResponse.totalCount(), userId);

    return ResponseEntity.ok(subscriptionListResponse);
  }

  @GetMapping(path = "/my", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<SubscriptionListResponse> getMySubscriptions(@AuthenticationPrincipal FxUser currentUser) {
    SubscriptionListResponse subscriptionListResponse = subscriptionsService.findSubscriptionResponsesByUserId(currentUser.getId().toString());

    LOGGER.info("Retrieved {} subscriptions for current user: userId={}",
            subscriptionListResponse.totalCount(), currentUser.getId());

    return ResponseEntity.ok(subscriptionListResponse);
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<SubscriptionCreateResponse> createSubscription(@AuthenticationPrincipal FxUser currentUser,
                                                                       @Valid @RequestBody SubscriptionCreateRequest subscriptionCreateRequest) {
    SubscriptionResponse createdSubscription = subscriptionsService.createSubscription(subscriptionCreateRequest, currentUser.getId());

    LOGGER.info("Created subscription: subscriptionId={}, userId={}, currencyPair={}",
            createdSubscription.id(), currentUser.getId(), subscriptionCreateRequest.currencyPair());

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new SubscriptionCreateResponse(
                    createdSubscription.id(),
                    "Subscription created successfully!",
                    createdSubscription
            ));
  }

  @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN') or @subscriptionsService.isSubscriptionOwner(#id, authentication.principal.id)")
  public ResponseEntity<SubscriptionUpdateResponse> updateSubscriptionById(@PathVariable String id,
                                                                           @Valid @RequestBody SubscriptionUpdateRequest subscriptionUpdateRequest) {
    SubscriptionResponse updatedSubscription = subscriptionsService.updateSubscriptionById(id, subscriptionUpdateRequest);

    LOGGER.info("Updated subscription: subscriptionId={}, currencyPair={}",
            id, subscriptionUpdateRequest.currencyPair());

    return ResponseEntity.ok(new SubscriptionUpdateResponse(
            updatedSubscription.id(),
            "Subscription updated successfully!",
            updatedSubscription));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or @subscriptionsService.isSubscriptionOwner(#id, authentication.principal.id)")
  public ResponseEntity<SubscriptionDeleteResponse> deleteSubscriptionById(@PathVariable String id) {
    SubscriptionDeleteResponse response = subscriptionsService.deleteSubscriptionById(id);

    LOGGER.info("Deleted subscription: subscriptionId={}", id);

    return ResponseEntity.ok(response);
  }

  @GetMapping(path = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SubscriptionListResponse> getAllSubscriptions() {
    List<SubscriptionResponse> allSubscriptions = subscriptionsService.findAllSubscriptionResponses();

    LOGGER.info("Admin retrieved all subscriptions: count={}", allSubscriptions.size());

    return ResponseEntity.ok(new SubscriptionListResponse(allSubscriptions, allSubscriptions.size()));
  }
}

package com.example.fx.subscription.service.controller;

import com.example.fx.subscription.service.dto.subscription.*;
import com.example.fx.subscription.service.model.FxUser;
import com.example.fx.subscription.service.service.SubscriptionsService;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.fx.subscription.service.util.LogSanitizer.sanitizeForLog;

@RestController
@RequestMapping("/api/v1/subscriptions")
@Observed(name = "subscriptions.controller")
public class SubscriptionsController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionsController.class);

	private final SubscriptionsService subscriptionsService;

	public SubscriptionsController(SubscriptionsService subscriptionsService) {
		this.subscriptionsService = subscriptionsService;
	}

	@GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('ADMIN') or @subscriptionsService.isSubscriptionOwner(#id, authentication.principal.id)")
	public SubscriptionResponse getSubscriptionById(@PathVariable String id) {
		SubscriptionResponse response = subscriptionsService.findSubscriptionById(id);

		LOGGER.atInfo().log("Retrieved subscription: subscriptionId={}", sanitizeForLog(id));

		return response;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id.toString()")
	public SubscriptionListResponse getSubscriptionsByUserId(@RequestParam String userId) {
		SubscriptionListResponse subscriptionListResponse = subscriptionsService.findSubscriptionResponsesByUserId(userId);

		LOGGER.atInfo().log("Retrieved {} subscriptions for user: userId={}",
				subscriptionListResponse.totalCount(), sanitizeForLog(userId));

		return subscriptionListResponse;
	}

	@GetMapping(path = "/my", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("isAuthenticated()")
	public SubscriptionListResponse getMySubscriptions(@AuthenticationPrincipal FxUser currentUser) {
		SubscriptionListResponse subscriptionListResponse = subscriptionsService.findSubscriptionResponsesByUserId(currentUser.getId().toString());

		LOGGER.info("Retrieved {} subscriptions for current user: userId={}",
				subscriptionListResponse.totalCount(), currentUser.getId());

		return subscriptionListResponse;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("isAuthenticated()")
	public SubscriptionCreateResponse createSubscription(@AuthenticationPrincipal FxUser currentUser,
	                                                     @Valid @RequestBody SubscriptionCreateRequest subscriptionCreateRequest) {
		SubscriptionResponse createdSubscription = subscriptionsService.createSubscription(subscriptionCreateRequest, currentUser.getId());

		LOGGER.info("Created subscription: subscriptionId={}, userId={}, currencyPair={}",
				createdSubscription.id(), currentUser.getId(), subscriptionCreateRequest.currencyPair());

		return new SubscriptionCreateResponse(
				createdSubscription.id(),
				"Subscription created successfully!",
				createdSubscription
		);
	}

	@PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('ADMIN') or @subscriptionsService.isSubscriptionOwner(#id, authentication.principal.id)")
	public SubscriptionUpdateResponse updateSubscriptionById(@PathVariable String id,
	                                                         @Valid @RequestBody SubscriptionUpdateRequest subscriptionUpdateRequest) {
		SubscriptionResponse updatedSubscription = subscriptionsService.updateSubscriptionById(id, subscriptionUpdateRequest);

		LOGGER.atInfo().log("Updated subscription: subscriptionId={}, currencyPair={}",
				sanitizeForLog(id), subscriptionUpdateRequest.currencyPair());

		return new SubscriptionUpdateResponse(
				updatedSubscription.id(),
				"Subscription updated successfully!",
				updatedSubscription);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN') or @subscriptionsService.isSubscriptionOwner(#id, authentication.principal.id)")
	public SubscriptionDeleteResponse deleteSubscriptionById(@PathVariable String id) {
		SubscriptionDeleteResponse response = subscriptionsService.deleteSubscriptionById(id);

		LOGGER.atInfo().log("Deleted subscription: subscriptionId={}", sanitizeForLog(id));

		return response;
	}

	@GetMapping(path = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('ADMIN')")
	public SubscriptionListResponse getAllSubscriptions() {
		List<SubscriptionResponse> allSubscriptions = subscriptionsService.findAllSubscriptionResponses();

		LOGGER.info("Admin retrieved all subscriptions: count={}", allSubscriptions.size());

		return new SubscriptionListResponse(allSubscriptions, allSubscriptions.size());
	}
}

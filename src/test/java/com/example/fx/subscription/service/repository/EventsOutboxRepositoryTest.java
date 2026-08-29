package com.example.fx.subscription.service.repository;

import com.example.fx.subscription.service.ai.tool.FxSubscriptionTool;
import com.example.fx.subscription.service.dto.subscription.SubscriptionResponse;
import com.example.fx.subscription.service.helper.PostgresTestContainerConfig;
import com.example.fx.subscription.service.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(PostgresTestContainerConfig.class)
class EventsOutboxRepositoryTest {

	@MockitoBean
	private FxSubscriptionTool fxSubscriptionTool;

	@Autowired
	private EventsOutboxRepository eventsOutboxRepository;

	private EventsOutbox testEventsOutbox;
	private Subscription testSubscription;

	@BeforeEach
	void setUp() {
		// Create test subscription
		testSubscription = new Subscription();
		testSubscription.setId(UUID.randomUUID());
		testSubscription.setCurrencyPair("GBP/USD");
		testSubscription.setThreshold(BigDecimal.valueOf(1.25));
		testSubscription.setDirection(ThresholdDirection.ABOVE);
		testSubscription.setStatus(SubscriptionStatus.ACTIVE);
		testSubscription.setCreatedAt(Instant.now());

		// Create test outbox event
		testEventsOutbox = new EventsOutbox();
		testEventsOutbox.setAggregateType("Subscription");
		testEventsOutbox.setAggregateId(UUID.randomUUID());
		testEventsOutbox.setEventType("SubscriptionCreated");
		testEventsOutbox.setPayload(SubscriptionResponse.fromSubscription(testSubscription));
		testEventsOutbox.setStatus(EventsOutboxStatus.PENDING);
		testEventsOutbox.setTimestamp(System.currentTimeMillis());
	}

	@AfterEach
	void tearDown() {
		eventsOutboxRepository.deleteAll();
	}

	@Test
	void save_ShouldPersistEventsOutbox() {
		// When
		EventsOutbox savedOutbox = eventsOutboxRepository.save(testEventsOutbox);

		// Then
		assertNotNull(savedOutbox.getId());
		assertEquals(EventsOutboxStatus.PENDING, savedOutbox.getStatus());
		assertEquals("SubscriptionCreated", savedOutbox.getEventType());

		// Verify it's in the database
		Optional<EventsOutbox> foundOutbox = eventsOutboxRepository.findById(savedOutbox.getId());
		assertTrue(foundOutbox.isPresent());
		assertEquals(EventsOutboxStatus.PENDING, foundOutbox.get().getStatus());
	}

	@Test
	void findById_WhenExists_ShouldReturnEventsOutbox() {
		// Given
		EventsOutbox savedOutbox = eventsOutboxRepository.save(testEventsOutbox);

		// When
		Optional<EventsOutbox> foundOutbox = eventsOutboxRepository.findById(savedOutbox.getId());

		// Then
		assertTrue(foundOutbox.isPresent());
		assertEquals(savedOutbox.getId(), foundOutbox.get().getId());
		assertEquals(EventsOutboxStatus.PENDING, foundOutbox.get().getStatus());
	}

	@Test
	void findById_WhenNotExists_ShouldReturnEmpty() {
		// When
		Optional<EventsOutbox> foundOutbox = eventsOutboxRepository.findById(UUID.randomUUID());

		// Then
		assertFalse(foundOutbox.isPresent());
	}

	@Test
	void findByStatus_ShouldReturnMatchingEvents() {
		// Given
		createTestOutbox("PENDING");
		createTestOutbox("PENDING");
		createTestOutbox("SENT");
		createTestOutbox("FAILED");

		// When
		List<EventsOutbox> pendingEvents = eventsOutboxRepository.findByStatus(EventsOutboxStatus.PENDING);
		List<EventsOutbox> sentEvents = eventsOutboxRepository.findByStatus(EventsOutboxStatus.SENT);
		List<EventsOutbox> failedEvents = eventsOutboxRepository.findByStatus(EventsOutboxStatus.FAILED);

		// Then
		assertEquals(2, pendingEvents.size());
		assertEquals(1, sentEvents.size());
		assertEquals(1, failedEvents.size());

		assertTrue(pendingEvents.stream().allMatch(e -> EventsOutboxStatus.PENDING.equals(e.getStatus())));
		assertTrue(sentEvents.stream().allMatch(e -> EventsOutboxStatus.SENT.equals(e.getStatus())));
		assertTrue(failedEvents.stream().allMatch(e -> EventsOutboxStatus.FAILED.equals(e.getStatus())));
	}

	@Test
	void updateStatus_ShouldUpdateEventsOutboxStatus() {
		// Given
		EventsOutbox savedOutbox = eventsOutboxRepository.save(testEventsOutbox);
		assertEquals(EventsOutboxStatus.PENDING, savedOutbox.getStatus());

		// When
		savedOutbox.setStatus(EventsOutboxStatus.SENT);
		EventsOutbox updatedOutbox = eventsOutboxRepository.save(savedOutbox);

		// Then
		assertEquals(EventsOutboxStatus.SENT, updatedOutbox.getStatus());

		// Verify in database
		Optional<EventsOutbox> foundOutbox = eventsOutboxRepository.findById(savedOutbox.getId());
		assertTrue(foundOutbox.isPresent());
		assertEquals(EventsOutboxStatus.SENT, foundOutbox.get().getStatus());
	}

	@Test
	void delete_ShouldRemoveEventsOutbox() {
		// Given
		EventsOutbox savedOutbox = eventsOutboxRepository.save(testEventsOutbox);
		assertTrue(eventsOutboxRepository.findById(savedOutbox.getId()).isPresent());

		// When
		eventsOutboxRepository.delete(savedOutbox);

		// Then
		assertFalse(eventsOutboxRepository.findById(savedOutbox.getId()).isPresent());
	}

	@Test
	void findAll_ShouldReturnAllEvents() {
		// Given
		EventsOutbox outbox1 = createTestOutbox("PENDING");
		EventsOutbox outbox2 = createTestOutbox("SENT");
		EventsOutbox outbox3 = createTestOutbox("PENDING");

		// When
		List<EventsOutbox> allEvents = eventsOutboxRepository.findAll();

		// Then
		assertTrue(allEvents.size() >= 3);
		assertTrue(allEvents.stream().anyMatch(e -> e.getId().equals(outbox1.getId())));
		assertTrue(allEvents.stream().anyMatch(e -> e.getId().equals(outbox2.getId())));
		assertTrue(allEvents.stream().anyMatch(e -> e.getId().equals(outbox3.getId())));
	}

	@Test
	void findByStatus_WhenNoMatchingStatus_ShouldReturnEmptyList() {
		// Given
		createTestOutbox("PENDING");
		createTestOutbox("SENT");

		// When
		List<EventsOutbox> failedEvents = eventsOutboxRepository.findByStatus(EventsOutboxStatus.FAILED);

		// Then
		assertTrue(failedEvents.isEmpty());
	}

	private EventsOutbox createTestOutbox(String status) {
		EventsOutbox outbox = new EventsOutbox();
		outbox.setAggregateType("Subscription");
		outbox.setAggregateId(UUID.randomUUID());
		outbox.setEventType("SubscriptionCreated");
		outbox.setPayload(SubscriptionResponse.fromSubscription(testSubscription));
		outbox.setStatus(EventsOutboxStatus.valueOf(status));
		outbox.setTimestamp(System.currentTimeMillis());
		return eventsOutboxRepository.save(outbox);
	}
} 
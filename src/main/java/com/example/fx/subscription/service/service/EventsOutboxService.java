package com.example.fx.subscription.service.service;

import com.example.fx.subscription.service.model.EventsOutbox;
import com.example.fx.subscription.service.repository.EventsOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EventsOutboxService {

  private final EventsOutboxRepository eventsOutboxRepository;

  public EventsOutboxService(EventsOutboxRepository eventsOutboxRepository) {
    this.eventsOutboxRepository = eventsOutboxRepository;
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void updateOutboxStatus(String outboxId, String newStatus) {
    EventsOutbox outbox = eventsOutboxRepository.findById(UUID.fromString(outboxId))
            .orElseThrow(() -> new RuntimeException("Outbox event not found with id: " + outboxId));

    outbox.setStatus(newStatus);
    eventsOutboxRepository.save(outbox);
  }

  public EventsOutbox findOutboxById(String id) {
    return eventsOutboxRepository.findById(UUID.fromString(id))
            .orElseThrow(() -> new RuntimeException("Outbox event not found with id: " + id));
  }
}

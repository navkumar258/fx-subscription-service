package com.example.fx.subscription.service.repository;

import com.example.fx.subscription.service.model.EventsOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventsOutboxRepository extends JpaRepository<EventsOutbox, UUID> {

  List<EventsOutbox> findByStatus(String status);

}

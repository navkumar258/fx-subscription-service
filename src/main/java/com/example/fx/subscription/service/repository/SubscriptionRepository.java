package com.example.fx.subscription.service.repository;

import com.example.fx.subscription.service.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  List<Subscription> findAllByUserId(UUID userId);

}

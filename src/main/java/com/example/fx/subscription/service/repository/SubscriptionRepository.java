package com.example.fx.subscription.service.repository;

import com.example.fx.subscription.service.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  @Query("SELECT s FROM Subscription s JOIN FETCH s.user WHERE s.user.id = :userId")
  List<Subscription> findSubscriptionsByUserId(@Param("userId") UUID userId);

  boolean existsByIdAndUserId(UUID id, UUID userId);

}

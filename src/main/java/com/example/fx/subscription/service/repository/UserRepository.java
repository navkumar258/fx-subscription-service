package com.example.fx.subscription.service.repository;

import com.example.fx.subscription.service.model.FXUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<FXUser, UUID> {
}

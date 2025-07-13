package com.example.fx.subscription.service.repository;

import com.example.fx.subscription.service.model.FxUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FxUserRepository extends JpaRepository<FxUser, UUID> {

  Optional<FxUser> findByEmail(String email);
  boolean existsByEmail(String email);

  @Query("SELECT u FROM FxUser u WHERE " +
         "(:email IS NULL OR u.email LIKE %:email%) AND " +
         "(:mobile IS NULL OR u.mobile LIKE %:mobile%) AND " +
         "(:enabled IS NULL OR u.enabled = :enabled)")
  Page<FxUser> searchUsers(@Param("email") String email,
                           @Param("mobile") String mobile,
                           @Param("enabled") Boolean enabled,
                           Pageable pageable);
}

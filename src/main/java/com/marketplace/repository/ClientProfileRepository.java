package com.marketplace.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.marketplace.model.ClientProfile;
import com.marketplace.model.User;

@Repository
public interface ClientProfileRepository extends JpaRepository<ClientProfile, Long> {
    Optional<ClientProfile> findByUser(User user);
    boolean existsByUser(User user);
    Optional<ClientProfile> findByUserId(Long userId);
}
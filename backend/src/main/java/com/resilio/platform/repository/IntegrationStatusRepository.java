package com.resilio.platform.repository;

import com.resilio.platform.model.IntegrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IntegrationStatusRepository extends JpaRepository<IntegrationStatus, Long> {
    Optional<IntegrationStatus> findByIntegrationKey(String integrationKey);
}

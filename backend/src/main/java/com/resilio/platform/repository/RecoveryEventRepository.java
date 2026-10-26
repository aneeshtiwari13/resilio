package com.resilio.platform.repository;

import com.resilio.platform.model.RecoveryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecoveryEventRepository extends JpaRepository<RecoveryEvent, Long> {
    List<RecoveryEvent> findAllByOrderByStartedAtDesc();
    Optional<RecoveryEvent> findTopByOrderByStartedAtDesc();
    long countByExecutionOutcome(String outcome);
    long countByVerificationResult(String result);
}

package com.resilio.platform.repository;

import com.resilio.platform.model.RecoveryOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecoveryOperationRepository extends JpaRepository<RecoveryOperation, Long> {

    Optional<RecoveryOperation> findByRecoveryAttemptId(String recoveryAttemptId);

    Optional<RecoveryOperation> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT r FROM RecoveryOperation r WHERE r.deploymentTarget = :target AND r.operationState IN ('ACQUIRED', 'IN_PROGRESS') AND r.leaseExpiresAt > :now ORDER BY r.createdAt DESC")
    List<RecoveryOperation> findActiveLocksForTarget(@Param("target") String target, @Param("now") Instant now);

    @Query("SELECT r FROM RecoveryOperation r WHERE r.operationState IN ('ACQUIRED', 'IN_PROGRESS') ORDER BY r.createdAt ASC")
    List<RecoveryOperation> findIncompleteOperations();

    List<RecoveryOperation> findAllByOrderByCreatedAtDesc();
}

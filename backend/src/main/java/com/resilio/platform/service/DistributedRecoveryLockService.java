package com.resilio.platform.service;

import com.resilio.platform.model.RecoveryOperation;
import com.resilio.platform.repository.RecoveryOperationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DistributedRecoveryLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedRecoveryLockService.class);

    private final RecoveryOperationRepository operationRepo;

    @Value("${resilio.recovery.lease-duration-seconds:60}")
    private long defaultLeaseSeconds;

    public DistributedRecoveryLockService(RecoveryOperationRepository operationRepo) {
        this.operationRepo = operationRepo;
    }

    public LockAcquisitionResult tryAcquireLock(String deploymentTarget, String lockOwner, String idempotencyKey) {
        return tryAcquireLock(deploymentTarget, "EXECUTE_ROLLBACK", lockOwner, idempotencyKey, null, null, null);
    }

    /**
     * Atomically attempts to acquire a distributed recovery lock for the specified target.
     * Supports idempotency: if an operation with the same idempotencyKey already exists, returns it.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LockAcquisitionResult tryAcquireLock(
            String deploymentTarget,
            String requestedAction,
            String lockOwner,
            String idempotencyKey,
            String incidentId,
            String expectedActiveRelease,
            String targetKnownGoodRelease
    ) {
        Instant now = Instant.now();

        // 1. Check for Idempotent Request
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<RecoveryOperation> existingIdempotent = operationRepo.findByIdempotencyKey(idempotencyKey);
            if (existingIdempotent.isPresent()) {
                RecoveryOperation op = existingIdempotent.get();
                log.info("Idempotent recovery request matched: key={}, attemptId={}, state={}",
                        idempotencyKey, op.getRecoveryAttemptId(), op.getOperationState());
                return new LockAcquisitionResult(op, true, true); // acquired=true, isReplay=true
            }
        }

        // 2. Check for active unexpired locks on the target deployment
        List<RecoveryOperation> activeLocks = operationRepo.findActiveLocksForTarget(deploymentTarget, now);

        if (!activeLocks.isEmpty()) {
            RecoveryOperation active = activeLocks.get(0);
            log.warn("CONFLICT: Recovery lock on target '{}' is currently held by owner '{}' (attemptId={}) until {}",
                    deploymentTarget, active.getLockOwner(), active.getRecoveryAttemptId(), active.getLeaseExpiresAt());

            throw new RecoveryLockConflictException(
                    "A recovery operation is already active for deployment target '" + deploymentTarget +
                            "'. Concurrent operation rejected to prevent split-brain state.",
                    active.getRecoveryAttemptId(),
                    active.getLockOwner()
            );
        }

        // 3. Clean up any stale/expired locks on this target
        cleanUpStaleLocks(deploymentTarget, now);

        // 4. Create and persist new RecoveryOperation with lease
        String attemptId = "REC-" + now.toEpochMilli() + "-" + (lockOwner != null ? lockOwner.hashCode() % 1000 : "0");
        Instant leaseExpiry = now.plus(Duration.ofSeconds(defaultLeaseSeconds));

        RecoveryOperation newOp = new RecoveryOperation(
                attemptId, incidentId, deploymentTarget, requestedAction,
                lockOwner, idempotencyKey, leaseExpiry,
                expectedActiveRelease, targetKnownGoodRelease
        );

        RecoveryOperation saved = operationRepo.save(newOp);
        log.info("Distributed recovery lock acquired: attemptId={}, target={}, owner={}, leaseExpiresAt={}",
                attemptId, deploymentTarget, lockOwner, leaseExpiry);

        return new LockAcquisitionResult(saved, true, false);
    }

    @Transactional
    public void completeLock(Long operationId, String resultSummary, boolean success) {
        operationRepo.findById(operationId).ifPresent(op -> {
            op.setOperationState(success ? "COMPLETED" : "FAILED");
            op.setFinalResult(resultSummary);
            op.setUpdatedAt(Instant.now());
            operationRepo.save(op);
            log.info("Distributed recovery lock released: attemptId={}, finalState={}",
                    op.getRecoveryAttemptId(), op.getOperationState());
        });
    }

    @Transactional
    public void abortLock(Long operationId, String reason) {
        operationRepo.findById(operationId).ifPresent(op -> {
            op.setOperationState("ABORTED");
            op.setFinalResult("Aborted: " + reason);
            op.setUpdatedAt(Instant.now());
            operationRepo.save(op);
            log.warn("Distributed recovery lock aborted: attemptId={}, reason={}", op.getRecoveryAttemptId(), reason);
        });
    }

    public boolean isLockActive(String deploymentTarget) {
        return !operationRepo.findActiveLocksForTarget(deploymentTarget, Instant.now()).isEmpty();
    }

    public List<RecoveryOperation> getRecentOperations() {
        return operationRepo.findAllByOrderByCreatedAtDesc();
    }

    private void cleanUpStaleLocks(String target, Instant now) {
        List<RecoveryOperation> all = operationRepo.findIncompleteOperations();
        for (RecoveryOperation op : all) {
            if (target.equalsIgnoreCase(op.getDeploymentTarget()) && op.isExpired()) {
                log.warn("Expiring stale recovery lease: attemptId={}, expiredAt={}", op.getRecoveryAttemptId(), op.getLeaseExpiresAt());
                op.setOperationState("ABORTED");
                op.setFinalResult("Lease expired before completion; marked ABORTED during lock acquisition.");
                op.setUpdatedAt(now);
                operationRepo.save(op);
            }
        }
    }

    public record LockAcquisitionResult(
            RecoveryOperation operation,
            boolean acquired,
            boolean isReplay
    ) {}
}

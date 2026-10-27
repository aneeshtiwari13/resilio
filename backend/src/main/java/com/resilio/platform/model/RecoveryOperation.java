package com.resilio.platform.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "recovery_operations", indexes = {
        @Index(name = "idx_rec_op_target_state", columnList = "deploymentTarget, operationState"),
        @Index(name = "idx_rec_op_idempotency", columnList = "idempotencyKey")
})
public class RecoveryOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String recoveryAttemptId; // e.g. REC-1793026326696

    private String incidentId;

    @Column(nullable = false)
    private String deploymentTarget; // e.g. PRODUCTION

    @Column(nullable = false)
    private String requestedAction; // e.g. EXECUTE_ROLLBACK

    private String lockOwner; // e.g. node-1:thread-2

    @Column(nullable = false)
    private String operationState; // ACQUIRED, IN_PROGRESS, COMPLETED, FAILED, ABORTED

    private String idempotencyKey;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant leaseExpiresAt;

    private String expectedActiveRelease;
    private String targetKnownGoodRelease;

    @Column(columnDefinition = "TEXT")
    private String finalResult;

    @Version
    private Long version;

    public RecoveryOperation() {}

    public RecoveryOperation(String recoveryAttemptId, String incidentId, String deploymentTarget,
                             String requestedAction, String lockOwner, String idempotencyKey,
                             Instant leaseExpiresAt, String expectedActiveRelease, String targetKnownGoodRelease) {
        this.recoveryAttemptId = recoveryAttemptId;
        this.incidentId = incidentId;
        this.deploymentTarget = deploymentTarget;
        this.requestedAction = requestedAction;
        this.lockOwner = lockOwner;
        this.idempotencyKey = idempotencyKey;
        this.operationState = "IN_PROGRESS";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.leaseExpiresAt = leaseExpiresAt;
        this.expectedActiveRelease = expectedActiveRelease;
        this.targetKnownGoodRelease = targetKnownGoodRelease;
    }

    public boolean isExpired() {
        return leaseExpiresAt != null && Instant.now().isAfter(leaseExpiresAt);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRecoveryAttemptId() { return recoveryAttemptId; }
    public void setRecoveryAttemptId(String recoveryAttemptId) { this.recoveryAttemptId = recoveryAttemptId; }
    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
    public String getDeploymentTarget() { return deploymentTarget; }
    public void setDeploymentTarget(String deploymentTarget) { this.deploymentTarget = deploymentTarget; }
    public String getRequestedAction() { return requestedAction; }
    public void setRequestedAction(String requestedAction) { this.requestedAction = requestedAction; }
    public String getLockOwner() { return lockOwner; }
    public void setLockOwner(String lockOwner) { this.lockOwner = lockOwner; }
    public String getOperationState() { return operationState; }
    public void setOperationState(String operationState) { this.operationState = operationState; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
    public void setLeaseExpiresAt(Instant leaseExpiresAt) { this.leaseExpiresAt = leaseExpiresAt; }
    public String getExpectedActiveRelease() { return expectedActiveRelease; }
    public void setExpectedActiveRelease(String expectedActiveRelease) { this.expectedActiveRelease = expectedActiveRelease; }
    public String getTargetKnownGoodRelease() { return targetKnownGoodRelease; }
    public void setTargetKnownGoodRelease(String targetKnownGoodRelease) { this.targetKnownGoodRelease = targetKnownGoodRelease; }
    public String getFinalResult() { return finalResult; }
    public void setFinalResult(String finalResult) { this.finalResult = finalResult; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}

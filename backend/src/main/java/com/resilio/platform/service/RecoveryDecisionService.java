package com.resilio.platform.service;

import com.resilio.platform.model.Incident;
import com.resilio.platform.model.RecoveryOperation;
import com.resilio.platform.model.ReleaseRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class RecoveryDecisionService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryDecisionService.class);

    private final ReleaseRegistryService releaseRegistry;
    private final RoutingService routingService;
    private final RecoveryVerificationService verificationService;
    private final IncidentService incidentService;
    private final DistributedRecoveryLockService distributedLockService;

    private final ReentrantLock recoveryLock = new ReentrantLock();
    private final AtomicBoolean isRecoveryInProgress = new AtomicBoolean(false);

    @Value("${resilio.recovery.max-retries:2}")
    private int maxRetries;

    @Value("${resilio.recovery.cooldown-seconds:30}")
    private int cooldownSeconds;

    private Instant lastRecoveryTimestamp = Instant.EPOCH;

    public RecoveryDecisionService(
            ReleaseRegistryService releaseRegistry,
            RoutingService routingService,
            RecoveryVerificationService verificationService,
            IncidentService incidentService
    ) {
        this(releaseRegistry, routingService, verificationService, incidentService, null);
    }

    @Autowired
    public RecoveryDecisionService(
            ReleaseRegistryService releaseRegistry,
            RoutingService routingService,
            RecoveryVerificationService verificationService,
            IncidentService incidentService,
            @Autowired(required = false) DistributedRecoveryLockService distributedLockService
    ) {
        this.releaseRegistry = releaseRegistry;
        this.routingService = routingService;
        this.verificationService = verificationService;
        this.incidentService = incidentService;
        this.distributedLockService = distributedLockService;
    }

    public boolean isRecoveryRunning() {
        if (distributedLockService != null && distributedLockService.isLockActive("PRODUCTION")) {
            return true;
        }
        return isRecoveryInProgress.get();
    }

    public Incident evaluateAndExecute(
            String failureCategory,
            String failedStage,
            String errorEvidence,
            String candidateVersion,
            String jenkinsBuild,
            boolean isPostActivation
    ) {
        return evaluateAndExecute(failureCategory, failedStage, errorEvidence, candidateVersion, jenkinsBuild, isPostActivation, null, "resilio-system");
    }

    /**
     * Context-aware evaluation and execution of recovery rules with distributed locking and idempotency.
     */
    public Incident evaluateAndExecute(
            String failureCategory,
            String failedStage,
            String errorEvidence,
            String candidateVersion,
            String jenkinsBuild,
            boolean isPostActivation,
            String idempotencyKey,
            String lockOwner
    ) {
        log.info("RecoveryDecisionService: Evaluating category={}, stage={}, postActivation={}, idempotencyKey={}",
                failureCategory, failedStage, isPostActivation, idempotencyKey);

        Optional<ReleaseRecord> activeOpt = releaseRegistry.getActiveRelease();
        String activeReleaseId = activeOpt.map(ReleaseRecord::getReleaseId).orElse("unknown-active");

        // RULE 1: BUILD_FAILURE
        if ("BUILD_FAILURE".equalsIgnoreCase(failureCategory)) {
            log.info("[RULE 1] BUILD_FAILURE: Halting release before containerization. Active release untouched.");
            Incident inc = incidentService.createIncident(
                    jenkinsBuild, failureCategory, failedStage, errorEvidence,
                    candidateVersion, activeReleaseId,
                    "NO_DEPLOYMENT_REQUIRED",
                    "Block pipeline execution; preserve active release intact",
                    "Compilation or packaging failed. Candidate was not containerized or deployed."
            );
            Incident closed = incidentService.closeIncident(inc.getIncidentId(), "NO_DEPLOYMENT_REQUIRED", "Active release " + activeReleaseId + " maintained.");
            return closed != null ? closed : inc;
        }

        // RULE 2: UNIT_TEST_FAILURE
        if ("UNIT_TEST_FAILURE".equalsIgnoreCase(failureCategory)) {
            log.info("[RULE 2] UNIT_TEST_FAILURE: Stopping pipeline before deployment. Preserving active release.");
            Incident inc = incidentService.createIncident(
                    jenkinsBuild, failureCategory, failedStage, errorEvidence,
                    candidateVersion, activeReleaseId,
                    "NO_DEPLOYMENT_REQUIRED",
                    "Stop pipeline before deployment; publish JUnit test reports",
                    "Unit test assertion failed. Candidate rejected prior to deployment."
            );
            Incident closed = incidentService.closeIncident(inc.getIncidentId(), "NO_DEPLOYMENT_REQUIRED", "Active release " + activeReleaseId + " maintained.");
            return closed != null ? closed : inc;
        }

        // RULE 3: CONTAINER_STARTUP_FAILURE
        if ("CONTAINER_STARTUP_FAILURE".equalsIgnoreCase(failureCategory)) {
            log.info("[RULE 3] CONTAINER_STARTUP_FAILURE: Candidate failed to boot. Rejecting candidate.");
            Incident inc = incidentService.createIncident(
                    jenkinsBuild, failureCategory, failedStage, errorEvidence,
                    candidateVersion, activeReleaseId,
                    "CANDIDATE_REJECTED",
                    "Collect container logs; terminate failed candidate container",
                    "Candidate container crashed during startup. Production routing remained on active release."
            );
            Incident closed = incidentService.closeIncident(inc.getIncidentId(), "CANDIDATE_REJECTED", "Candidate failed startup. Active release untouched.");
            return closed != null ? closed : inc;
        }

        // RULE 4: SELENIUM_REGRESSION_FAILURE before traffic switch
        if ("SELENIUM_REGRESSION_FAILURE".equalsIgnoreCase(failureCategory) && !isPostActivation) {
            log.info("[RULE 4] SELENIUM_REGRESSION_FAILURE: Pre-promotion browser regression. Rejecting candidate.");
            Incident inc = incidentService.createIncident(
                    jenkinsBuild, failureCategory, failedStage, errorEvidence,
                    candidateVersion, activeReleaseId,
                    "CANDIDATE_REJECTED",
                    "Capture failure screenshots; abort promotion to production traffic",
                    "Candidate failed browser verification gate. Candidate rejected before traffic switch."
            );
            Incident closed = incidentService.closeIncident(inc.getIncidentId(), "CANDIDATE_REJECTED", "Active release " + activeReleaseId + " preserved.");
            return closed != null ? closed : inc;
        }

        // RULE 5: Failure after traffic switch (Post-Activation Health or Smoke Check Failure)
        if (isPostActivation) {
            log.warn("[RULE 5] Post-Activation failure detected! Evaluating automated rollback feasibility...");

            // Distributed Lock Guard & In-process lock guard
            if (!recoveryLock.tryLock()) {
                log.warn("Recovery operation already in progress locally! Rejecting concurrent execution.");
                Incident inc = incidentService.createIncident(
                        jenkinsBuild, failureCategory, failedStage, errorEvidence,
                        candidateVersion, activeReleaseId,
                        "RECOVERY_IN_PROGRESS",
                        "Wait for active recovery process to complete",
                        "Concurrent recovery conflict prevented by local mutex."
                );
                return inc;
            }

            RecoveryOperation distributedOp = null;
            if (distributedLockService != null) {
                try {
                    var lockResult = distributedLockService.tryAcquireLock(
                            "PRODUCTION",
                            "EXECUTE_ROLLBACK",
                            lockOwner != null ? lockOwner : "system-node",
                            idempotencyKey,
                            null,
                            activeReleaseId,
                            null
                    );

                    distributedOp = lockResult.operation();

                    if (lockResult.isReplay()) {
                        log.info("Idempotent replay of recovery operation: attemptId={}", distributedOp.getRecoveryAttemptId());
                        recoveryLock.unlock();
                        // Return existing incident if possible
                        return incidentService.getAllIncidents().stream().findFirst().orElse(null);
                    }
                } catch (RecoveryLockConflictException e) {
                    recoveryLock.unlock();
                    log.warn("Distributed lock conflict: {}", e.getMessage());
                    throw e;
                }
            }

            try {
                isRecoveryInProgress.set(true);

                // RULE 6: Safe recovery target check
                Optional<ReleaseRecord> targetOpt = releaseRegistry.getLatestKnownGood();
                if (targetOpt.isEmpty()) {
                    log.error("[RULE 6] CRITICAL: No verified known-good release exists in registry!");
                    Incident inc = incidentService.createIncident(
                            jenkinsBuild, failureCategory, failedStage, errorEvidence,
                            candidateVersion, activeReleaseId,
                            "MANUAL_INTERVENTION_REQUIRED",
                            "Halt automated retries; notify SRE team",
                            "Active release failed, but no previously verified known-good release was found in registry."
                    );
                    Incident closed = incidentService.closeIncident(inc.getIncidentId(), "MANUAL_INTERVENTION_REQUIRED", "No eligible known-good release found.");
                    if (distributedLockService != null && distributedOp != null) {
                        distributedLockService.completeLock(distributedOp.getId(), "MANUAL_INTERVENTION_REQUIRED: No known-good release found", false);
                    }
                    return closed != null ? closed : inc;
                }

                ReleaseRecord knownGood = targetOpt.get();
                String fallbackSlot = knownGood.getDeploymentSlot();
                log.info("Identified verified recovery target: releaseId={}, slot={}", knownGood.getReleaseId(), fallbackSlot);

                Incident inc = incidentService.createIncident(
                        jenkinsBuild, failureCategory, failedStage, errorEvidence,
                        candidateVersion, activeReleaseId,
                        "EXECUTE_ROLLBACK",
                        "Switch Nginx traffic back to " + fallbackSlot + " (" + knownGood.getReleaseId() + ")",
                        "Active application degraded after activation. Automatic rollback initiated."
                );

                if (distributedOp != null) {
                    distributedOp.setIncidentId(inc.getIncidentId());
                    distributedOp.setTargetKnownGoodRelease(knownGood.getReleaseId());
                }

                incidentService.recordTimelineEvent(
                        inc.getIncidentId(), "ROLLBACK_INITIATED", "Routing", "WARNING",
                        "Reverting Nginx upstream traffic to " + fallbackSlot,
                        "Target release: " + knownGood.getReleaseId()
                );

                // Execute atomic traffic switch
                boolean routingOk = routingService.switchUpstreamTraffic(fallbackSlot);
                if (!routingOk) {
                    // RULE 7: Recovery failure
                    log.error("[RULE 7] Rollback routing switch failed!");
                    incidentService.recordTimelineEvent(
                            inc.getIncidentId(), "ROUTING_FAILED", "Routing", "FAILURE",
                            "Failed to update Nginx upstream configuration",
                            "Preserving diagnostics for operator inspection"
                    );
                    Incident closed = incidentService.closeIncident(inc.getIncidentId(), "RECOVERY_FAILED", "Nginx routing reconfiguration failed.");
                    if (distributedLockService != null && distributedOp != null) {
                        distributedLockService.completeLock(distributedOp.getId(), "RECOVERY_FAILED: Upstream routing failed", false);
                    }
                    return closed != null ? closed : inc;
                }

                incidentService.recordTimelineEvent(
                        inc.getIncidentId(), "TRAFFIC_REVERTED", "Routing", "SUCCESS",
                        "Traffic successfully switched to slot " + fallbackSlot,
                        "Beginning 7-point independent post-recovery verification"
                );

                // Update registry mapping
                releaseRegistry.markRolledBack(activeReleaseId, knownGood.getReleaseId());

                // Post-recovery verification
                var audit = verificationService.verifyPostRecovery(knownGood.getVersionTag(), fallbackSlot);
                incidentService.recordTimelineEvent(
                        inc.getIncidentId(), "VERIFICATION_AUDIT", "Verification",
                        audit.allPassed() ? "SUCCESS" : "FAILURE",
                        "Post-recovery verification result: " + (audit.allPassed() ? "PASSED" : "FAILED"),
                        audit.details()
                );

                Incident closed;
                if (audit.allPassed()) {
                    closed = incidentService.closeIncident(inc.getIncidentId(), "RECOVERED", audit.details());
                    log.info("AUTOMATED ROLLBACK & VERIFICATION COMPLETED SUCCESSFULLY.");
                    if (distributedLockService != null && distributedOp != null) {
                        distributedLockService.completeLock(distributedOp.getId(), "RECOVERED: " + audit.details(), true);
                    }
                } else {
                    closed = incidentService.closeIncident(inc.getIncidentId(), "RECOVERY_FAILED", audit.details());
                    log.error("Rollback traffic switched but post-recovery verification failed!");
                    if (distributedLockService != null && distributedOp != null) {
                        distributedLockService.completeLock(distributedOp.getId(), "RECOVERY_FAILED: " + audit.details(), false);
                    }
                }

                lastRecoveryTimestamp = Instant.now();
                return closed != null ? closed : inc;

            } finally {
                isRecoveryInProgress.set(false);
                recoveryLock.unlock();
            }
        }

        // Default Fallback
        Incident inc = incidentService.createIncident(
                jenkinsBuild, failureCategory, failedStage, errorEvidence,
                candidateVersion, activeReleaseId,
                "MANUAL_INTERVENTION_REQUIRED",
                "Flag incident for review",
                "Unclassified failure condition requires operator assessment."
        );
        Incident closed = incidentService.closeIncident(inc.getIncidentId(), "MANUAL_INTERVENTION_REQUIRED", "Operator review required.");
        return closed != null ? closed : inc;
    }
}

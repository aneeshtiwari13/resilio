package com.resilio.platform.service;

import com.resilio.platform.model.Incident;
import com.resilio.platform.model.RecoveryOperation;
import com.resilio.platform.repository.IncidentRepository;
import com.resilio.platform.repository.RecoveryOperationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class RecoveryReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryReconciliationService.class);

    private final RecoveryOperationRepository operationRepo;
    private final IncidentRepository incidentRepo;
    private final ReleaseRegistryService releaseRegistry;
    private final RoutingService routingService;
    private final IncidentService incidentService;

    private boolean reconciliationCompleted = false;
    private String reconciliationSummary = "Pending startup reconciliation";

    public RecoveryReconciliationService(
            RecoveryOperationRepository operationRepo,
            IncidentRepository incidentRepo,
            ReleaseRegistryService releaseRegistry,
            RoutingService routingService,
            IncidentService incidentService
    ) {
        this.operationRepo = operationRepo;
        this.incidentRepo = incidentRepo;
        this.releaseRegistry = releaseRegistry;
        this.routingService = routingService;
        this.incidentService = incidentService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reconcileOnStartup() {
        log.info("=== STARTING RECOVERY & DEPLOYMENT STATE RECONCILIATION ===");

        // 1. Reconcile Routing and Baseline Release
        releaseRegistry.reconcileStateOnStartup();
        String activeSlot = routingService.getActiveSlot();
        log.info("Reconciliation: Current disk routing slot is: {}", activeSlot);

        // 2. Check for Incomplete Recovery Operations
        List<RecoveryOperation> incompleteOps = operationRepo.findIncompleteOperations();
        if (!incompleteOps.isEmpty()) {
            log.warn("Reconciliation: Found {} incomplete recovery operation(s) from prior session!", incompleteOps.size());

            for (RecoveryOperation op : incompleteOps) {
                log.warn("Reconciliation: Aborting interrupted recovery attempt: id={}, target={}, requestedAction={}",
                        op.getRecoveryAttemptId(), op.getDeploymentTarget(), op.getRequestedAction());

                op.setOperationState("ABORTED");
                op.setFinalResult("Interrupted by server restart/crash. Operation state safely reconciled and aborted by RecoveryReconciliationService on startup.");
                op.setUpdatedAt(Instant.now());
                operationRepo.save(op);

                // If associated incident is still IN_PROGRESS, escalate to manual intervention
                if (op.getIncidentId() != null) {
                    incidentRepo.findByIncidentId(op.getIncidentId()).ifPresent(inc -> {
                        if ("RECOVERY_IN_PROGRESS".equalsIgnoreCase(inc.getFinalStatus())) {
                            incidentService.recordTimelineEvent(
                                    inc.getIncidentId(), "RECONCILIATION_ABORTED", "Startup Reconciliation", "WARNING",
                                    "Recovery interrupted by server restart; operation aborted to prevent split-brain state",
                                    "Reconciled slot: " + activeSlot
                            );
                            incidentService.closeIncident(
                                    inc.getIncidentId(),
                                    "MANUAL_INTERVENTION_REQUIRED",
                                    "Interrupted during prior execution; operator assessment recommended."
                            );
                        }
                    });
                }
            }
            reconciliationSummary = "Reconciliation completed: Aborted " + incompleteOps.size() + " interrupted operation(s). Active slot verified as " + activeSlot + ".";
        } else {
            log.info("Reconciliation: No incomplete recovery operations found. Deployment state clean.");
            reconciliationSummary = "Reconciliation clean: Active slot confirmed as " + activeSlot + ". Zero interrupted operations.";
        }

        reconciliationCompleted = true;
        log.info("=== RECOVERY & DEPLOYMENT STATE RECONCILIATION COMPLETED ===");
    }

    public boolean isReconciliationCompleted() {
        return reconciliationCompleted;
    }

    public String getReconciliationSummary() {
        return reconciliationSummary;
    }
}

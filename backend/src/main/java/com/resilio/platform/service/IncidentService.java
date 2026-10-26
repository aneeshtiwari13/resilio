package com.resilio.platform.service;

import com.resilio.platform.model.Incident;
import com.resilio.platform.model.IncidentTimelineEvent;
import com.resilio.platform.repository.IncidentRepository;
import com.resilio.platform.repository.IncidentTimelineEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepo;
    private final IncidentTimelineEventRepository timelineRepo;

    public IncidentService(IncidentRepository incidentRepo, IncidentTimelineEventRepository timelineRepo) {
        this.incidentRepo = incidentRepo;
        this.timelineRepo = timelineRepo;
    }

    public List<Incident> getAllIncidents() {
        return incidentRepo.findAllByOrderByActionStartedAtDesc();
    }

    public Optional<Incident> getIncidentById(String incidentId) {
        return incidentRepo.findByIncidentId(incidentId);
    }

    public List<IncidentTimelineEvent> getTimelineForIncident(String incidentId) {
        return incidentRepo.findByIncidentId(incidentId)
                .map(i -> timelineRepo.findByIncidentIdOrderByTimestampAsc(i.getId()))
                .orElse(List.of());
    }

    @Transactional
    public Incident createIncident(
            String jenkinsBuild,
            String failureCategory,
            String failedStage,
            String errorEvidence,
            String candidateReleaseId,
            String activeReleaseId,
            String recoveryDecision,
            String recoveryAction,
            String explanation
    ) {
        String incId = "INC-" + Instant.now().toEpochMilli();
        log.info("Creating incident record: id={}, category={}, decision={}", incId, failureCategory, recoveryDecision);

        Incident incident = new Incident(
                incId, jenkinsBuild, failureCategory, failedStage, errorEvidence,
                candidateReleaseId, activeReleaseId, recoveryDecision, recoveryAction, explanation
        );

        // Pre-populate initial timeline events
        incident.addTimelineEvent(new IncidentTimelineEvent(
                "FAILURE_DETECTED", failedStage, "FAILURE",
                "Failure detected during stage: " + failedStage,
                errorEvidence
        ));
        incident.addTimelineEvent(new IncidentTimelineEvent(
                "FAILURE_CLASSIFIED", failedStage, "INFO",
                "Failure categorized as: " + failureCategory,
                explanation
        ));
        incident.addTimelineEvent(new IncidentTimelineEvent(
                "DECISION_SELECTED", failedStage, "INFO",
                "Recovery decision selected: " + recoveryDecision,
                "Action to execute: " + recoveryAction
        ));

        return incidentRepo.save(incident);
    }

    @Transactional
    public void recordTimelineEvent(String incidentId, String eventType, String stageName, String status, String description, String details) {
        incidentRepo.findByIncidentId(incidentId).ifPresent(inc -> {
            IncidentTimelineEvent event = new IncidentTimelineEvent(eventType, stageName, status, description, details);
            inc.addTimelineEvent(event);
            incidentRepo.save(inc);
            log.info("Recorded timeline event for {}: {} - {}", incidentId, eventType, description);
        });
    }

    @Transactional
    public Incident closeIncident(String incidentId, String finalStatus, String verificationResults) {
        return incidentRepo.findByIncidentId(incidentId).map(inc -> {
            inc.setFinalStatus(finalStatus);
            if ("MANUAL_INTERVENTION_REQUIRED".equalsIgnoreCase(finalStatus) || "RECOVERY_FAILED".equalsIgnoreCase(finalStatus)) {
                inc.setRetryEligible(true);
            }
            inc.setVerificationResults(verificationResults);
            inc.setActionCompletedAt(Instant.now());
            if (inc.getActionStartedAt() != null) {
                inc.setRecoveryDurationSeconds(Duration.between(inc.getActionStartedAt(), inc.getActionCompletedAt()).toSeconds());
            }

            String timelineStatus = "RECOVERED".equalsIgnoreCase(finalStatus) ? "SUCCESS" : "FAILURE";
            inc.addTimelineEvent(new IncidentTimelineEvent(
                    "INCIDENT_CONCLUDED",
                    inc.getFailedStage(),
                    timelineStatus,
                    "Incident resolved with final outcome: " + finalStatus,
                    verificationResults
            ));

            Incident saved = incidentRepo.save(inc);
            log.info("Closed incident {}: status={}, duration={}s", incidentId, finalStatus, inc.getRecoveryDurationSeconds());
            return saved;
        }).orElse(null);
    }
}

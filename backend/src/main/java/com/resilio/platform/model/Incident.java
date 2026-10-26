package com.resilio.platform.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String incidentId; // e.g. INC-2026-001

    private String jenkinsBuild; // e.g. #3
    private String failureCategory; // BUILD_FAILURE, UNIT_TEST_FAILURE, CONTAINER_STARTUP_FAILURE, HEALTH_CHECK_FAILURE, SELENIUM_REGRESSION_FAILURE, DEPLOYMENT_FAILURE, UNKNOWN_FAILURE
    private String failedStage;

    @Column(columnDefinition = "TEXT")
    private String errorEvidence;

    private String candidateReleaseId;
    private String activeReleaseId;
    private String recoveryDecision; // CANDIDATE_REJECTED, NO_DEPLOYMENT_REQUIRED, EXECUTE_ROLLBACK, MANUAL_INTERVENTION_REQUIRED, RECOVERY_FAILED
    private String recoveryAction;

    private Instant actionStartedAt;
    private Instant actionCompletedAt;
    private Long recoveryDurationSeconds;

    @Column(columnDefinition = "TEXT")
    private String verificationResults;

    private String finalStatus; // RECOVERED, RECOVERY_FAILED, CANDIDATE_REJECTED, NO_DEPLOYMENT_REQUIRED, MANUAL_INTERVENTION_REQUIRED, RECOVERY_IN_PROGRESS
    
    @Column(columnDefinition = "TEXT")
    private String explanation;

    private boolean retryEligible;
    private int retryCount;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("timestamp ASC")
    private List<IncidentTimelineEvent> timelineEvents = new ArrayList<>();

    public Incident() {}

    public Incident(String incidentId, String jenkinsBuild, String failureCategory, String failedStage,
                    String errorEvidence, String candidateReleaseId, String activeReleaseId,
                    String recoveryDecision, String recoveryAction, String explanation) {
        this.incidentId = incidentId;
        this.jenkinsBuild = jenkinsBuild;
        this.failureCategory = failureCategory;
        this.failedStage = failedStage;
        this.errorEvidence = errorEvidence;
        this.candidateReleaseId = candidateReleaseId;
        this.activeReleaseId = activeReleaseId;
        this.recoveryDecision = recoveryDecision;
        this.recoveryAction = recoveryAction;
        this.explanation = explanation;
        this.actionStartedAt = Instant.now();
        this.finalStatus = "RECOVERY_IN_PROGRESS";
        this.retryEligible = false;
        this.retryCount = 0;
    }

    public void addTimelineEvent(IncidentTimelineEvent event) {
        timelineEvents.add(event);
        event.setIncident(this);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
    public String getJenkinsBuild() { return jenkinsBuild; }
    public void setJenkinsBuild(String jenkinsBuild) { this.jenkinsBuild = jenkinsBuild; }
    public String getFailureCategory() { return failureCategory; }
    public void setFailureCategory(String failureCategory) { this.failureCategory = failureCategory; }
    public String getFailedStage() { return failedStage; }
    public void setFailedStage(String failedStage) { this.failedStage = failedStage; }
    public String getErrorEvidence() { return errorEvidence; }
    public void setErrorEvidence(String errorEvidence) { this.errorEvidence = errorEvidence; }
    public String getCandidateReleaseId() { return candidateReleaseId; }
    public void setCandidateReleaseId(String candidateReleaseId) { this.candidateReleaseId = candidateReleaseId; }
    public String getActiveReleaseId() { return activeReleaseId; }
    public void setActiveReleaseId(String activeReleaseId) { this.activeReleaseId = activeReleaseId; }
    public String getRecoveryDecision() { return recoveryDecision; }
    public void setRecoveryDecision(String recoveryDecision) { this.recoveryDecision = recoveryDecision; }
    public String getRecoveryAction() { return recoveryAction; }
    public void setRecoveryAction(String recoveryAction) { this.recoveryAction = recoveryAction; }
    public Instant getActionStartedAt() { return actionStartedAt; }
    public void setActionStartedAt(Instant actionStartedAt) { this.actionStartedAt = actionStartedAt; }
    public Instant getActionCompletedAt() { return actionCompletedAt; }
    public void setActionCompletedAt(Instant actionCompletedAt) { this.actionCompletedAt = actionCompletedAt; }
    public Long getRecoveryDurationSeconds() { return recoveryDurationSeconds; }
    public void setRecoveryDurationSeconds(Long recoveryDurationSeconds) { this.recoveryDurationSeconds = recoveryDurationSeconds; }
    public String getVerificationResults() { return verificationResults; }
    public void setVerificationResults(String verificationResults) { this.verificationResults = verificationResults; }
    public String getFinalStatus() { return finalStatus; }
    public void setFinalStatus(String finalStatus) { this.finalStatus = finalStatus; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public boolean isRetryEligible() { return retryEligible; }
    public void setRetryEligible(boolean retryEligible) { this.retryEligible = retryEligible; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public List<IncidentTimelineEvent> getTimelineEvents() { return timelineEvents; }
    public void setTimelineEvents(List<IncidentTimelineEvent> timelineEvents) { this.timelineEvents = timelineEvents; }
}

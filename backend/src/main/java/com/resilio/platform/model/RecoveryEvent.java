package com.resilio.platform.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "recovery_events")
public class RecoveryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String failureCategory; // SOURCE_CHECKOUT, MAVEN_BUILD, UNIT_TEST, SELENIUM_REGRESSION, POST_DEPLOY_UNHEALTHY, etc.

    @Column(columnDefinition = "TEXT")
    private String evidenceDetails;

    private String selectedAction; // ROLLBACK, REJECT_CANDIDATE, BLOCK_RELEASE, RETRY_HEALTH, QUARANTINE
    private String executionOutcome; // IN_PROGRESS, SUCCESS, FAILED
    private String verificationResult; // VERIFIED, FAILED, PENDING

    private String previousWorkingVersion;
    private String failedCandidateVersion;
    private String restoredVersion;

    private Instant startedAt;
    private Instant completedAt;
    private Long durationSeconds;

    private boolean operatorTriggered;

    public RecoveryEvent() {}

    public RecoveryEvent(String failureCategory, String evidenceDetails, String selectedAction,
                         String previousWorkingVersion, String failedCandidateVersion, boolean operatorTriggered) {
        this.failureCategory = failureCategory;
        this.evidenceDetails = evidenceDetails;
        this.selectedAction = selectedAction;
        this.previousWorkingVersion = previousWorkingVersion;
        this.failedCandidateVersion = failedCandidateVersion;
        this.operatorTriggered = operatorTriggered;
        this.startedAt = Instant.now();
        this.executionOutcome = "IN_PROGRESS";
        this.verificationResult = "PENDING";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFailureCategory() { return failureCategory; }
    public void setFailureCategory(String failureCategory) { this.failureCategory = failureCategory; }
    public String getEvidenceDetails() { return evidenceDetails; }
    public void setEvidenceDetails(String evidenceDetails) { this.evidenceDetails = evidenceDetails; }
    public String getSelectedAction() { return selectedAction; }
    public void setSelectedAction(String selectedAction) { this.selectedAction = selectedAction; }
    public String getExecutionOutcome() { return executionOutcome; }
    public void setExecutionOutcome(String executionOutcome) { this.executionOutcome = executionOutcome; }
    public String getVerificationResult() { return verificationResult; }
    public void setVerificationResult(String verificationResult) { this.verificationResult = verificationResult; }
    public String getPreviousWorkingVersion() { return previousWorkingVersion; }
    public void setPreviousWorkingVersion(String previousWorkingVersion) { this.previousWorkingVersion = previousWorkingVersion; }
    public String getFailedCandidateVersion() { return failedCandidateVersion; }
    public void setFailedCandidateVersion(String failedCandidateVersion) { this.failedCandidateVersion = failedCandidateVersion; }
    public String getRestoredVersion() { return restoredVersion; }
    public void setRestoredVersion(String restoredVersion) { this.restoredVersion = restoredVersion; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
    public boolean isOperatorTriggered() { return operatorTriggered; }
    public void setOperatorTriggered(boolean operatorTriggered) { this.operatorTriggered = operatorTriggered; }
}

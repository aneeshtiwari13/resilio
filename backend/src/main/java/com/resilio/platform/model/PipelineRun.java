package com.resilio.platform.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pipeline_runs")
public class PipelineRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String buildNumber;
    private String triggerType; // GITHUB_WEBHOOK, MANUAL, RECOVERY
    private String commitSha;
    private String branch;
    private String commitMessage;

    private String status; // QUEUED, RUNNING, PASSED, FAILED, CANCELLED
    private Instant startTime;
    private Instant endTime;
    private Long durationSeconds;

    @OneToMany(mappedBy = "pipelineRun", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PipelineStage> stages = new ArrayList<>();

    public PipelineRun() {}

    public PipelineRun(String buildNumber, String triggerType, String commitSha, String branch, String commitMessage) {
        this.buildNumber = buildNumber;
        this.triggerType = triggerType;
        this.commitSha = commitSha;
        this.branch = branch;
        this.commitMessage = commitMessage;
        this.status = "RUNNING";
        this.startTime = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBuildNumber() { return buildNumber; }
    public void setBuildNumber(String buildNumber) { this.buildNumber = buildNumber; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String commitSha) { this.commitSha = commitSha; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getCommitMessage() { return commitMessage; }
    public void setCommitMessage(String commitMessage) { this.commitMessage = commitMessage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
    public List<PipelineStage> getStages() { return stages; }
    public void setStages(List<PipelineStage> stages) { this.stages = stages; }

    public void addStage(PipelineStage stage) {
        stages.add(stage);
        stage.setPipelineRun(this);
    }
}

package com.resilio.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "pipeline_stages")
public class PipelineStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stageName;
    private String status; // WAITING, RUNNING, PASSED, FAILED, SKIPPED
    private int executionOrder;
    private Long durationMs;

    @Column(columnDefinition = "TEXT")
    private String logExcerpt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_run_id")
    @JsonIgnore
    private PipelineRun pipelineRun;

    public PipelineStage() {}

    public PipelineStage(String stageName, int executionOrder, String status) {
        this.stageName = stageName;
        this.executionOrder = executionOrder;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getExecutionOrder() { return executionOrder; }
    public void setExecutionOrder(int executionOrder) { this.executionOrder = executionOrder; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getLogExcerpt() { return logExcerpt; }
    public void setLogExcerpt(String logExcerpt) { this.logExcerpt = logExcerpt; }
    public PipelineRun getPipelineRun() { return pipelineRun; }
    public void setPipelineRun(PipelineRun pipelineRun) { this.pipelineRun = pipelineRun; }
}

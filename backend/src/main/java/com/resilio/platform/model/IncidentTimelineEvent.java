package com.resilio.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "incident_timeline_events")
public class IncidentTimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant timestamp;
    private String eventType; // e.g. BUILD_STARTED, FAILURE_CLASSIFIED, ROLLBACK_INITIATED, etc.
    private String stageName;
    private String status; // INFO, SUCCESS, FAILURE, WARNING
    private String description;

    @Column(columnDefinition = "TEXT")
    private String details;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    @JsonIgnore
    private Incident incident;

    public IncidentTimelineEvent() {}

    public IncidentTimelineEvent(String eventType, String stageName, String status, String description, String details) {
        this.timestamp = Instant.now();
        this.eventType = eventType;
        this.stageName = stageName;
        this.status = status;
        this.description = description;
        this.details = details;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }
}

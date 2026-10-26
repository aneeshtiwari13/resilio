package com.resilio.platform.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "integration_statuses")
public class IntegrationStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String integrationKey; // GITHUB, JENKINS, DOCKER, SELENIUM, POSTGRESQL, NGINX

    private String displayName;
    private String status; // CONNECTED, DISCONNECTED, DEGRADED
    private Instant lastChecked;
    private String endpointUrl;

    @Column(columnDefinition = "TEXT")
    private String statusDetails;

    public IntegrationStatus() {}

    public IntegrationStatus(String integrationKey, String displayName, String status, String endpointUrl, String statusDetails) {
        this.integrationKey = integrationKey;
        this.displayName = displayName;
        this.status = status;
        this.endpointUrl = endpointUrl;
        this.statusDetails = statusDetails;
        this.lastChecked = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIntegrationKey() { return integrationKey; }
    public void setIntegrationKey(String integrationKey) { this.integrationKey = integrationKey; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getLastChecked() { return lastChecked; }
    public void setLastChecked(Instant lastChecked) { this.lastChecked = lastChecked; }
    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    public String getStatusDetails() { return statusDetails; }
    public void setStatusDetails(String statusDetails) { this.statusDetails = statusDetails; }
}

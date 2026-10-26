package com.resilio.platform.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "health_check_logs")
public class HealthCheckLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant timestamp;
    private String targetUrl;
    private String environmentSlot; // BLUE, GREEN, PROXY
    private Integer statusCode;
    private Long latencyMs;
    private String status; // HEALTHY, DEGRADED, UNHEALTHY, UNREACHABLE
    private Integer consecutiveFailures;

    @Column(columnDefinition = "TEXT")
    private String errorDetail;

    public HealthCheckLog() {}

    public HealthCheckLog(String targetUrl, String environmentSlot, Integer statusCode, Long latencyMs,
                          String status, Integer consecutiveFailures, String errorDetail) {
        this.timestamp = Instant.now();
        this.targetUrl = targetUrl;
        this.environmentSlot = environmentSlot;
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
        this.status = status;
        this.consecutiveFailures = consecutiveFailures;
        this.errorDetail = errorDetail;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
    public String getEnvironmentSlot() { return environmentSlot; }
    public void setEnvironmentSlot(String environmentSlot) { this.environmentSlot = environmentSlot; }
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(Integer consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }
    public String getErrorDetail() { return errorDetail; }
    public void setErrorDetail(String errorDetail) { this.errorDetail = errorDetail; }
}

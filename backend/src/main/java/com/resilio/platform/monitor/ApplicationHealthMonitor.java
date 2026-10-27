package com.resilio.platform.monitor;

import com.resilio.platform.engine.DecisionEngine;
import com.resilio.platform.engine.DecisionResult;
import com.resilio.platform.engine.FailureCategory;
import com.resilio.platform.engine.RecoveryAction;
import com.resilio.platform.model.HealthCheckLog;
import com.resilio.platform.orchestrator.BlueGreenOrchestrator;
import com.resilio.platform.repository.HealthCheckLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
@EnableScheduling
public class ApplicationHealthMonitor {

    private static final Logger log = LoggerFactory.getLogger(ApplicationHealthMonitor.class);

    private final HealthCheckLogRepository healthLogRepo;
    private final DecisionEngine decisionEngine;
    private final BlueGreenOrchestrator orchestrator;
    private final RestTemplate restTemplate;

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicReference<String> currentStatus = new AtomicReference<>("UNKNOWN");
    private final AtomicReference<Instant> lastSuccessfulCheck = new AtomicReference<>(null);

    @Value("${resilio.monitor.target-url:http://localhost:80}")
    private String targetUrl;

    @Value("${resilio.monitor.consecutive-failure-threshold:3}")
    private int failureThreshold;

    @Value("${resilio.monitor.auto-rollback-enabled:false}")
    private boolean autoRollbackEnabled;

    private final AtomicBoolean autoRollbackSuspended = new AtomicBoolean(false);

    public ApplicationHealthMonitor(
            HealthCheckLogRepository healthLogRepo,
            DecisionEngine decisionEngine,
            BlueGreenOrchestrator orchestrator,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${resilio.monitor.timeout-ms:2500}") int timeoutMs
    ) {
        this.healthLogRepo = healthLogRepo;
        this.decisionEngine = decisionEngine;
        this.orchestrator = orchestrator;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    public String getCurrentStatus() { return currentStatus.get(); }
    public int getConsecutiveFailures() { return consecutiveFailures.get(); }
    public Instant getLastSuccessfulCheck() { return lastSuccessfulCheck.get(); }

    @Scheduled(fixedDelayString = "${resilio.monitor.poll-interval-ms:3000}")
    public void performHealthProbe() {
        String activeSlot = orchestrator.getActiveSlot();
        String probeEndpoint = targetUrl + "/health";
        long start = System.currentTimeMillis();

        Integer statusCode = null;
        Long latencyMs = null;
        String status = "UNHEALTHY";
        String errorDetail = null;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(probeEndpoint, Map.class);
            latencyMs = System.currentTimeMillis() - start;
            statusCode = response.getStatusCode().value();

            if (response.getStatusCode().is2xxSuccessful()) {
                Map body = response.getBody();
                if (body != null && "UP".equalsIgnoreCase(String.valueOf(body.get("status")))) {
                    if (latencyMs > 2000) {
                        status = "DEGRADED"; // Latency threshold warning
                    } else {
                        status = "HEALTHY";
                    }
                    consecutiveFailures.set(0);
                    lastSuccessfulCheck.set(Instant.now());
                    autoRollbackSuspended.set(false);
                } else {
                    status = "UNHEALTHY";
                    errorDetail = "Payload status is not UP: " + body;
                    consecutiveFailures.incrementAndGet();
                }
            } else {
                status = "UNHEALTHY";
                errorDetail = "HTTP status code: " + statusCode;
                consecutiveFailures.incrementAndGet();
            }
        } catch (HttpStatusCodeException e) {
            latencyMs = System.currentTimeMillis() - start;
            statusCode = e.getStatusCode().value();
            status = "UNHEALTHY";
            errorDetail = "HTTP error " + statusCode + ": " + e.getResponseBodyAsString();
            consecutiveFailures.incrementAndGet();
        } catch (ResourceAccessException e) {
            latencyMs = System.currentTimeMillis() - start;
            status = "UNREACHABLE";
            errorDetail = "Connection timeout or connection refused: " + e.getMessage();
            consecutiveFailures.incrementAndGet();
        } catch (Exception e) {
            latencyMs = System.currentTimeMillis() - start;
            status = "UNHEALTHY";
            errorDetail = "Unexpected error: " + e.getMessage();
            consecutiveFailures.incrementAndGet();
        }

        currentStatus.set(status);

        // Record health probe
        HealthCheckLog logEntry = new HealthCheckLog(
                probeEndpoint, activeSlot, statusCode, latencyMs, status,
                consecutiveFailures.get(), errorDetail
        );
        healthLogRepo.save(logEntry);

        // Evaluate self-healing failure decision if failures mount
        int failures = consecutiveFailures.get();
        if (failures > 0) {
            FailureCategory category = (failures >= failureThreshold)
                    ? FailureCategory.POST_DEPLOYMENT_HEALTH_FAILURE
                    : FailureCategory.TEMPORARY_HEALTH_FAILURE;

            DecisionResult decision = decisionEngine.evaluate(
                    category,
                    true, // Post activation
                    true, // Has known-good version
                    failures,
                    false
            );

            log.warn("Health probe failed (consecutive={}/{}): status={}, decisionAction={}",
                    failures, failureThreshold, status, decision.getAction());

            if (failures >= failureThreshold && decision.getAction() == RecoveryAction.EXECUTE_ROLLBACK_TO_KNOWN_GOOD) {
                if (autoRollbackSuspended.get()) {
                    log.warn("Auto-rollback currently suspended to prevent infinite flapping. Awaiting operator intervention or target service availability.");
                    return;
                }

                if (autoRollbackEnabled) {
                    log.error("Consecutive failure threshold reached! Invoking Self-Healing Automatic Rollback...");
                    com.resilio.platform.model.RecoveryEvent event = orchestrator.executeRollback(
                            "Application health failure threshold breached (" + failures + " consecutive failures).",
                            "Endpoint " + probeEndpoint + " returned: " + errorDetail,
                            false
                    );
                    consecutiveFailures.set(0); // Reset counter post-rollback

                    if (event == null || !"VERIFIED".equalsIgnoreCase(event.getVerificationResult())) {
                        autoRollbackSuspended.set(true);
                        log.error("CIRCUIT BREAKER TRIPPED: Restored deployment failed verification! Auto-rollback suspended to prevent infinite flapping. Operator intervention required.");
                    }
                } else {
                    log.warn("Auto-rollback disabled by operator policy (autoRollbackEnabled=false). Awaiting manual intervention.");
                }
            }
        }
    }
}

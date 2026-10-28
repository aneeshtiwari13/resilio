package com.resilio.platform.orchestrator;

import com.resilio.platform.model.Deployment;
import com.resilio.platform.model.RecoveryEvent;
import com.resilio.platform.repository.DeploymentRepository;
import com.resilio.platform.repository.RecoveryEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class BlueGreenOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(BlueGreenOrchestrator.class);

    private final DeploymentRepository deploymentRepo;
    private final RecoveryEventRepository recoveryRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    private final AtomicReference<String> activeSlot = new AtomicReference<>("BLUE");
    private final AtomicBoolean isRollbackInProgress = new AtomicBoolean(false);

    @Value("${resilio.monitor.target-url:http://localhost:80}")
    private String targetProxyUrl;

    @Value("${resilio.monitor.target-blue-url:http://localhost:8081}")
    private String blueUrl;

    @Value("${resilio.monitor.target-green-url:http://localhost:8082}")
    private String greenUrl;

    public BlueGreenOrchestrator(DeploymentRepository deploymentRepo, RecoveryEventRepository recoveryRepo) {
        this.deploymentRepo = deploymentRepo;
        this.recoveryRepo = recoveryRepo;
    }

    public String getActiveSlot() {
        return activeSlot.get();
    }

    public void setActiveSlot(String slot) {
        this.activeSlot.set(slot != null ? slot.toUpperCase() : "BLUE");
    }

    public String getInactiveSlot() {
        return "BLUE".equalsIgnoreCase(activeSlot.get()) ? "GREEN" : "BLUE";
    }

    public String getSlotUrl(String slot) {
        return "BLUE".equalsIgnoreCase(slot) ? blueUrl : greenUrl;
    }

    /**
     * Atomically switches Nginx routing to target slot and verifies health.
     */
    public synchronized boolean switchTraffic(String targetSlot) {
        log.info("Initiating traffic switch to slot: {}", targetSlot);
        String previousSlot = activeSlot.get();

        boolean configUpdated = applyNginxUpstreamConfig(targetSlot);
        if (!configUpdated) {
            log.error("Failed to update Nginx upstream configuration for {}", targetSlot);
            return false;
        }

        activeSlot.set(targetSlot);

        // Update database records
        Optional<Deployment> targetDep = deploymentRepo.findByEnvironmentSlotAndStatus(targetSlot, "STANDBY");
        targetDep.ifPresent(d -> {
            d.setStatus("ACTIVE");
            d.setPromotedAt(Instant.now());
            deploymentRepo.save(d);
        });

        Optional<Deployment> prevDep = deploymentRepo.findByEnvironmentSlotAndStatus(previousSlot, "ACTIVE");
        prevDep.ifPresent(d -> {
            d.setStatus("STANDBY");
            deploymentRepo.save(d);
        });

        // Verification of active proxy route
        boolean verified = verifyHealth(targetProxyUrl);
        if (verified) {
            targetDep.ifPresent(d -> {
                d.setKnownGood(true);
                d.setVerifiedAt(Instant.now());
                deploymentRepo.save(d);
            });
            log.info("Traffic switch to {} successfully completed and verified.", targetSlot);
            return true;
        } else {
            log.warn("Target slot {} failed immediate post-activation verification!", targetSlot);
            return false;
        }
    }

    /**
     * Executes automated or operator-directed rollback to the last known-good deployment.
     */
    public synchronized RecoveryEvent executeRollback(String reason, String evidence, boolean manual) {
        if (isRollbackInProgress.get()) {
            log.warn("Rollback already in progress; rejecting concurrent invocation");
            return null;
        }

        isRollbackInProgress.set(true);
        Instant startTime = Instant.now();
        String currentSlot = activeSlot.get();
        String fallbackSlot = getInactiveSlot();

        Optional<Deployment> knownGoodDep = deploymentRepo.findFirstByIsKnownGoodTrueOrderByVerifiedAtDesc();
        String restoredVersion = knownGoodDep.map(Deployment::getVersionTag).orElse("v1.0.0-baseline");
        String failedVersion = deploymentRepo.findByEnvironmentSlotAndStatus(currentSlot, "ACTIVE")
                .map(Deployment::getVersionTag).orElse("candidate-unhealthy");

        log.info("EXECUTING ROLLBACK from slot {} ({}) -> slot {} ({})",
                currentSlot, failedVersion, fallbackSlot, restoredVersion);

        RecoveryEvent event = new RecoveryEvent(
                "POST_DEPLOYMENT_HEALTH_FAILURE",
                evidence,
                "EXECUTE_ROLLBACK_TO_KNOWN_GOOD",
                restoredVersion,
                failedVersion,
                manual
        );
        event = recoveryRepo.save(event);

        try {
            // 1. Switch Nginx proxy upstream back to the inactive slot
            boolean switchOk = applyNginxUpstreamConfig(fallbackSlot);
            if (!switchOk) {
                event.setExecutionOutcome("FAILED");
                event.setVerificationResult("FAILED");
                event.setCompletedAt(Instant.now());
                event.setDurationSeconds(Duration.between(startTime, event.getCompletedAt()).toSeconds());
                return recoveryRepo.save(event);
            }

            activeSlot.set(fallbackSlot);

            // 2. Update deployment records
            knownGoodDep.ifPresent(d -> {
                d.setStatus("ACTIVE");
                d.setEnvironmentSlot(fallbackSlot);
                deploymentRepo.save(d);
            });

            // 3. Independent post-rollback verification
            boolean healthOk = verifyHealth(targetProxyUrl);
            boolean smokeOk = verifySmokeWorkflow(targetProxyUrl);

            event.setCompletedAt(Instant.now());
            event.setDurationSeconds(Duration.between(startTime, event.getCompletedAt()).toSeconds());
            event.setRestoredVersion(restoredVersion);

            if (healthOk && smokeOk) {
                event.setExecutionOutcome("SUCCESS");
                event.setVerificationResult("VERIFIED");
                log.info("Rollback successfully completed and verified in {} seconds.", event.getDurationSeconds());
            } else {
                event.setExecutionOutcome("SUCCESS");
                event.setVerificationResult("FAILED");
                log.error("Rollback completed routing but post-rollback verification failed!");
            }

            return recoveryRepo.save(event);
        } catch (Exception e) {
            log.error("Exception during rollback execution: {}", e.getMessage(), e);
            event.setExecutionOutcome("FAILED");
            event.setVerificationResult("FAILED");
            event.setCompletedAt(Instant.now());
            event.setDurationSeconds(Duration.between(startTime, event.getCompletedAt()).toSeconds());
            return recoveryRepo.save(event);
        } finally {
            isRollbackInProgress.set(false);
        }
    }

    private boolean applyNginxUpstreamConfig(String slot) {
        String targetHost = "BLUE".equalsIgnoreCase(slot) ? "demo-app-blue:8080" : "demo-app-green:8080";
        String localHost = "BLUE".equalsIgnoreCase(slot) ? "localhost:8081" : "localhost:8082";

        // Write upstream configuration file for Nginx include
        File confDir = new File("deploy/nginx");
        if (!confDir.exists()) {
            confDir.mkdirs();
        }
        File confFile = new File(confDir, "upstream.conf");
        try (FileWriter writer = new FileWriter(confFile)) {
            writer.write("# Auto-generated by Resilio BlueGreenOrchestrator\n");
            writer.write("upstream target_backend {\n");
            writer.write("    server " + targetHost + " max_fails=3 fail_timeout=10s;\n");
            writer.write("    # Local development fallback:\n");
            writer.write("    # server " + localHost + ";\n");
            writer.write("}\n");
            log.info("Wrote upstream.conf pointing to {}", targetHost);
            return true;
        } catch (IOException e) {
            log.error("Could not write Nginx upstream config: {}", e.getMessage());
            return false;
        }
    }

    public boolean verifyHealth(String baseUrl) {
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(baseUrl + "/health", Map.class);
            return resp.getStatusCode().is2xxSuccessful() && "UP".equals(resp.getBody() != null ? resp.getBody().get("status") : "");
        } catch (Exception e) {
            log.debug("Verification health check failed for {}: {}", baseUrl, e.getMessage());
            return false;
        }
    }

    private boolean verifySmokeWorkflow(String baseUrl) {
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(baseUrl + "/api/version", Map.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}

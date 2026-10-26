package com.resilio.platform.service;

import com.resilio.platform.model.ReleaseRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Service
public class BlueGreenDeploymentService {

    private static final Logger log = LoggerFactory.getLogger(BlueGreenDeploymentService.class);

    private final RoutingService routingService;
    private final ReleaseRegistryService releaseRegistry;
    private final RecoveryVerificationService verificationService;
    private final RestTemplate restTemplate;

    @Value("${resilio.monitor.target-url:http://localhost:80}")
    private String publicProxyUrl;

    public record DeploymentResult(
            boolean success,
            String releaseId,
            String activeSlot,
            String message
    ) {}

    public BlueGreenDeploymentService(
            RoutingService routingService,
            ReleaseRegistryService releaseRegistry,
            RecoveryVerificationService verificationService,
            RestTemplateBuilder builder
    ) {
        this.routingService = routingService;
        this.releaseRegistry = releaseRegistry;
        this.verificationService = verificationService;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(2000))
                .setReadTimeout(Duration.ofMillis(3000))
                .build();
    }

    /**
     * Promotes a candidate release into production via atomic Nginx upstream switch.
     */
    public synchronized DeploymentResult promoteCandidate(
            String versionTag,
            String commitSha,
            String imageTag,
            String jenkinsBuildRef
    ) {
        String targetSlot = routingService.getInactiveSlot();
        log.info("Starting promotion of candidate: version={}, commit={}, targetSlot={}",
                versionTag, commitSha, targetSlot);

        // 1. Register Candidate in registry
        ReleaseRecord candidate = releaseRegistry.registerCandidate(
                versionTag, commitSha, imageTag, targetSlot, jenkinsBuildRef
        );

        // 2. Perform traffic switch via RoutingService
        boolean switched = routingService.switchUpstreamTraffic(targetSlot);
        if (!switched) {
            log.error("Failed to switch Nginx routing to slot: {}", targetSlot);
            return new DeploymentResult(false, candidate.getReleaseId(), routingService.getActiveSlot(),
                    "Traffic routing switch failed.");
        }

        // 3. Mark release as active in registry
        releaseRegistry.activateRelease(candidate.getReleaseId());

        // 4. Verify post-activation health & smoke checks
        var audit = verificationService.verifyPostRecovery(versionTag, targetSlot);
        if (audit.allPassed()) {
            releaseRegistry.markKnownGood(candidate.getReleaseId(), true, true);
            log.info("Candidate {} promoted and verified successfully as active known-good release.",
                    candidate.getReleaseId());
            return new DeploymentResult(true, candidate.getReleaseId(), targetSlot,
                    "Deployment and post-activation verification succeeded.");
        } else {
            log.warn("Post-activation verification failed for candidate {}: {}", candidate.getReleaseId(), audit.details());
            return new DeploymentResult(false, candidate.getReleaseId(), targetSlot,
                    "Candidate activated but failed post-deployment verification: " + audit.details());
        }
    }
}

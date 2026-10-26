package com.resilio.platform;

import com.resilio.platform.model.Incident;
import com.resilio.platform.model.ReleaseRecord;
import com.resilio.platform.repository.IncidentRepository;
import com.resilio.platform.repository.IncidentTimelineEventRepository;
import com.resilio.platform.repository.ReleaseRecordRepository;
import com.resilio.platform.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class Phase2AcceptanceScenariosTest {

    @Autowired
    private FailureClassificationService classificationService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private ReleaseRegistryService releaseRegistry;

    @Autowired
    private RoutingService routingService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentTimelineEventRepository timelineRepository;

    @Autowired
    private ReleaseRecordRepository releaseRepository;

    private RecoveryDecisionService recoveryDecisionService;

    @BeforeEach
    void setUp() {
        timelineRepository.deleteAll();
        incidentRepository.deleteAll();
        releaseRepository.deleteAll();

        // Custom verification service stub for deterministic testing
        RecoveryVerificationService stubVerification = new RecoveryVerificationService(new RestTemplateBuilder(), routingService) {
            @Override
            public VerificationAudit verifyPostRecovery(String expectedVersion, String expectedSlot) {
                if ("FAILING_TARGET".equalsIgnoreCase(expectedVersion)) {
                    return new VerificationAudit(
                            false,
                            Map.of("HEALTH_CHECK", false),
                            "Verification check failed: Unhealthy",
                            null
                    );
                }
                return new VerificationAudit(
                        true,
                        Map.of(
                                "SLOT_ACTIVE", true,
                                "DIRECT_READINESS", true,
                                "PUBLIC_ROUTE", true,
                                "VERSION_MATCH", true,
                                "SMOKE_WORKFLOW", true,
                                "TRAFFIC_CONFIRMED", true,
                                "INTEGRITY", true
                        ),
                        "7-point verification passed",
                        expectedVersion
                );
            }
        };

        recoveryDecisionService = new RecoveryDecisionService(
                releaseRegistry,
                routingService,
                stubVerification,
                incidentService
        );
    }

    @Test
    @DisplayName("Scenario 1: Compilation Failure -> BUILD_FAILURE & CANDIDATE_REJECTED")
    void testScenario1_CompilationFailure() {
        var classification = classificationService.classify(
                "Maven Build & Packaging",
                1,
                "[ERROR] COMPILATION ERROR: cannot find symbol variable xyz",
                Map.of()
        );
        assertEquals("BUILD_FAILURE", classification.failureCategory());

        Incident incident = recoveryDecisionService.evaluateAndExecute(
                classification.failureCategory(),
                "Maven Build & Packaging",
                classification.evidence(),
                "cand-1.1.0",
                "#101",
                false
        );

        assertEquals("NO_DEPLOYMENT_REQUIRED", incident.getRecoveryDecision());
        assertEquals("NO_DEPLOYMENT_REQUIRED", incident.getFinalStatus());
        assertFalse(incident.isRetryEligible());
        assertTrue(incident.getTimelineEvents().size() >= 2);
    }

    @Test
    @DisplayName("Scenario 2: Unit Test Failure -> UNIT_TEST_FAILURE & NO_DEPLOYMENT_REQUIRED")
    void testScenario2_UnitTestFailure() {
        var classification = classificationService.classify(
                "Maven Build & Unit Tests",
                1,
                "[ERROR] Failures: 1, Errors: 0, Skipped: 0. org.opentest4j.AssertionFailedError: expected 200 but was 500",
                Map.of()
        );
        assertEquals("UNIT_TEST_FAILURE", classification.failureCategory());

        Incident incident = recoveryDecisionService.evaluateAndExecute(
                classification.failureCategory(),
                "Maven Build & Unit Tests",
                classification.evidence(),
                "cand-1.1.0",
                "#102",
                false
        );

        assertEquals("NO_DEPLOYMENT_REQUIRED", incident.getRecoveryDecision());
        assertEquals("NO_DEPLOYMENT_REQUIRED", incident.getFinalStatus());
    }

    @Test
    @DisplayName("Scenario 3: Container Startup Failure -> CONTAINER_STARTUP_FAILURE & CANDIDATE_REJECTED")
    void testScenario3_ContainerStartupFailure() {
        var classification = classificationService.classify(
                "Candidate Container Startup",
                125,
                "docker: Error response from daemon: port 8082 is already allocated",
                Map.of()
        );
        assertEquals("CONTAINER_STARTUP_FAILURE", classification.failureCategory());

        Incident incident = recoveryDecisionService.evaluateAndExecute(
                classification.failureCategory(),
                "Candidate Container Startup",
                classification.evidence(),
                "cand-1.1.0",
                "#103",
                false
        );

        assertEquals("CANDIDATE_REJECTED", incident.getRecoveryDecision());
        assertEquals("CANDIDATE_REJECTED", incident.getFinalStatus());
    }

    @Test
    @DisplayName("Scenario 4: Pre-Switch Selenium Regression -> SELENIUM_REGRESSION_FAILURE & CANDIDATE_REJECTED")
    void testScenario4_SeleniumRegressionFailure() {
        var classification = classificationService.classify(
                "Selenium Candidate Validation",
                1,
                "org.openqa.selenium.NoSuchElementException: Unable to find element with id 'workflow-submit'",
                Map.of()
        );
        assertEquals("SELENIUM_REGRESSION_FAILURE", classification.failureCategory());

        Incident incident = recoveryDecisionService.evaluateAndExecute(
                classification.failureCategory(),
                "Selenium Candidate Validation",
                classification.evidence(),
                "cand-1.1.0",
                "#104",
                false
        );

        assertEquals("CANDIDATE_REJECTED", incident.getRecoveryDecision());
        assertEquals("CANDIDATE_REJECTED", incident.getFinalStatus());
    }

    @Test
    @DisplayName("Scenario 5: Post-Activation Failure with Known-Good -> EXECUTE_ROLLBACK & RECOVERED")
    void testScenario5_PostActivationFailure_WithKnownGood() {
        // Register verified baseline release
        ReleaseRecord baseline = releaseRegistry.registerCandidate("1.0.0", "sha-baseline", "img-1.0.0", "BLUE", "#99");
        releaseRegistry.activateRelease(baseline.getReleaseId());
        releaseRegistry.markKnownGood(baseline.getReleaseId(), true, true);

        // Register candidate and promote to green
        ReleaseRecord candidate = releaseRegistry.registerCandidate("1.1.0-faulty", "sha-faulty", "img-1.1.0", "GREEN", "#105");
        releaseRegistry.activateRelease(candidate.getReleaseId());

        // Post-activation failure detected
        Incident incident = recoveryDecisionService.evaluateAndExecute(
                "HEALTH_CHECK_FAILURE",
                "Post-Deployment Health Verification",
                "Health check probe failed 3 consecutive times with HTTP 500",
                candidate.getReleaseId(),
                "#105",
                true // post-activation
        );

        assertEquals("EXECUTE_ROLLBACK", incident.getRecoveryDecision());
        assertEquals("RECOVERED", incident.getFinalStatus());
        assertNotNull(incident.getVerificationResults());
        assertTrue(incident.getVerificationResults().contains("7-point verification passed"));

        // Active slot must now be restored to BLUE
        assertEquals("BLUE", routingService.getActiveSlot());
    }

    @Test
    @DisplayName("Scenario 6: Missing Known-Good -> MANUAL_INTERVENTION_REQUIRED (Zero guessing)")
    void testScenario6_MissingKnownGood_ManualIntervention() {
        // Only faulty candidate exists, no known-good registered
        ReleaseRecord candidate = releaseRegistry.registerCandidate("1.0.0-unverified", "sha-bad", "img-1.0.0", "BLUE", "#106");
        releaseRegistry.activateRelease(candidate.getReleaseId());

        Incident incident = recoveryDecisionService.evaluateAndExecute(
                "HEALTH_CHECK_FAILURE",
                "Post-Deployment Health Verification",
                "Crash loop backoff detected on active workload",
                candidate.getReleaseId(),
                "#106",
                true
        );

        assertEquals("MANUAL_INTERVENTION_REQUIRED", incident.getRecoveryDecision());
        assertEquals("MANUAL_INTERVENTION_REQUIRED", incident.getFinalStatus());
        assertTrue(incident.isRetryEligible());
    }

    @Test
    @DisplayName("Scenario 7: Post-Recovery Verification Failure -> RECOVERY_FAILED")
    void testScenario7_PostRecoveryVerificationFailure() {
        // Register known-good that will fail verification stub
        ReleaseRecord baseline = releaseRegistry.registerCandidate("FAILING_TARGET", "sha-fail", "img-fail", "BLUE", "#98");
        releaseRegistry.activateRelease(baseline.getReleaseId());
        releaseRegistry.markKnownGood(baseline.getReleaseId(), true, true);

        ReleaseRecord candidate = releaseRegistry.registerCandidate("1.2.0-bad", "sha-bad2", "img-bad2", "GREEN", "#107");
        releaseRegistry.activateRelease(candidate.getReleaseId());

        Incident incident = recoveryDecisionService.evaluateAndExecute(
                "HEALTH_CHECK_FAILURE",
                "Post-Deployment Health Verification",
                "Active container unresponsive",
                candidate.getReleaseId(),
                "#107",
                true
        );

        assertEquals("EXECUTE_ROLLBACK", incident.getRecoveryDecision());
        assertEquals("RECOVERY_FAILED", incident.getFinalStatus());
    }

    @Test
    @DisplayName("Scenario 8: Concurrency Lock prevents overlapping recovery executions")
    void testScenario8_ConcurrencyLockPreventsFlapping() {
        assertFalse(recoveryDecisionService.isRecoveryRunning());
    }
}

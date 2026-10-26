package com.resilio.platform;

import com.resilio.platform.model.Incident;
import com.resilio.platform.model.ReleaseRecord;
import com.resilio.platform.service.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class RecoveryDecisionServiceTest {

    private ReleaseRegistryService releaseRegistry;
    private RoutingService routingService;
    private RecoveryVerificationService verificationService;
    private IncidentService incidentService;
    private RecoveryDecisionService decisionService;

    @BeforeEach
    void setUp() {
        releaseRegistry = Mockito.mock(ReleaseRegistryService.class);
        routingService = Mockito.mock(RoutingService.class);
        verificationService = Mockito.mock(RecoveryVerificationService.class);
        incidentService = Mockito.mock(IncidentService.class);

        decisionService = new RecoveryDecisionService(
                releaseRegistry, routingService, verificationService, incidentService
        );

        when(incidentService.createIncident(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> new Incident(
                        "INC-TEST", inv.getArgument(0), inv.getArgument(1), inv.getArgument(2),
                        inv.getArgument(3), inv.getArgument(4), inv.getArgument(5),
                        inv.getArgument(6), inv.getArgument(7), inv.getArgument(8)
                ));
    }

    @Test
    void testRule1BuildFailureBlocksDeploymentWithoutRollback() {
        Incident inc = decisionService.evaluateAndExecute(
                "BUILD_FAILURE", "Compile", "Syntax error", "1.1.0", "#1", false
        );

        Assertions.assertEquals("NO_DEPLOYMENT_REQUIRED", inc.getRecoveryDecision());
        Assertions.assertTrue(inc.getExplanation().contains("Compilation or packaging failed"));
    }

    @Test
    void testRule2UnitTestFailurePreservesActiveRelease() {
        Incident inc = decisionService.evaluateAndExecute(
                "UNIT_TEST_FAILURE", "Maven Test", "Assertion failed", "1.1.0", "#2", false
        );

        Assertions.assertEquals("NO_DEPLOYMENT_REQUIRED", inc.getRecoveryDecision());
    }

    @Test
    void testRule3CandidateStartupFailureRejectsCandidate() {
        Incident inc = decisionService.evaluateAndExecute(
                "CONTAINER_STARTUP_FAILURE", "Startup", "Port conflict", "1.1.0", "#3", false
        );

        Assertions.assertEquals("CANDIDATE_REJECTED", inc.getRecoveryDecision());
    }

    @Test
    void testRule4SeleniumRegressionBeforeSwitchRejectsCandidate() {
        Incident inc = decisionService.evaluateAndExecute(
                "SELENIUM_REGRESSION_FAILURE", "Selenium Tests", "UI element missing", "1.1.0", "#4", false
        );

        Assertions.assertEquals("CANDIDATE_REJECTED", inc.getRecoveryDecision());
    }

    @Test
    void testRule5PostActivationFailureTriggersRollbackAndVerification() {
        ReleaseRecord active = new ReleaseRecord("rel-1.1.0", "1.1.0", "sha-new", "img", "GREEN", "#5");
        ReleaseRecord knownGood = new ReleaseRecord("rel-1.0.0", "1.0.0", "sha-old", "img", "BLUE", "#1");
        knownGood.setKnownGood(true);

        when(releaseRegistry.getActiveRelease()).thenReturn(Optional.of(active));
        when(releaseRegistry.getLatestKnownGood()).thenReturn(Optional.of(knownGood));
        when(routingService.switchUpstreamTraffic("BLUE")).thenReturn(true);
        when(verificationService.verifyPostRecovery(any(), any()))
                .thenReturn(new RecoveryVerificationService.VerificationAudit(true, java.util.Map.of(), "Verified", "1.0.0"));

        Incident inc = decisionService.evaluateAndExecute(
                "HEALTH_CHECK_FAILURE", "Post-Deploy Probe", "HTTP 500", "1.1.0", "#5", true
        );

        Assertions.assertEquals("EXECUTE_ROLLBACK", inc.getRecoveryDecision());
        Mockito.verify(routingService).switchUpstreamTraffic("BLUE");
        Mockito.verify(releaseRegistry).markRolledBack("rel-1.1.0", "rel-1.0.0");
    }

    @Test
    void testRule6MissingKnownGoodReleaseFlagsManualIntervention() {
        when(releaseRegistry.getActiveRelease()).thenReturn(Optional.of(new ReleaseRecord("rel-1", "1.0", "sha", "img", "BLUE", "#1")));
        when(releaseRegistry.getLatestKnownGood()).thenReturn(Optional.empty()); // No known good!

        Incident inc = decisionService.evaluateAndExecute(
                "HEALTH_CHECK_FAILURE", "Post-Deploy Probe", "HTTP 500", "1.0.0", "#6", true
        );

        Assertions.assertEquals("MANUAL_INTERVENTION_REQUIRED", inc.getRecoveryDecision());
        Assertions.assertTrue(inc.getExplanation().contains("no previously verified known-good release"));
    }
}

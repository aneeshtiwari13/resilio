package com.resilio.platform;

import com.resilio.platform.service.FailureClassificationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

class FailureClassificationServiceTest {

    private FailureClassificationService service;

    @BeforeEach
    void setUp() {
        service = new FailureClassificationService();
    }

    @Test
    void testClassifyBuildFailure() {
        var result = service.classify(
                "Maven Build & Unit Tests",
                1,
                "[ERROR] COMPILATION ERROR : DeliberateSyntaxError.java: <identifier> expected",
                Map.of()
        );
        Assertions.assertEquals("BUILD_FAILURE", result.failureCategory());
        Assertions.assertFalse(result.recoveryEligible());
    }

    @Test
    void testClassifyUnitTestFailure() {
        var result = service.classify(
                "Maven Build & Unit Tests",
                1,
                "[ERROR] Failures: DeliberateFailingUnitTest.testAssertion:8 AssertionFailedError",
                Map.of()
        );
        Assertions.assertEquals("UNIT_TEST_FAILURE", result.failureCategory());
        Assertions.assertFalse(result.recoveryEligible());
    }

    @Test
    void testClassifyContainerStartupFailure() {
        var result = service.classify(
                "Candidate Container Startup",
                125,
                "docker: Error response from daemon: port is already allocated.",
                Map.of()
        );
        Assertions.assertEquals("CONTAINER_STARTUP_FAILURE", result.failureCategory());
        Assertions.assertFalse(result.recoveryEligible());
    }

    @Test
    void testClassifyCandidateReadinessFailure() {
        var result = service.classify(
                "Candidate Readiness Probe",
                1,
                "curl: (7) Failed to connect to candidate port 8080: Connection refused",
                Map.of("isPostActivation", false)
        );
        Assertions.assertEquals("HEALTH_CHECK_FAILURE", result.failureCategory());
        Assertions.assertFalse(result.recoveryEligible()); // Not post-activation, rejected before switch
    }

    @Test
    void testClassifyPostActivationHealthFailure() {
        var result = service.classify(
                "Post-Deployment Health Verification",
                1,
                "HTTP/1.1 500 Internal Server Error: Thread panic in payment engine",
                Map.of("isPostActivation", true)
        );
        Assertions.assertEquals("HEALTH_CHECK_FAILURE", result.failureCategory());
        Assertions.assertTrue(result.recoveryEligible()); // Post-activation is rollback eligible!
    }

    @Test
    void testClassifySeleniumRegressionFailure() {
        var result = service.classify(
                "Selenium Functional & Regression Tests",
                1,
                "org.opentest4j.AssertionFailedError: GATE FAILURE: Functional regression detected",
                Map.of("isPostActivation", false)
        );
        Assertions.assertEquals("SELENIUM_REGRESSION_FAILURE", result.failureCategory());
        Assertions.assertFalse(result.recoveryEligible());
    }

    @Test
    void testClassifyUnknownFailureFallback() {
        var result = service.classify(
                "Arbitrary Step",
                99,
                "Random unexpected system exit without error signatures",
                Map.of()
        );
        Assertions.assertEquals("UNKNOWN_FAILURE", result.failureCategory());
        Assertions.assertFalse(result.recoveryEligible());
    }
}

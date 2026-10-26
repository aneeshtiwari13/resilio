package com.resilio.platform;

import com.resilio.platform.engine.DecisionEngine;
import com.resilio.platform.engine.DecisionResult;
import com.resilio.platform.engine.FailureCategory;
import com.resilio.platform.engine.RecoveryAction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DecisionEngineTest {

    private DecisionEngine decisionEngine;

    @BeforeEach
    void setUp() {
        decisionEngine = new DecisionEngine();
    }

    @Test
    void testMavenCompilationFailureBlocksReleaseWithoutRollback() {
        DecisionResult result = decisionEngine.evaluate(
                FailureCategory.MAVEN_COMPILATION_FAILURE,
                false, // Not post-activation
                true,  // Has known-good
                0,
                false
        );

        Assertions.assertEquals(RecoveryAction.BLOCK_RELEASE_SHOW_BUILD_ERROR, result.getAction());
        Assertions.assertFalse(result.isRequiresVerification());
    }

    @Test
    void testSeleniumRegressionFailureRejectsCandidate() {
        DecisionResult result = decisionEngine.evaluate(
                FailureCategory.SELENIUM_REGRESSION_FAILURE,
                false, // Not post-activation
                true,
                0,
                false
        );

        Assertions.assertEquals(RecoveryAction.REJECT_CANDIDATE_KEEP_ACTIVE, result.getAction());
        Assertions.assertTrue(result.getRationale().contains("Candidate rejected"));
    }

    @Test
    void testTemporaryHealthCheckFailureRetries() {
        DecisionResult result = decisionEngine.evaluate(
                FailureCategory.TEMPORARY_HEALTH_FAILURE,
                true, // Post-activation
                true,
                1,    // 1st failure
                false
        );

        Assertions.assertEquals(RecoveryAction.RETRY_ACCORDING_TO_POLICY, result.getAction());
    }

    @Test
    void testPostDeploymentRepeatedFailureTriggersRollback() {
        DecisionResult result = decisionEngine.evaluate(
                FailureCategory.POST_DEPLOYMENT_HEALTH_FAILURE,
                true, // Post-activation
                true, // Has known-good
                3,    // 3 consecutive failures
                false
        );

        Assertions.assertEquals(RecoveryAction.EXECUTE_ROLLBACK_TO_KNOWN_GOOD, result.getAction());
        Assertions.assertTrue(result.isRequiresVerification());
    }

    @Test
    void testRollbackExecutionFailureHaltsRetries() {
        DecisionResult result = decisionEngine.evaluate(
                FailureCategory.ROLLBACK_EXECUTION_FAILURE,
                true,
                true,
                3,
                true // Is rollback attempt
        );

        Assertions.assertEquals(RecoveryAction.QUARANTINE_HALT_AUTO_RETRY, result.getAction());
    }
}

package com.resilio.platform.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DecisionEngine {

    private static final Logger log = LoggerFactory.getLogger(DecisionEngine.class);

    /**
     * Context-aware evaluation of detected failure according to deployment state.
     *
     * @param category The detected failure category
     * @param isPostActivation Whether the candidate was already promoted to receive production traffic
     * @param hasKnownGoodVersion Whether a valid previous version exists in history
     * @param consecutiveFailures Current counter of consecutive health check failures
     * @param isRollbackAttempt Whether this failure occurred during a rollback execution itself
     * @return DecisionResult containing the remediation action and rationale
     */
    public DecisionResult evaluate(
            FailureCategory category,
            boolean isPostActivation,
            boolean hasKnownGoodVersion,
            int consecutiveFailures,
            boolean isRollbackAttempt
    ) {
        log.debug("DecisionEngine evaluating: category={}, isPostActivation={}, hasKnownGood={}, failures={}, isRollback={}",
                category, isPostActivation, hasKnownGoodVersion, consecutiveFailures, isRollbackAttempt);

        // Guard: Failures occurring during rollback or verification of rollback
        if (isRollbackAttempt || category == FailureCategory.ROLLBACK_EXECUTION_FAILURE) {
            return new DecisionResult(
                    FailureCategory.ROLLBACK_EXECUTION_FAILURE,
                    RecoveryAction.QUARANTINE_HALT_AUTO_RETRY,
                    "Rollback operation failed to complete. Automated retries halted to prevent oscillating state.",
                    false,
                    "Manual Operator Intervention Required"
            );
        }

        if (category == FailureCategory.POST_ROLLBACK_VERIFICATION_FAILURE) {
            return new DecisionResult(
                    FailureCategory.POST_ROLLBACK_VERIFICATION_FAILURE,
                    RecoveryAction.MANUAL_OPERATOR_INTERVENTION,
                    "Restored version failed post-rollback health or smoke checks. System quarantined.",
                    false,
                    "Manual Operator Diagnostics"
            );
        }

        // Pre-activation failures: strictly block or reject without touching active production deployment
        if (!isPostActivation) {
            return switch (category) {
                case SOURCE_CHECKOUT_FAILURE -> new DecisionResult(
                        category,
                        RecoveryAction.ABORT_SOURCE_ERROR,
                        "Source checkout failed. Release halted before build.",
                        false,
                        "None"
                );
                case MAVEN_COMPILATION_FAILURE -> new DecisionResult(
                        category,
                        RecoveryAction.BLOCK_RELEASE_SHOW_BUILD_ERROR,
                        "Java compilation failed. Deployment gate blocked.",
                        false,
                        "None"
                );
                case MAVEN_UNIT_TEST_FAILURE -> new DecisionResult(
                        category,
                        RecoveryAction.BLOCK_RELEASE_SHOW_TEST_ERROR,
                        "One or more unit tests failed. Artifact packaging rejected.",
                        false,
                        "None"
                );
                case SELENIUM_REGRESSION_FAILURE -> new DecisionResult(
                        category,
                        RecoveryAction.REJECT_CANDIDATE_KEEP_ACTIVE,
                        "Functional regression detected during candidate browser testing. Candidate rejected.",
                        false,
                        "None"
                );
                case SELENIUM_INFRASTRUCTURE_FAILURE -> new DecisionResult(
                        category,
                        RecoveryAction.ALERT_SELENIUM_INFRASTRUCTURE,
                        "Selenium Grid infrastructure unavailable. Test execution aborted.",
                        false,
                        "Check Selenium Grid container"
                );
                case REPEATED_CANDIDATE_HEALTH_FAILURE -> new DecisionResult(
                        category,
                        RecoveryAction.REJECT_CANDIDATE_BEFORE_PROMOTION,
                        "Candidate failed pre-promotion readiness probes repeatedly. Route switch prevented.",
                        false,
                        "None"
                );
                default -> new DecisionResult(
                        category,
                        RecoveryAction.BLOCK_RELEASE_SHOW_BUILD_ERROR,
                        "Pre-deployment check failed. Active version preserved intact.",
                        false,
                        "None"
                );
            };
        }

        // Post-activation scenarios
        if (category == FailureCategory.TEMPORARY_HEALTH_FAILURE) {
            return new DecisionResult(
                    category,
                    RecoveryAction.RETRY_ACCORDING_TO_POLICY,
                    "Transient health failure detected (" + consecutiveFailures + " occurrence). Awaiting threshold.",
                    false,
                    "Follow-up probe in 3000ms"
            );
        }

        if (category == FailureCategory.POST_DEPLOYMENT_HEALTH_FAILURE) {
            if (hasKnownGoodVersion) {
                return new DecisionResult(
                        category,
                        RecoveryAction.EXECUTE_ROLLBACK_TO_KNOWN_GOOD,
                        "Active application exceeded consecutive failure threshold. Executing automatic rollback to last known-good version.",
                        true,
                        "Verify reverted version responds HTTP 200 and smoke tests pass"
                );
            } else {
                return new DecisionResult(
                        category,
                        RecoveryAction.MANUAL_OPERATOR_INTERVENTION,
                        "Active application failed but no known-good previous version exists in deployment history.",
                        false,
                        "Operator intervention required"
                );
            }
        }

        return new DecisionResult(
                category,
                RecoveryAction.MANUAL_OPERATOR_INTERVENTION,
                "Unclassified failure state. Operator review recommended.",
                false,
                "Manual review"
        );
    }
}

package com.resilio.platform.engine;

public enum RecoveryAction {
    ABORT_SOURCE_ERROR("Stop pipeline and report source integration error"),
    BLOCK_RELEASE_SHOW_BUILD_ERROR("Block release and display compilation failure details"),
    BLOCK_RELEASE_SHOW_TEST_ERROR("Block release and display failing unit test reports"),
    REJECT_CANDIDATE_KEEP_ACTIVE("Reject candidate before promotion; keep active version intact"),
    ALERT_SELENIUM_INFRASTRUCTURE("Alert operator that Selenium Grid or browser is unreachable"),
    RETRY_ACCORDING_TO_POLICY("Increment retry counter; re-poll health check"),
    REJECT_CANDIDATE_BEFORE_PROMOTION("Reject candidate due to repeated pre-deployment health check failures"),
    EXECUTE_ROLLBACK_TO_KNOWN_GOOD("Initiate automatic rollback to last known-good validated version"),
    QUARANTINE_HALT_AUTO_RETRY("Halt automated rollback retries and trigger critical operator intervention"),
    MANUAL_OPERATOR_INTERVENTION("Mark deployment as requiring operator intervention");

    private final String description;

    RecoveryAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

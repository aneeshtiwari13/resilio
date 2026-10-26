package com.resilio.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class FailureClassificationService {

    private static final Logger log = LoggerFactory.getLogger(FailureClassificationService.class);

    public record ClassificationResult(
            String failureCategory,
            String failedStage,
            String evidence,
            String errorExcerpt,
            double confidence,
            Instant timestamp,
            boolean recoveryEligible,
            String explanation
    ) {}

    /**
     * Deterministic rule-based classification of failure based on execution evidence.
     */
    public ClassificationResult classify(String stageName, int exitCode, String rawLogs, Map<String, Object> context) {
        log.info("Classifying failure at stage: '{}' with exitCode: {}", stageName, exitCode);

        String lowerStage = stageName != null ? stageName.toLowerCase() : "";
        String lowerLogs = rawLogs != null ? rawLogs.toLowerCase() : "";
        String excerpt = extractErrorExcerpt(rawLogs);

        // 1. SELENIUM_REGRESSION_FAILURE (Browser / Selenium stage takes precedence)
        if (lowerStage.contains("selenium") || lowerStage.contains("browser") || lowerLogs.contains("gate failure")) {
            return new ClassificationResult(
                    "SELENIUM_REGRESSION_FAILURE",
                    stageName,
                    "Selenium WebDriver browser tests failed assertion or critical workflow gate",
                    excerpt,
                    0.95,
                    Instant.now(),
                    false, // Candidate rejected before promotion
                    "Browser functional verification detected UI or business workflow regression in candidate."
            );
        }

        // 2. BUILD_FAILURE (Compilation errors, syntax errors, missing symbols)
        if (lowerLogs.contains("compilation error") || lowerLogs.contains("compilation failure")
                || lowerLogs.contains("cannot find symbol") || lowerLogs.contains("syntax error")
                || lowerLogs.contains("<identifier> expected")) {
            return new ClassificationResult(
                    "BUILD_FAILURE",
                    stageName,
                    "Maven compilation process terminated with compiler error (exit code " + exitCode + ")",
                    excerpt,
                    1.0,
                    Instant.now(),
                    false, // Pre-deployment: no rollback required
                    "Java compiler or Maven packaging failed. Candidate blocked before containerization."
            );
        }

        // 3. UNIT_TEST_FAILURE (Surefire / JUnit test failures)
        if (lowerLogs.contains("there are test failures")
                || (lowerLogs.contains("assertionfailederror") && !lowerStage.contains("selenium"))
                || (lowerLogs.contains("failures: ") && !lowerLogs.contains("failures: 0"))) {
            return new ClassificationResult(
                    "UNIT_TEST_FAILURE",
                    stageName,
                    "One or more unit tests failed assertion checks in Surefire test harness",
                    excerpt,
                    1.0,
                    Instant.now(),
                    false,
                    "Mandatory unit test assertion failed. Pipeline halted to prevent packaging defective logic."
            );
        }

        // 4. CONTAINER_STARTUP_FAILURE (Docker container crashed or failed to start)
        if (lowerStage.contains("container") || lowerStage.contains("candidate startup")
                || lowerStage.contains("start candidate") || lowerLogs.contains("docker: error response")
                || lowerLogs.contains("port is already allocated")) {
            return new ClassificationResult(
                    "CONTAINER_STARTUP_FAILURE",
                    stageName,
                    "Docker daemon reported container crash or process startup exit",
                    excerpt,
                    0.95,
                    Instant.now(),
                    false, // Candidate container never accepted production traffic
                    "Candidate container failed to boot. Process exited prematurely."
            );
        }

        // 5. HEALTH_CHECK_FAILURE (Readiness probe timed out or returned 5xx)
        if (lowerStage.contains("health") || lowerStage.contains("readiness") || lowerStage.contains("probe")
                || lowerLogs.contains("connection refused") || lowerLogs.contains("500 internal server error")) {
            boolean isPostActivation = context != null && Boolean.TRUE.equals(context.get("isPostActivation"));
            return new ClassificationResult(
                    "HEALTH_CHECK_FAILURE",
                    stageName,
                    "Application health endpoint failed to respond HTTP 200 UP within timeout boundary",
                    excerpt,
                    1.0,
                    Instant.now(),
                    isPostActivation, // Recovery eligible if production traffic was already switched!
                    isPostActivation
                            ? "Active production workload became unhealthy after deployment. Eligible for automated rollback."
                            : "Candidate container failed pre-deployment readiness checks. Rejected before traffic promotion."
            );
        }

        // 6. DEPLOYMENT_FAILURE (Routing switch or Nginx configuration failure)
        if (lowerStage.contains("promote") || lowerStage.contains("traffic") || lowerStage.contains("routing")) {
            return new ClassificationResult(
                    "DEPLOYMENT_FAILURE",
                    stageName,
                    "Nginx reverse proxy upstream switch or configuration reload failed",
                    excerpt,
                    0.9,
                    Instant.now(),
                    true, // Safe recovery target must be maintained
                    "Traffic switch could not be completed cleanly. Reverting routing."
            );
        }

        // 7. Generic build failure if exitCode != 0 in Maven stage
        if (lowerStage.contains("compile") || lowerStage.contains("package") || lowerStage.contains("maven")) {
            return new ClassificationResult(
                    "BUILD_FAILURE",
                    stageName,
                    "Maven lifecycle execution failed with exit code " + exitCode,
                    excerpt,
                    0.9,
                    Instant.now(),
                    false,
                    "Build stage failed. Pipeline halted before deployment."
            );
        }

        // 8. UNKNOWN_FAILURE (Fallback)
        return new ClassificationResult(
                "UNKNOWN_FAILURE",
                stageName,
                "Unrecognized error pattern in logs with exit code: " + exitCode,
                excerpt,
                0.5,
                Instant.now(),
                false,
                "Unable to definitively categorize failure. Safe policy: halt deployment and alert operator."
        );
    }

    private String extractErrorExcerpt(String logs) {
        if (logs == null || logs.isBlank()) return "No log output available.";
        String[] lines = logs.split("\r?\n");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            String l = line.toLowerCase();
            if (l.contains("error") || l.contains("fail") || l.contains("exception") || l.contains("fatal")) {
                sb.append(line.trim()).append("\n");
                count++;
                if (count >= 5) break; // Capture top 5 critical lines
            }
        }
        return sb.length() > 0 ? sb.toString().trim() : (logs.length() > 300 ? logs.substring(0, 300) + "..." : logs);
    }
}

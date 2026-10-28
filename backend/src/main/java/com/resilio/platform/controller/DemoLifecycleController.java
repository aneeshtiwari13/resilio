package com.resilio.platform.controller;

import com.resilio.platform.model.Incident;
import com.resilio.platform.model.ReleaseRecord;
import com.resilio.platform.service.IncidentService;
import com.resilio.platform.service.ReleaseRegistryService;
import com.resilio.platform.service.RoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/demo/lifecycle")
public class DemoLifecycleController {

    private static final Logger log = LoggerFactory.getLogger(DemoLifecycleController.class);

    private final RoutingService routingService;
    private final ReleaseRegistryService releaseRegistry;
    private final IncidentService incidentService;

    private int currentStep = 1;
    private String lastIncidentId = null;

    public DemoLifecycleController(
            RoutingService routingService,
            ReleaseRegistryService releaseRegistry,
            IncidentService incidentService
    ) {
        this.routingService = routingService;
        this.releaseRegistry = releaseRegistry;
        this.incidentService = incidentService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("currentStep", currentStep);
        resp.put("activeSlot", routingService.getActiveSlot());
        resp.put("activeRelease", releaseRegistry.getActiveRelease().orElse(null));
        resp.put("latestKnownGood", releaseRegistry.getLatestKnownGood().orElse(null));
        resp.put("lastIncidentId", lastIncidentId);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/step/{step}")
    public ResponseEntity<Map<String, Object>> executeStep(@PathVariable int step) {
        log.info("Executing Autonomous Self-Healing Lifecycle Demonstration: Step {}", step);
        Map<String, Object> result = new HashMap<>();
        this.currentStep = step;

        switch (step) {
            case 1 -> {
                // STEP 1: Stable Baseline (Slot BLUE - Version 1.0.0)
                routingService.switchUpstreamTraffic("BLUE");
                releaseRegistry.activateRelease("rel-1.0.0-baseline");
                result.put("stepName", "BASELINE_ACTIVE");
                result.put("version", "1.0.0");
                result.put("slot", "BLUE");
                result.put("status", "HEALTHY");
                result.put("description", "Production traffic routed to verified baseline: Care Chronicle v1.0.0 on Slot BLUE.");
            }
            case 2 -> {
                // STEP 2: Deploy Candidate (Slot GREEN - Version 2.0.0)
                ReleaseRecord cand = releaseRegistry.registerCandidate(
                        "2.0.0", "sha-care-chronicle-v2", "care-chronicle:2.0.0", "GREEN", "#105"
                );
                routingService.switchUpstreamTraffic("GREEN");
                releaseRegistry.activateRelease(cand.getReleaseId());
                result.put("stepName", "CANDIDATE_DEPLOYED");
                result.put("version", "2.0.0");
                result.put("slot", "GREEN");
                result.put("status", "HEALTHY");
                result.put("description", "Jenkins Build #105 deployed Candidate v2.0.0 (LangGraph Multi-Agent Upgrade) to Slot GREEN. Traffic switched.");
            }
            case 3 -> {
                // STEP 3: Crash / Anomaly Injected in Candidate v2.0.0
                Incident inc = incidentService.createIncident(
                        "#105",
                        "POST_DEPLOYMENT_HEALTH_FAILURE",
                        "Post-Activation Health Monitor",
                        "HTTP 500: Subsystem Thread Panic / Memory Buffer Overflow in LangGraph Engine",
                        "2.0.0",
                        "rel-1.0.0-baseline",
                        "EXECUTE_ROLLBACK",
                        "Autonomous Rollback to Known-Good Baseline (Slot BLUE)",
                        "Candidate v2.0.0 degraded after promotion. Consecutive health probe threshold (3/3) breached."
                );
                this.lastIncidentId = inc.getIncidentId();
                result.put("stepName", "OUTAGE_DETECTED");
                result.put("version", "2.0.0");
                result.put("slot", "GREEN");
                result.put("status", "UNHEALTHY");
                result.put("incidentId", inc.getIncidentId());
                result.put("description", "CRITICAL ANOMALY: Candidate v2.0.0 breached health threshold (HTTP 500). Incident " + inc.getIncidentId() + " logged.");
            }
            case 4 -> {
                // STEP 4: Immediate Auto-Rollback to Slot BLUE (Version 1.0.0)
                routingService.switchUpstreamTraffic("BLUE");
                releaseRegistry.markRolledBack("rel-2.0.0-sha-car", "rel-1.0.0-baseline");
                if (lastIncidentId != null) {
                    incidentService.recordTimelineEvent(
                            lastIncidentId, "AUTO_ROLLBACK_EXECUTED", "RoutingService", "SUCCESS",
                            "Traffic instantaneously reverted to Slot BLUE (v1.0.0).",
                            "Zero downtime for active outpatient oncology clinicians."
                    );
                }
                result.put("stepName", "AUTO_ROLLBACK_RESTORED");
                result.put("version", "1.0.0");
                result.put("slot", "BLUE");
                result.put("status", "HEALTHY");
                result.put("description", "AUTONOMOUS RECOVERY: Production traffic restored to Slot BLUE (v1.0.0). Outage neutralized in < 2 seconds.");
            }
            case 5 -> {
                // STEP 5: Self-Healing of Candidate in Isolated Slot GREEN
                if (lastIncidentId != null) {
                    incidentService.recordTimelineEvent(
                            lastIncidentId, "SELF_HEALING_DIAGNOSTICS", "SelfHealingAgent", "INFO",
                            "Isolating candidate in Slot GREEN. Root Cause: LangGraph async event loop starvation.",
                            "Applied auto-remediation patch: Event loop pool scaled, container memory limit expanded."
                    );
                    incidentService.recordTimelineEvent(
                            lastIncidentId, "CONTAINER_HOTFIX_APPLIED", "DockerEngine", "SUCCESS",
                            "Candidate container in Slot GREEN restarted with hotfix patch.",
                            "Readiness probe on port 8000/8082 returned HTTP 200 OK."
                    );
                }
                result.put("stepName", "SELF_HEALING_IN_PROGRESS");
                result.put("version", "2.0.0-HEALED");
                result.put("slot", "GREEN (ISOLATED)");
                result.put("status", "HEALING");
                result.put("description", "SELF-HEALING ENGINE ACTIVE: Isolated candidate container patched, memory pool expanded, and internal health checks passing.");
            }
            case 6 -> {
                // STEP 6: 7-Point Verification & Safe Re-Promotion to v2.0.0
                routingService.switchUpstreamTraffic("GREEN");
                ReleaseRecord healed = releaseRegistry.registerCandidate(
                        "2.0.0-GOLD", "sha-care-chronicle-healed", "care-chronicle:2.0.0-gold", "GREEN", "#106"
                );
                releaseRegistry.activateRelease(healed.getReleaseId());
                releaseRegistry.markKnownGood(healed.getReleaseId(), true, true);

                if (lastIncidentId != null) {
                    incidentService.recordTimelineEvent(
                            lastIncidentId, "SELENIUM_VERIFICATION_PASSED", "SeleniumGrid", "SUCCESS",
                            "Automated browser regression test suite passed: 4/4 test assertions verified.",
                            "Smoke validation workflow confirmed 100% operational."
                    );
                    incidentService.closeIncident(
                            lastIncidentId, "RECOVERED",
                            "Candidate v2.0.0 successfully healed, verified via 7-point audit, and promoted to production."
                    );
                }
                result.put("stepName", "RE_PROMOTED_HEALED");
                result.put("version", "2.0.0-GOLD");
                result.put("slot", "GREEN");
                result.put("status", "HEALTHY");
                result.put("description", "LIFECYCLE COMPLETE: 7-point verification passed. Healed Care Chronicle v2.0.0 promoted as active known-good release!");
            }
            default -> {
                result.put("error", "Invalid step: " + step);
                return ResponseEntity.badRequest().body(result);
            }
        }

        result.put("activeSlot", routingService.getActiveSlot());
        result.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(result);
    }
}

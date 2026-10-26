package com.resilio.platform.controller;

import com.resilio.platform.model.Incident;
import com.resilio.platform.model.ReleaseRecord;
import com.resilio.platform.service.IncidentService;
import com.resilio.platform.service.RecoveryDecisionService;
import com.resilio.platform.service.ReleaseRegistryService;
import com.resilio.platform.service.RoutingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/recovery")
public class RecoveryController {

    private final IncidentService incidentService;
    private final RecoveryDecisionService decisionService;
    private final ReleaseRegistryService releaseRegistry;
    private final RoutingService routingService;

    public RecoveryController(
            IncidentService incidentService,
            RecoveryDecisionService decisionService,
            ReleaseRegistryService releaseRegistry,
            RoutingService routingService
    ) {
        this.incidentService = incidentService;
        this.decisionService = decisionService;
        this.releaseRegistry = releaseRegistry;
        this.routingService = routingService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getRecoveryStatus() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("recoveryInProgress", decisionService.isRecoveryRunning());
        resp.put("activeSlot", routingService.getActiveSlot());
        resp.put("inactiveSlot", routingService.getInactiveSlot());

        Optional<ReleaseRecord> active = releaseRegistry.getActiveRelease();
        resp.put("activeRelease", active.orElse(null));

        Optional<ReleaseRecord> knownGood = releaseRegistry.getLatestKnownGood();
        resp.put("latestKnownGood", knownGood.orElse(null));
        resp.put("rollbackAvailable", knownGood.isPresent());

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/history")
    public ResponseEntity<List<Incident>> getRecoveryHistory() {
        return ResponseEntity.ok(incidentService.getAllIncidents());
    }

    @PostMapping("/{incidentId}/retry")
    public ResponseEntity<Map<String, Object>> retryRecovery(
            @PathVariable String incidentId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        Map<String, Object> resp = new HashMap<>();

        if (decisionService.isRecoveryRunning()) {
            resp.put("success", false);
            resp.put("error", "Another recovery operation is currently running. Please wait.");
            return ResponseEntity.badRequest().body(resp);
        }

        Optional<Incident> incOpt = incidentService.getIncidentById(incidentId);
        if (incOpt.isEmpty()) {
            resp.put("success", false);
            resp.put("error", "Incident not found: " + incidentId);
            return ResponseEntity.notFound().build();
        }

        Incident inc = incOpt.get();
        logRetryAction(incidentId, body != null ? body.get("operator") : "admin");

        // Re-execute evaluation
        Incident result = decisionService.evaluateAndExecute(
                inc.getFailureCategory(),
                inc.getFailedStage(),
                "Operator requested manual retry: " + (body != null ? body.getOrDefault("reason", "Manual retry") : "Manual retry"),
                inc.getCandidateReleaseId(),
                inc.getJenkinsBuild(),
                true // Force recovery attempt
        );

        resp.put("success", true);
        resp.put("incident", result);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/trigger")
    public ResponseEntity<Incident> triggerManualRollback(@RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "Operator manual rollback") : "Operator manual rollback";
        String evidence = body != null ? body.getOrDefault("evidence", "Manual judgment via control center") : "Manual judgment";

        Incident incident = decisionService.evaluateAndExecute(
                "HEALTH_CHECK_FAILURE",
                "Manual Operator Override",
                evidence,
                "current-active",
                "manual",
                true // Post-activation rollback
        );

        return ResponseEntity.ok(incident);
    }

    private void logRetryAction(String incidentId, String operator) {
        incidentService.recordTimelineEvent(
                incidentId,
                "MANUAL_RETRY_INITIATED",
                "Control Center",
                "INFO",
                "Operator '" + operator + "' initiated manual recovery retry",
                "Authorized manual intervention triggered from dashboard"
        );
    }
}

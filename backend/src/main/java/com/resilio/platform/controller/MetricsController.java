package com.resilio.platform.controller;

import com.resilio.platform.model.Incident;
import com.resilio.platform.model.ReleaseRecord;
import com.resilio.platform.service.IncidentService;
import com.resilio.platform.service.ReleaseRegistryService;
import com.resilio.platform.service.RoutingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final IncidentService incidentService;
    private final ReleaseRegistryService releaseRegistry;
    private final RoutingService routingService;

    public MetricsController(
            IncidentService incidentService,
            ReleaseRegistryService releaseRegistry,
            RoutingService routingService
    ) {
        this.incidentService = incidentService;
        this.releaseRegistry = releaseRegistry;
        this.routingService = routingService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> resp = new HashMap<>();

        List<Incident> incidents = incidentService.getAllIncidents();
        List<ReleaseRecord> releases = releaseRegistry.getAllReleases();

        // 1. Basic counts
        int totalIncidents = incidents.size();
        int totalReleases = releases.size();

        resp.put("totalIncidents", totalIncidents);
        resp.put("totalReleases", totalReleases);
        resp.put("activeSlot", routingService.getActiveSlot());
        resp.put("inactiveSlot", routingService.getInactiveSlot());

        Optional<ReleaseRecord> active = releaseRegistry.getActiveRelease();
        resp.put("activeVersion", active.map(ReleaseRecord::getVersionTag).orElse("1.0.0"));

        // 2. Completed recoveries calculation (excluding in-progress)
        long successfulRecoveries = incidents.stream()
                .filter(i -> "RECOVERED".equalsIgnoreCase(i.getFinalStatus()))
                .count();

        long candidatesRejected = incidents.stream()
                .filter(i -> "CANDIDATE_REJECTED".equalsIgnoreCase(i.getFinalStatus()))
                .count();

        long manualInterventions = incidents.stream()
                .filter(i -> "MANUAL_INTERVENTION_REQUIRED".equalsIgnoreCase(i.getFinalStatus()))
                .count();

        long eligibleRollbacks = incidents.stream()
                .filter(i -> "RECOVERED".equalsIgnoreCase(i.getFinalStatus()) || "RECOVERY_FAILED".equalsIgnoreCase(i.getFinalStatus()))
                .count();

        resp.put("successfulRecoveries", successfulRecoveries);
        resp.put("candidatesRejected", candidatesRejected);
        resp.put("manualInterventions", manualInterventions);

        // 3. Rate & Duration metrics
        if (eligibleRollbacks == 0) {
            resp.put("recoverySuccessRateDisplay", "No data yet");
            resp.put("avgRecoveryDurationDisplay", "No data yet");
        } else {
            double rate = ((double) successfulRecoveries / eligibleRollbacks) * 100.0;
            resp.put("recoverySuccessRateDisplay", String.format("%.1f%%", rate));

            double avgSec = incidents.stream()
                    .filter(i -> "RECOVERED".equalsIgnoreCase(i.getFinalStatus()) && i.getRecoveryDurationSeconds() != null)
                    .mapToLong(Incident::getRecoveryDurationSeconds)
                    .average()
                    .orElse(0.0);
            resp.put("avgRecoveryDurationDisplay", String.format("%.1fs", avgSec));
        }

        return ResponseEntity.ok(resp);
    }
}

package com.resilio.platform.controller;

import com.resilio.platform.model.Incident;
import com.resilio.platform.model.IncidentTimelineEvent;
import com.resilio.platform.service.FailureClassificationService;
import com.resilio.platform.service.IncidentService;
import com.resilio.platform.service.RecoveryDecisionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final FailureClassificationService classificationService;
    private final RecoveryDecisionService recoveryDecisionService;

    public IncidentController(
            IncidentService incidentService,
            FailureClassificationService classificationService,
            RecoveryDecisionService recoveryDecisionService
    ) {
        this.incidentService = incidentService;
        this.classificationService = classificationService;
        this.recoveryDecisionService = recoveryDecisionService;
    }

    @GetMapping
    public ResponseEntity<List<Incident>> getAllIncidents() {
        return ResponseEntity.ok(incidentService.getAllIncidents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> getIncidentById(@PathVariable String id) {
        return incidentService.getIncidentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<IncidentTimelineEvent>> getIncidentTimeline(@PathVariable String id) {
        return ResponseEntity.ok(incidentService.getTimelineForIncident(id));
    }

    @PostMapping("/report-failure")
    public ResponseEntity<Incident> reportPipelineFailure(@RequestBody Map<String, Object> body) {
        String stageName = (String) body.getOrDefault("stageName", "Unknown Stage");
        int exitCode = body.containsKey("exitCode") ? ((Number) body.get("exitCode")).intValue() : 1;
        String rawLogs = (String) body.getOrDefault("rawLogs", "");
        String candidateVersion = (String) body.getOrDefault("candidateVersion", "unknown-candidate");
        String jenkinsBuild = (String) body.getOrDefault("jenkinsBuild", "#1");
        boolean isPostActivation = Boolean.TRUE.equals(body.get("isPostActivation"));

        // 1. Failure Classification
        var classification = classificationService.classify(stageName, exitCode, rawLogs, body);

        // 2. Recovery Decision & Execution
        Incident incident = recoveryDecisionService.evaluateAndExecute(
                classification.failureCategory(),
                stageName,
                classification.evidence(),
                candidateVersion,
                jenkinsBuild,
                isPostActivation
        );

        return ResponseEntity.ok(incident);
    }
}

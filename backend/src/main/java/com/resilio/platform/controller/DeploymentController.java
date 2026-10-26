package com.resilio.platform.controller;

import com.resilio.platform.model.Deployment;
import com.resilio.platform.model.RecoveryEvent;
import com.resilio.platform.orchestrator.BlueGreenOrchestrator;
import com.resilio.platform.repository.DeploymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/deployments")
public class DeploymentController {

    private final DeploymentRepository deploymentRepo;
    private final BlueGreenOrchestrator orchestrator;

    public DeploymentController(DeploymentRepository deploymentRepo, BlueGreenOrchestrator orchestrator) {
        this.deploymentRepo = deploymentRepo;
        this.orchestrator = orchestrator;
    }

    @GetMapping
    public ResponseEntity<List<Deployment>> getDeployments() {
        return ResponseEntity.ok(deploymentRepo.findAllByOrderByDeployedAtDesc());
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveDeployment() {
        String activeSlot = orchestrator.getActiveSlot();
        Optional<Deployment> activeDep = deploymentRepo.findByEnvironmentSlotAndStatus(activeSlot, "ACTIVE");

        Map<String, Object> resp = new HashMap<>();
        resp.put("activeSlot", activeSlot);
        resp.put("inactiveSlot", orchestrator.getInactiveSlot());
        resp.put("deployment", activeDep.orElse(null));

        Optional<Deployment> lastKnownGood = deploymentRepo.findFirstByIsKnownGoodTrueOrderByVerifiedAtDesc();
        resp.put("lastKnownGood", lastKnownGood.orElse(null));

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/rollback")
    public ResponseEntity<RecoveryEvent> requestManualRollback(@RequestBody(required = false) Map<String, String> body) {
        String reason = body != null && body.containsKey("reason")
                ? body.get("reason")
                : "Operator manually triggered rollback from Resilio control center";
        String evidence = body != null && body.containsKey("evidence")
                ? body.get("evidence")
                : "Manual operator judgment via web UI";

        RecoveryEvent event = orchestrator.executeRollback(reason, evidence, true);
        if (event == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(event);
    }

    @PostMapping("/switch-slot")
    public ResponseEntity<Map<String, Object>> switchSlot(@RequestParam String targetSlot) {
        boolean ok = orchestrator.switchTraffic(targetSlot.toUpperCase());
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", ok);
        resp.put("activeSlot", orchestrator.getActiveSlot());
        return ResponseEntity.ok(resp);
    }
}

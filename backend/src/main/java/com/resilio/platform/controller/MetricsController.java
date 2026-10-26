package com.resilio.platform.controller;

import com.resilio.platform.model.Deployment;
import com.resilio.platform.model.RecoveryEvent;
import com.resilio.platform.monitor.ApplicationHealthMonitor;
import com.resilio.platform.orchestrator.BlueGreenOrchestrator;
import com.resilio.platform.repository.DeploymentRepository;
import com.resilio.platform.repository.PipelineRunRepository;
import com.resilio.platform.repository.RecoveryEventRepository;
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

    private final PipelineRunRepository pipelineRepo;
    private final DeploymentRepository deploymentRepo;
    private final RecoveryEventRepository recoveryRepo;
    private final ApplicationHealthMonitor healthMonitor;
    private final BlueGreenOrchestrator orchestrator;

    public MetricsController(
            PipelineRunRepository pipelineRepo,
            DeploymentRepository deploymentRepo,
            RecoveryEventRepository recoveryRepo,
            ApplicationHealthMonitor healthMonitor,
            BlueGreenOrchestrator orchestrator
    ) {
        this.pipelineRepo = pipelineRepo;
        this.deploymentRepo = deploymentRepo;
        this.recoveryRepo = recoveryRepo;
        this.healthMonitor = healthMonitor;
        this.orchestrator = orchestrator;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> resp = new HashMap<>();

        long totalPipelines = pipelineRepo.count();
        long totalDeployments = deploymentRepo.count();
        List<RecoveryEvent> recoveries = recoveryRepo.findAll();

        resp.put("totalPipelines", totalPipelines);
        resp.put("totalDeployments", totalDeployments);
        resp.put("activeSlot", orchestrator.getActiveSlot());
        resp.put("currentHealthStatus", healthMonitor.getCurrentStatus());

        Optional<Deployment> activeDep = deploymentRepo.findByEnvironmentSlotAndStatus(orchestrator.getActiveSlot(), "ACTIVE");
        resp.put("activeVersion", activeDep.map(Deployment::getVersionTag).orElse("Not deployed"));

        // Accurate recovery metrics with sample size
        int sampleSize = recoveries.size();
        resp.put("recoverySampleSize", sampleSize);

        if (sampleSize == 0) {
            resp.put("recoveryRateDisplay", "No data yet");
            resp.put("avgRecoverySecondsDisplay", "No data yet");
            resp.put("verifiedRecoveryCount", 0);
        } else {
            long verifiedCount = recoveries.stream()
                    .filter(r -> "VERIFIED".equalsIgnoreCase(r.getVerificationResult()))
                    .count();
            double rate = ((double) verifiedCount / sampleSize) * 100.0;
            resp.put("recoveryRateDisplay", String.format("%.1f%%", rate));
            resp.put("verifiedRecoveryCount", verifiedCount);

            double avgSec = recoveries.stream()
                    .filter(r -> r.getDurationSeconds() != null)
                    .mapToLong(RecoveryEvent::getDurationSeconds)
                    .average()
                    .orElse(0.0);
            resp.put("avgRecoverySecondsDisplay", String.format("%.1fs", avgSec));
        }

        return ResponseEntity.ok(resp);
    }
}

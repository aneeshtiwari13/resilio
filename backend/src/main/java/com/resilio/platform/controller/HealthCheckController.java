package com.resilio.platform.controller;

import com.resilio.platform.model.HealthCheckLog;
import com.resilio.platform.monitor.ApplicationHealthMonitor;
import com.resilio.platform.orchestrator.BlueGreenOrchestrator;
import com.resilio.platform.repository.HealthCheckLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthCheckController {

    private final ApplicationHealthMonitor monitor;
    private final BlueGreenOrchestrator orchestrator;
    private final HealthCheckLogRepository logRepo;

    public HealthCheckController(ApplicationHealthMonitor monitor, BlueGreenOrchestrator orchestrator, HealthCheckLogRepository logRepo) {
        this.monitor = monitor;
        this.orchestrator = orchestrator;
        this.logRepo = logRepo;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> resilioHealth() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "UP");
        resp.put("service", "resilio-backend");
        resp.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/health-monitor/status")
    public ResponseEntity<Map<String, Object>> targetStatus() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("currentStatus", monitor.getCurrentStatus());
        resp.put("consecutiveFailures", monitor.getConsecutiveFailures());
        resp.put("activeSlot", orchestrator.getActiveSlot());
        resp.put("lastSuccessfulCheck", monitor.getLastSuccessfulCheck() != null ? monitor.getLastSuccessfulCheck().toString() : null);

        List<HealthCheckLog> recentLogs = logRepo.findAllByOrderByTimestampDesc(PageRequest.of(0, 20));
        resp.put("recentLogs", recentLogs);

        return ResponseEntity.ok(resp);
    }
}

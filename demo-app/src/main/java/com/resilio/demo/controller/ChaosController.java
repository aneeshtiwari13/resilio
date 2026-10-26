package com.resilio.demo.controller;

import com.resilio.demo.service.ChaosStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chaos")
public class ChaosController {

    private final ChaosStateService chaosService;

    public ChaosController(ChaosStateService chaosService) {
        this.chaosService = chaosService;
    }

    @PostMapping("/fault")
    public ResponseEntity<Map<String, Object>> injectFault(@RequestParam(defaultValue = "UNHEALTHY_500") String mode) {
        ChaosStateService.FailureMode failureMode;
        try {
            failureMode = ChaosStateService.FailureMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Invalid mode. Valid options: UNHEALTHY_500, LATENCY_SPIKE, FUNCTIONAL_REGRESSION");
            return ResponseEntity.badRequest().body(err);
        }

        chaosService.setMode(failureMode);

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "FAULT_INJECTED");
        resp.put("activeMode", failureMode.name());
        resp.put("message", "Simulated failure condition activated for demonstration");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetFault() {
        chaosService.reset();
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "NORMAL");
        resp.put("activeMode", ChaosStateService.FailureMode.NONE.name());
        resp.put("message", "All chaos parameters restored to healthy baseline");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("activeMode", chaosService.getActiveMode().name());
        resp.put("failureCount", chaosService.getFailureCount());
        resp.put("regressionInduced", chaosService.isRegressionInduced());
        return ResponseEntity.ok(resp);
    }
}

package com.resilio.platform.controller;

import com.resilio.platform.model.ReleaseRecord;
import com.resilio.platform.service.BlueGreenDeploymentService;
import com.resilio.platform.service.RecoveryVerificationService;
import com.resilio.platform.service.ReleaseRegistryService;
import com.resilio.platform.service.RoutingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/releases")
public class ReleaseController {

    private final ReleaseRegistryService releaseRegistry;
    private final BlueGreenDeploymentService deploymentService;
    private final RecoveryVerificationService verificationService;
    private final RoutingService routingService;

    public ReleaseController(
            ReleaseRegistryService releaseRegistry,
            BlueGreenDeploymentService deploymentService,
            RecoveryVerificationService verificationService,
            RoutingService routingService
    ) {
        this.releaseRegistry = releaseRegistry;
        this.deploymentService = deploymentService;
        this.verificationService = verificationService;
        this.routingService = routingService;
    }

    @GetMapping
    public ResponseEntity<List<ReleaseRecord>> getAllReleases() {
        return ResponseEntity.ok(releaseRegistry.getAllReleases());
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveRelease() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("activeSlot", routingService.getActiveSlot());
        resp.put("inactiveSlot", routingService.getInactiveSlot());
        resp.put("release", releaseRegistry.getActiveRelease().orElse(null));
        resp.put("lastKnownGood", releaseRegistry.getLatestKnownGood().orElse(null));
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/known-good")
    public ResponseEntity<List<ReleaseRecord>> getKnownGoodReleases() {
        return ResponseEntity.ok(releaseRegistry.getAllKnownGoodReleases());
    }

    @PostMapping("/promote")
    public ResponseEntity<BlueGreenDeploymentService.DeploymentResult> promoteCandidate(@RequestBody Map<String, String> body) {
        String versionTag = body.getOrDefault("versionTag", "1.1.0");
        String commitSha = body.getOrDefault("commitSha", "candidate-sha");
        String imageTag = body.getOrDefault("imageTag", "resilio/demo-app:" + versionTag);
        String jenkinsBuildRef = body.getOrDefault("jenkinsBuildRef", "#1");

        var result = deploymentService.promoteCandidate(versionTag, commitSha, imageTag, jenkinsBuildRef);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{releaseId}/verify")
    public ResponseEntity<RecoveryVerificationService.VerificationAudit> verifyRelease(@PathVariable String releaseId) {
        var relOpt = releaseRegistry.getAllReleases().stream()
                .filter(r -> r.getReleaseId().equalsIgnoreCase(releaseId))
                .findFirst();

        if (relOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ReleaseRecord r = relOpt.get();
        var audit = verificationService.verifyPostRecovery(r.getVersionTag(), r.getDeploymentSlot());
        return ResponseEntity.ok(audit);
    }
}

package com.resilio.platform.controller;

import com.resilio.platform.jenkins.JenkinsClient;
import com.resilio.platform.model.PipelineRun;
import com.resilio.platform.repository.PipelineRunRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pipelines")
public class PipelineController {

    private final PipelineRunRepository pipelineRepo;
    private final JenkinsClient jenkinsClient;

    public PipelineController(PipelineRunRepository pipelineRepo, JenkinsClient jenkinsClient) {
        this.pipelineRepo = pipelineRepo;
        this.jenkinsClient = jenkinsClient;
    }

    @GetMapping
    public ResponseEntity<List<PipelineRun>> getAllPipelines() {
        return ResponseEntity.ok(pipelineRepo.findAllByOrderByStartTimeDesc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PipelineRun> getPipelineById(@PathVariable Long id) {
        return pipelineRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/trigger")
    public ResponseEntity<PipelineRun> triggerPipeline(@RequestBody(required = false) Map<String, String> body) {
        String triggerType = body != null && body.containsKey("triggerType") ? body.get("triggerType") : "MANUAL";
        String commitSha = body != null ? body.get("commitSha") : null;
        String branch = body != null ? body.get("branch") : "main";
        String message = body != null ? body.get("message") : "Manual trigger from Resilio Dashboard";

        PipelineRun run = jenkinsClient.triggerPipeline(triggerType, commitSha, branch, message);
        return ResponseEntity.ok(run);
    }
}

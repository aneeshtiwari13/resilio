package com.resilio.platform.jenkins;

import com.resilio.platform.model.PipelineRun;
import com.resilio.platform.model.PipelineStage;
import com.resilio.platform.repository.PipelineRunRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class JenkinsClient {

    private static final Logger log = LoggerFactory.getLogger(JenkinsClient.class);

    private final RestTemplate restTemplate;
    private final PipelineRunRepository pipelineRepo;

    @Value("${resilio.simulation.enabled:true}")
    private boolean simulationEnabled;

    @Value("${jenkins.url:http://localhost:8085}")
    private String jenkinsUrl;

    @Value("${jenkins.username:admin}")
    private String username;

    @Value("${jenkins.api-token:admin123}")
    private String apiToken;

    @Value("${jenkins.job-name:resilio-pipeline}")
    private String jobName;

    public JenkinsClient(RestTemplateBuilder builder, PipelineRunRepository pipelineRepo) {
        this.pipelineRepo = pipelineRepo;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(2000))
                .setReadTimeout(Duration.ofMillis(2000))
                .build();
    }

    @PostConstruct
    public void seedInitialPipelines() {
        if (pipelineRepo.count() == 0) {
            log.info("Seeding initial baseline CI/CD pipeline runs for demonstration...");
            PipelineRun run1 = new PipelineRun(
                    "#101",
                    "GITHUB_PUSH",
                    "982a17f",
                    "main",
                    "Release v1.0.0: Target microservice baseline deploy"
            );
            run1.setStatus("PASSED");
            run1.setDurationSeconds(42L);
            run1.setEndTime(Instant.now().minusSeconds(3600));
            populateAllStages(run1, "PASSED");
            pipelineRepo.save(run1);

            PipelineRun run2 = new PipelineRun(
                    "#102",
                    "GITHUB_WEBHOOK",
                    "47cf39b",
                    "main",
                    "Release v1.1.0: Self-healing circuit breaker integration"
            );
            run2.setStatus("PASSED");
            run2.setDurationSeconds(38L);
            run2.setEndTime(Instant.now().minusSeconds(1200));
            populateAllStages(run2, "PASSED");
            pipelineRepo.save(run2);
        }
    }

    public boolean isConnected() {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(
                    jenkinsUrl + "/api/json", HttpMethod.GET, entity, String.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("Jenkins health check failed: {}", e.getMessage());
            return simulationEnabled;
        }
    }

    public PipelineRun triggerPipeline(String triggerType, String commitSha, String branch, String message) {
        log.info("Triggering Jenkins pipeline: type={}, commit={}, branch={}", triggerType, commitSha, branch);

        String buildNum = "#" + (103 + (System.currentTimeMillis() % 900));
        PipelineRun run = new PipelineRun(
                buildNum,
                triggerType,
                commitSha != null ? commitSha : "sha-" + Long.toHexString(System.currentTimeMillis()).substring(0, 7),
                branch != null ? branch : "main",
                message != null ? message : "Manual deployment trigger via Resilio Dashboard"
        );

        populateAllStages(run, "WAITING");
        if (!run.getStages().isEmpty()) {
            run.getStages().get(0).setStatus("RUNNING");
        }

        PipelineRun saved = pipelineRepo.save(run);

        // Attempt dispatch to real Jenkins if online
        boolean realDispatched = false;
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String triggerUrl = jenkinsUrl + "/job/" + jobName + "/build";
            restTemplate.postForEntity(triggerUrl, entity, String.class);
            log.info("Successfully dispatched build request to real Jenkins URL: {}", triggerUrl);
            realDispatched = true;
        } catch (ResourceAccessException e) {
            log.debug("Jenkins server unreachable. Running in simulation mode.");
        } catch (Exception e) {
            log.debug("Jenkins trigger exception: {}. Check credentials.", e.getMessage());
        }

        // If real Jenkins is not reachable but simulation is enabled, simulate stage progression
        if (!realDispatched && simulationEnabled) {
            CompletableFuture.runAsync(() -> simulatePipelineExecution(saved.getId()));
        }

        return saved;
    }

    private void simulatePipelineExecution(Long runId) {
        try {
            for (int i = 0; i < 10; i++) {
                Thread.sleep(800);
                Optional<PipelineRun> opt = pipelineRepo.findById(runId);
                if (opt.isEmpty()) return;
                PipelineRun p = opt.get();
                List<PipelineStage> stages = p.getStages();
                if (i < stages.size()) {
                    stages.get(i).setStatus("PASSED");
                    stages.get(i).setDurationMs(750L);
                    stages.get(i).setLogExcerpt("Stage " + stages.get(i).getStageName() + " completed with exit code 0.");
                }
                if (i + 1 < stages.size()) {
                    stages.get(i + 1).setStatus("RUNNING");
                } else {
                    p.setStatus("PASSED");
                    p.setEndTime(Instant.now());
                    p.setDurationSeconds(8L);
                }
                pipelineRepo.save(p);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void populateAllStages(PipelineRun run, String initialStatus) {
        run.addStage(new PipelineStage("Checkout Source", 1, initialStatus));
        run.addStage(new PipelineStage("Validate Environment", 2, initialStatus));
        run.addStage(new PipelineStage("Maven Build & Unit Tests", 3, initialStatus));
        run.addStage(new PipelineStage("Package Application", 4, initialStatus));
        run.addStage(new PipelineStage("Docker Image Build", 5, initialStatus));
        run.addStage(new PipelineStage("Candidate Deployment", 6, initialStatus));
        run.addStage(new PipelineStage("Candidate Readiness Probe", 7, initialStatus));
        run.addStage(new PipelineStage("Selenium Regression & Smoke Tests", 8, initialStatus));
        run.addStage(new PipelineStage("Promote Candidate", 9, initialStatus));
        run.addStage(new PipelineStage("Post-Deployment Verification", 10, initialStatus));
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, apiToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}

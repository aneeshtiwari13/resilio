package com.resilio.platform.jenkins;

import com.resilio.platform.model.PipelineRun;
import com.resilio.platform.model.PipelineStage;
import com.resilio.platform.repository.PipelineRunRepository;
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
import java.util.Map;

@Service
public class JenkinsClient {

    private static final Logger log = LoggerFactory.getLogger(JenkinsClient.class);

    private final RestTemplate restTemplate;
    private final PipelineRunRepository pipelineRepo;

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
                .setConnectTimeout(Duration.ofMillis(3000))
                .setReadTimeout(Duration.ofMillis(3000))
                .build();
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
            return false;
        }
    }

    public PipelineRun triggerPipeline(String triggerType, String commitSha, String branch, String message) {
        log.info("Triggering Jenkins pipeline: type={}, commit={}, branch={}", triggerType, commitSha, branch);

        PipelineRun run = new PipelineRun(
                "#" + (System.currentTimeMillis() % 10000),
                triggerType,
                commitSha != null ? commitSha : "manual-" + Long.toHexString(System.currentTimeMillis()),
                branch != null ? branch : "main",
                message != null ? message : "Manual deployment trigger via Resilio Dashboard"
        );

        // Pre-populate expected execution stages
        run.addStage(new PipelineStage("Checkout Source", 1, "RUNNING"));
        run.addStage(new PipelineStage("Validate Environment", 2, "WAITING"));
        run.addStage(new PipelineStage("Maven Build & Unit Tests", 3, "WAITING"));
        run.addStage(new PipelineStage("Package Application", 4, "WAITING"));
        run.addStage(new PipelineStage("Docker Image Build", 5, "WAITING"));
        run.addStage(new PipelineStage("Candidate Deployment", 6, "WAITING"));
        run.addStage(new PipelineStage("Candidate Readiness Probe", 7, "WAITING"));
        run.addStage(new PipelineStage("Selenium Regression & Smoke Tests", 8, "WAITING"));
        run.addStage(new PipelineStage("Promote Candidate", 9, "WAITING"));
        run.addStage(new PipelineStage("Post-Deployment Verification", 10, "WAITING"));

        PipelineRun saved = pipelineRepo.save(run);

        // Dispatch call to real Jenkins if reachable
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String triggerUrl = jenkinsUrl + "/job/" + jobName + "/build";
            restTemplate.postForEntity(triggerUrl, entity, String.class);
            log.info("Successfully dispatched build request to Jenkins URL: {}", triggerUrl);
        } catch (ResourceAccessException e) {
            log.warn("Jenkins server is unreachable at {}. Pipeline saved locally with running state.", jenkinsUrl);
        } catch (Exception e) {
            log.warn("Jenkins trigger returned exception: {}. Check credentials and job configuration.", e.getMessage());
        }

        return saved;
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, apiToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}

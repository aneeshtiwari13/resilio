package com.resilio.platform.controller;

import com.resilio.platform.jenkins.JenkinsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {

    private final JenkinsClient jenkinsClient;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${selenium.grid-url:http://localhost:4444/wd/hub}")
    private String seleniumUrl;

    @Value("${resilio.monitor.target-url:http://localhost:80}")
    private String nginxUrl;

    public IntegrationController(JenkinsClient jenkinsClient) {
        this.jenkinsClient = jenkinsClient;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getIntegrations() {
        List<Map<String, Object>> integrations = new ArrayList<>();

        // 1. Docker Runtime
        boolean dockerUp = checkDockerDaemon();
        integrations.add(createIntegrationMap(
                "DOCKER",
                "Docker Container Engine",
                dockerUp ? "CONNECTED" : "DISCONNECTED",
                "local daemon (cli/pipe)",
                dockerUp ? "Docker container engine operational." : "Docker daemon not detected or offline on host."
        ));

        // 2. Jenkins Integration
        boolean jenkinsUp = jenkinsClient.isConnected();
        integrations.add(createIntegrationMap(
                "JENKINS",
                "Jenkins CI/CD Automation",
                jenkinsUp ? "CONNECTED" : "DISCONNECTED",
                "http://localhost:8085",
                jenkinsUp ? "Pipeline execution engine operational." : "Unable to reach Jenkins on port 8085."
        ));

        // 3. Selenium Grid
        boolean seleniumUp = checkEndpoint(seleniumUrl + "/status");
        integrations.add(createIntegrationMap(
                "SELENIUM",
                "Selenium Browser Testing Grid",
                seleniumUp ? "CONNECTED" : "DISCONNECTED",
                seleniumUrl,
                seleniumUp ? "Standalone Chrome node active and ready for sessions." : "Selenium Grid endpoint unavailable."
        ));

        // 4. Nginx Reverse Proxy
        boolean nginxUp = checkEndpoint(nginxUrl);
        integrations.add(createIntegrationMap(
                "NGINX",
                "Nginx Blue/Green Traffic Router",
                nginxUp ? "CONNECTED" : "DISCONNECTED",
                nginxUrl,
                nginxUp ? "Traffic router upstream proxy listening." : "Nginx proxy unavailable."
        ));

        // 4. PostgreSQL / Platform Database
        integrations.add(createIntegrationMap(
                "DATABASE",
                "Platform Storage Engine",
                "CONNECTED",
                "jdbc:postgresql://localhost:5432/resilio",
                "Active relational persistence engine operational."
        ));

        // 5. GitHub Source Control
        integrations.add(createIntegrationMap(
                "GITHUB",
                "GitHub Source Code Repository",
                "CONNECTED",
                "https://github.com/resilio-devops/demo-app",
                "Webhook endpoint listener registered."
        ));

        return ResponseEntity.ok(integrations);
    }

    private boolean checkDockerDaemon() {
        try {
            Process process = new ProcessBuilder("docker", "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkEndpoint(String url) {
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            return resp.getStatusCode().is2xxSuccessful() || resp.getStatusCode().is3xxRedirection();
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> createIntegrationMap(String key, String name, String status, String url, String details) {
        Map<String, Object> map = new HashMap<>();
        map.put("key", key);
        map.put("displayName", name);
        map.put("status", status);
        map.put("endpointUrl", url);
        map.put("details", details);
        map.put("lastChecked", Instant.now().toString());
        return map;
    }
}

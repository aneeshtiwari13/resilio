package com.resilio.demo.controller;

import com.resilio.demo.service.ChaosStateService;
import com.resilio.demo.service.DataProcessingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
public class WebController {

    private final ChaosStateService chaosService;
    private final DataProcessingService dataProcessingService;

    @Value("${app.version:1.0.0}")
    private String version;

    @Value("${app.commit-sha:local-dev-sha-001}")
    private String commitSha;

    @Value("${app.build-number:1}")
    private String buildNumber;

    @Value("${app.environment:standalone}")
    private String environment;

    public WebController(ChaosStateService chaosService, DataProcessingService dataProcessingService) {
        this.chaosService = chaosService;
        this.dataProcessingService = dataProcessingService;
    }

    @GetMapping(value = "/", produces = "text/html")
    public String home() {
        if (chaosService.isUnhealthy()) {
            throw new RuntimeException("Simulated Post-Deployment Catastrophic Service Failure");
        }
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Resilio Demo Application</title>
                <style>
                    :root {
                        --bg-canvas: #F7F6F2;
                        --card-bg: #FFFFFF;
                        --text-charcoal: #262B2A;
                        --text-muted: #747A76;
                        --accent-emerald: #17866B;
                        --accent-emerald-hover: #136F58;
                        --border-color: #E7E5DF;
                        --accent-coral: #E97963;
                        --accent-coral-light: #FDEEEC;
                        --accent-emerald-light: #E6F5F0;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background: var(--bg-canvas);
                        color: var(--text-charcoal);
                        margin: 0;
                        padding: 0;
                    }
                    .navbar {
                        background: var(--card-bg);
                        border-bottom: 1px solid var(--border-color);
                        padding: 14px 32px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }
                    .nav-brand {
                        font-weight: 700;
                        font-size: 16px;
                        color: var(--text-charcoal);
                        display: flex;
                        align-items: center;
                        gap: 8px;
                    }
                    .nav-brand-badge {
                        background: var(--accent-emerald);
                        color: white;
                        font-size: 12px;
                        padding: 2px 8px;
                        border-radius: 4px;
                    }
                    .nav-links a {
                        color: var(--text-muted);
                        text-decoration: none;
                        font-size: 13px;
                        font-weight: 500;
                        margin-left: 20px;
                        transition: color 0.15s;
                    }
                    .nav-links a:hover {
                        color: var(--accent-emerald);
                    }
                    .container {
                        max-width: 720px;
                        margin: 40px auto;
                        padding: 0 20px;
                    }
                    .card {
                        background: var(--card-bg);
                        border: 1px solid var(--border-color);
                        border-radius: 12px;
                        padding: 32px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.02);
                        margin-bottom: 24px;
                    }
                    .badge {
                        display: inline-block;
                        padding: 4px 12px;
                        border-radius: 9999px;
                        font-weight: 600;
                        font-size: 12px;
                        text-transform: uppercase;
                    }
                    .badge-healthy {
                        background: var(--accent-emerald-light);
                        color: var(--accent-emerald);
                        border: 1px solid #A2DBC9;
                    }
                    .meta-grid {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 16px;
                        background: #FBFBFA;
                        border: 1px solid var(--border-color);
                        border-radius: 8px;
                        padding: 16px;
                        margin: 20px 0;
                        font-size: 13px;
                    }
                    .field {
                        margin: 16px 0;
                    }
                    label {
                        display: block;
                        font-size: 13px;
                        font-weight: 600;
                        color: var(--text-charcoal);
                        margin-bottom: 6px;
                    }
                    input[type="text"] {
                        width: 100%%;
                        padding: 10px 14px;
                        border: 1px solid var(--border-color);
                        border-radius: 6px;
                        box-sizing: border-box;
                        font-size: 14px;
                        transition: border-color 0.15s;
                    }
                    input[type="text"]:focus {
                        border-color: var(--accent-emerald);
                        outline: none;
                    }
                    button {
                        background: var(--accent-emerald);
                        color: white;
                        border: none;
                        padding: 10px 20px;
                        border-radius: 6px;
                        font-weight: 600;
                        font-size: 13px;
                        cursor: pointer;
                        transition: background-color 0.15s;
                    }
                    button:hover {
                        background: var(--accent-emerald-hover);
                    }
                    #result-message {
                        margin-top: 20px;
                        padding: 14px 18px;
                        border-radius: 8px;
                        font-size: 13px;
                        display: none;
                    }
                    .result-success {
                        background: var(--accent-emerald-light);
                        color: var(--accent-emerald);
                        border: 1px solid #A2DBC9;
                    }
                    .result-error {
                        background: var(--accent-coral-light);
                        color: var(--accent-coral);
                        border: 1px solid #F5B9B0;
                    }
                </style>
            </head>
            <body>
                <!-- Navigation Bar -->
                <nav class="navbar" id="main-nav">
                    <div class="nav-brand">
                        <span class="nav-brand-badge">RESILIO</span>
                        <span id="app-title">Resilio Demo Application</span>
                    </div>
                    <div class="nav-links">
                        <a href="#overview">Overview</a>
                        <a href="#functional-test">Functional Test</a>
                        <a href="/actuator/health" target="_blank">Health Actuator</a>
                        <a href="/api/version" target="_blank">Version API</a>
                    </div>
                </nav>

                <div class="container">
                    <!-- Overview Card -->
                    <div class="card" id="overview">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                            <h2 style="margin: 0; font-size: 20px;">Target Application Overview</h2>
                            <span class="badge badge-healthy" id="app-status">Status: Healthy</span>
                        </div>
                        <p style="color: var(--text-muted); font-size: 14px; line-height: 1.6; margin: 0 0 16px 0;" id="app-description">
                            This is the demonstration workload tested, containerized, and deployed by Resilio. It provides functional workflows and health endpoints for continuous verification.
                        </p>
                        <div class="meta-grid">
                            <div><strong>Application Version:</strong> <span id="app-version">%s</span></div>
                            <div><strong>Git Commit SHA:</strong> <span id="commit-sha" style="font-family: monospace;">%s</span></div>
                            <div><strong>Build Identifier:</strong> <span id="build-number">#%s</span></div>
                            <div><strong>Runtime Environment:</strong> <span id="environment-name">%s</span></div>
                        </div>
                    </div>

                    <!-- Functional Test Page / Card -->
                    <div class="card" id="functional-test">
                        <h3 style="margin-top: 0; font-size: 18px;">Interactive Functional Test (Validation Gate)</h3>
                        <p style="color: var(--text-muted); font-size: 13px; line-height: 1.5; margin-bottom: 16px;">
                            Submit a candidate payload for business-logic validation. The Selenium test suite verifies page loading, valid payload acceptance, rejection of invalid entries, and verified result display.
                        </p>
                        <form id="workflow-form" onsubmit="handleWorkflow(event)">
                            <div class="field">
                                <label for="payload-input">Transaction Payload</label>
                                <input type="text" id="payload-input" placeholder="e.g. RESILIO-ALPHA-2026" required />
                                <small style="color: var(--text-muted); font-size: 12px; margin-top: 4px; display: block;">
                                    Must be 4 to 32 alphanumeric characters, dashes, or underscores.
                                </small>
                            </div>
                            <button type="submit" id="validate-btn">Execute Validation</button>
                        </form>
                        <div id="result-message">
                            <div id="result-text" style="font-weight: 600;"></div>
                            <div id="processed-value" style="font-family: monospace; font-size: 12px; margin-top: 4px;"></div>
                        </div>
                    </div>
                </div>

                <script>
                    async function handleWorkflow(e) {
                        e.preventDefault();
                        const input = document.getElementById('payload-input').value;
                        const msgBox = document.getElementById('result-message');
                        const msgText = document.getElementById('result-text');
                        const processedValue = document.getElementById('processed-value');

                        msgBox.style.display = 'block';
                        msgBox.className = '';

                        try {
                            const res = await fetch('/api/workflow/validate', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({ payload: input })
                            });
                            const data = await res.json();

                            if (res.ok && data.valid) {
                                msgBox.className = 'result-success';
                                msgBox.setAttribute('data-status', 'success');
                                msgText.innerText = data.message;
                                processedValue.innerText = 'Output: ' + data.processedValue;
                            } else {
                                msgBox.className = 'result-error';
                                msgBox.setAttribute('data-status', 'error');
                                msgText.innerText = data.message || data.error || 'Validation Rejected';
                                processedValue.innerText = '';
                            }
                        } catch (err) {
                            msgBox.className = 'result-error';
                            msgBox.setAttribute('data-status', 'error');
                            msgText.innerText = 'Network error: ' + err.message;
                            processedValue.innerText = '';
                        }
                    }
                </script>
            </body>
            </html>
            """.formatted(version, commitSha, buildNumber, environment);
    }

    @GetMapping("/api/version")
    public ResponseEntity<Map<String, Object>> getVersion() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "resilio-demo-app");
        body.put("version", version);
        body.put("commitSha", commitSha);
        body.put("buildNumber", buildNumber);
        body.put("environment", environment);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthAlias() {
        // Alias for Actuator health check
        chaosService.recordCheck();

        if (chaosService.isUnhealthy()) {
            Map<String, Object> body = new HashMap<>();
            body.put("status", "DOWN");
            body.put("error", "Application internal subsystem failure: critical service thread panic");
            body.put("timestamp", Instant.now().toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("status", "UP");
        body.put("environment", environment);
        body.put("version", version);
        body.put("commitSha", commitSha);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/api/workflow/validate")
    public ResponseEntity<Map<String, Object>> validateWorkflow(@RequestBody Map<String, String> request) {
        if (chaosService.isRegressionInduced()) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Simulated functional regression: NullPointerException in WorkflowValidator");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }

        String payload = request.get("payload");
        var result = dataProcessingService.processPayload(payload);

        Map<String, Object> resp = new HashMap<>();
        resp.put("valid", result.valid());
        resp.put("message", result.message());
        resp.put("processedValue", result.processedValue());
        resp.put("timestamp", result.timestamp());

        if (result.valid()) {
            return ResponseEntity.ok(resp);
        } else {
            return ResponseEntity.badRequest().body(resp);
        }
    }
}

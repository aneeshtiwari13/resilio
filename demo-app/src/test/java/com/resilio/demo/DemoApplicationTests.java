package com.resilio.demo;

import com.resilio.demo.service.ChaosStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DemoApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChaosStateService chaosService;

    @BeforeEach
    void setUp() {
        chaosService.reset();
    }

    @Test
    void testHomePageLoadsWithNavAndTitle() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Resilio Demo Application")))
                .andExpect(content().string(containsString("main-nav")))
                .andExpect(content().string(containsString("workflow-form")));
    }

    @Test
    void testActuatorHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testCustomHealthEndpoint() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testVersionEndpoint() throws Exception {
        mockMvc.perform(get("/api/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("resilio-demo-app"))
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(jsonPath("$.commitSha").isNotEmpty());
    }

    @Test
    void testValidWorkflowInput() throws Exception {
        String payload = "{\"payload\": \"RESILIO-ALPHA-2026\"}";
        mockMvc.perform(post("/api/workflow/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Validation Passed")))
                .andExpect(jsonPath("$.processedValue").value("PROCESSED::RESILIO-ALPHA-2026"));
    }

    @Test
    void testInvalidWorkflowInput() throws Exception {
        String payload = "{\"payload\": \"abc\"}"; // Too short
        mockMvc.perform(post("/api/workflow/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Validation Error")));
    }
}

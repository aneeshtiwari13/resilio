package com.resilio.platform;

import com.resilio.platform.model.RecoveryOperation;
import com.resilio.platform.repository.IncidentRepository;
import com.resilio.platform.repository.RecoveryOperationRepository;
import com.resilio.platform.service.DistributedRecoveryLockService;
import com.resilio.platform.service.RecoveryLockConflictException;
import com.resilio.platform.service.RecoveryReconciliationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Phase3SecurityAndDistributedLockTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DistributedRecoveryLockService lockService;

    @Autowired
    private RecoveryOperationRepository operationRepo;

    @Autowired
    private RecoveryReconciliationService reconciliationService;

    @Autowired
    private IncidentRepository incidentRepo;

    @Test
    @DisplayName("Security: Anonymous request to trigger recovery is rejected with 401 Unauthorized")
    void testAnonymousRecoveryRejected() throws Exception {
        mockMvc.perform(post("/api/recovery/trigger")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"incidentId\": 9999}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Security: Viewer role is forbidden from triggering mutations (403 Forbidden)")
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    void testViewerRoleForbiddenFromMutation() throws Exception {
        mockMvc.perform(post("/api/recovery/trigger")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"incidentId\": 9999}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("Security: Viewer role can read public and read-only endpoints")
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    void testViewerCanAccessReadOnlyEndpoints() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api/recovery/status"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Distributed Locking: Simultaneous lock acquisition on target causes 409 Conflict")
    void testConcurrentDistributedLockRejection() {
        String target = "TEST-TARGET-PROD";
        String idempotencyKey1 = "key-req-001";
        String idempotencyKey2 = "key-req-002";

        DistributedRecoveryLockService.LockAcquisitionResult res1 =
                lockService.tryAcquireLock(target, "operator1", idempotencyKey1);
        assertNotNull(res1.operation());
        assertEquals("IN_PROGRESS", res1.operation().getOperationState());

        // Attempt second concurrent lock with different idempotency key
        assertThrows(RecoveryLockConflictException.class, () -> {
            lockService.tryAcquireLock(target, "operator2", idempotencyKey2);
        });

        // Clean up
        lockService.completeLock(res1.operation().getId(), "Test completed", true);
    }

    @Test
    @DisplayName("Idempotency: Replaying request with same idempotency key returns existing operation")
    void testIdempotentRequestReplay() {
        String target = "TEST-IDEMPOTENT-TARGET";
        String key = "idem-key-abc-123";

        DistributedRecoveryLockService.LockAcquisitionResult first =
                lockService.tryAcquireLock(target, "operator1", key);
        assertNotNull(first.operation());
        assertFalse(first.isReplay());

        // Replaying with identical key returns first without exception
        DistributedRecoveryLockService.LockAcquisitionResult second =
                lockService.tryAcquireLock(target, "operator1", key);
        assertEquals(first.operation().getId(), second.operation().getId());
        assertTrue(second.isReplay());
        assertEquals("IN_PROGRESS", second.operation().getOperationState());

        lockService.completeLock(first.operation().getId(), "Idempotent test completed", true);
    }

    @Test
    @DisplayName("Distributed Locking: Stale lease expires and allows subsequent lock acquisition")
    void testStaleLeaseExpiration() {
        String target = "TEST-EXPIRING-TARGET";

        // Create an expired operation manually
        RecoveryOperation staleOp = new RecoveryOperation(
                "REC-EXPIRED-999", "INC-01", target, "EXECUTE_ROLLBACK",
                "old-worker", "expired-key", Instant.now().minusSeconds(120),
                "v1.0.0", "v0.9.0"
        );
        staleOp.setOperationState("IN_PROGRESS");
        operationRepo.save(staleOp);

        // Acquiring a new lock must detect expiration, mark old operation ABORTED, and succeed
        DistributedRecoveryLockService.LockAcquisitionResult res =
                lockService.tryAcquireLock(target, "new-worker", "new-key");
        assertNotNull(res.operation());
        assertEquals("IN_PROGRESS", res.operation().getOperationState());

        RecoveryOperation refreshedStale = operationRepo.findById(staleOp.getId()).orElseThrow();
        assertEquals("ABORTED", refreshedStale.getOperationState());

        lockService.completeLock(res.operation().getId(), "Cleanup completed", true);
    }

    @Test
    @DisplayName("Startup Reconciliation: Interrupted recovery operations are detected and safely aborted")
    void testStartupReconciliationAbortsInterruptedOperations() {
        String target = "TEST-RECON-TARGET";
        RecoveryOperation interrupted = new RecoveryOperation(
                "REC-INTERRUPTED-888", "INC-02", target, "EXECUTE_ROLLBACK",
                "crashed-node", "recon-key", Instant.now().plusSeconds(300),
                "v2.0.0", "v1.0.0"
        );
        interrupted.setOperationState("IN_PROGRESS");
        operationRepo.save(interrupted);

        // Run reconciliation
        reconciliationService.reconcileOnStartup();

        RecoveryOperation afterRecon = operationRepo.findById(interrupted.getId()).orElseThrow();
        assertEquals("ABORTED", afterRecon.getOperationState());
        assertTrue(afterRecon.getFinalResult().contains("aborted by RecoveryReconciliationService on startup"));
    }

    @Test
    @DisplayName("Observability: Correlation ID header is propagated in response")
    void testCorrelationIdHeaderPropagated() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header("X-Correlation-ID", "custom-trace-12345"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String header = result.getResponse().getHeader("X-Correlation-ID");
                    assertEquals("custom-trace-12345", header);
                });
    }
}

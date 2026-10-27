# RESILIO — Recovery State Machine & Startup Reconciliation

**Platform:** RESILIO — Self-Healing CI/CD Platform  
**Document:** Recovery Reconciliation & Distributed Locking Specification  
**Phase:** Phase 3 Architecture  

---

## 1. Overview & Problem Statement

In distributed CI/CD and self-healing deployment platforms, uncoordinated recovery actions and unhandled server restarts present severe reliability hazards:

1. **Split-Brain State Hazard**: If a recovery decision engine crashes mid-execution (after modifying an internal state variable but before reloading Nginx, or after updating Nginx upstream without recording the release in the database), the next server boot may assume an erroneous active slot or attempt redundant conflicting actions.
2. **Concurrent Recovery Race Hazard**: When multiple worker nodes, parallel health probes, or administrative operators initiate rollbacks simultaneously, concurrent mutations can clobber routing configurations, resulting in traffic flapping and failed rollbacks.
3. **Stale Lease Abandonment Hazard**: If a node holding a lock terminates abnormally (e.g., OOM killed or VM restarted), other nodes could be blocked indefinitely if locks are purely process-local or lack TTL-based lease expiration.

Phase 3 resolves these hazards through **Startup State Reconciliation**, **Database-Backed Distributed Recovery Locks**, and **Idempotency Key Tracking**.

---

## 2. Startup Reconciliation Sequence

Upon JVM initialization, Spring Boot publishes the `ApplicationReadyEvent`. The `RecoveryReconciliationService` hooks into this lifecycle event:

```mermaid
sequenceDiagram
    autonumber
    participant Spring as Spring Boot Bootstrapper
    participant Recon as RecoveryReconciliationService
    participant Disk as Filesystem (deploy/nginx/upstream.conf)
    participant DB as Platform Database (PostgreSQL/H2)
    participant Incident as IncidentService

    Spring->>Recon: @EventListener(ApplicationReadyEvent)
    Recon->>Disk: Inspect active upstream block (BLUE vs GREEN)
    Disk-->>Recon: Returns current physical active slot
    Recon->>DB: Reconcile active release in releases table
    Recon->>DB: Query findIncompleteOperations() (IN_PROGRESS / ACQUIRED)
    alt Incomplete Operations Found (Server Crashed Mid-Recovery)
        Recon->>DB: Mark operations as ABORTED
        Recon->>DB: Record finalResult ("Interrupted by server restart...")
        Recon->>Incident: Record timeline event RECONCILIATION_ABORTED
        Recon->>Incident: Escalate associated incident to MANUAL_INTERVENTION_REQUIRED
    else No Incomplete Operations
        Recon->>DB: Mark reconciliation as CLEAN
    end
    Recon-->>Spring: Reconciliation Complete (Ready for Traffic)
```

### Reconciliation Invariants:
1. **Source of Truth for Live Traffic**: The disk configuration (`deploy/nginx/upstream.conf`) reflects what Nginx actually serves. If the database active release does not match the disk slot, the database state is reconciled to reflect physical reality.
2. **Interrupted Operation Abortion**: Any `RecoveryOperation` left in an `IN_PROGRESS` or `ACQUIRED` state at restart was not verified to completion. To avoid split-brain execution, it is transitioned to `ABORTED`, and its associated incident is escalated to `MANUAL_INTERVENTION_REQUIRED`.

---

## 3. Distributed Recovery Locking Architecture

Process-local locks (such as `ReentrantLock`) only synchronize threads within a single JVM. To coordinate across multiple instances or clustered deployments, Resilio persists recovery operations to the `recovery_operations` table with optimistic locking (`@Version`) and lease TTLs.

### Schema: `recovery_operations`
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | `BIGINT PRIMARY KEY` | Auto-increment primary key |
| `recovery_attempt_id` | `VARCHAR(64) UNIQUE` | Unique execution identifier (e.g. `REC-1793033...`) |
| `deployment_target` | `VARCHAR(32)` | Target environment (e.g. `PRODUCTION`) |
| `requested_action` | `VARCHAR(64)` | Action being performed (e.g. `EXECUTE_ROLLBACK`) |
| `lock_owner` | `VARCHAR(128)` | Node/thread holding the lock (e.g. `node-1:thread-2`) |
| `operation_state` | `VARCHAR(32)` | `ACQUIRED`, `IN_PROGRESS`, `COMPLETED`, `FAILED`, `ABORTED` |
| `idempotency_key` | `VARCHAR(128)` | Client-supplied key for duplicate suppression |
| `lease_expires_at` | `TIMESTAMP` | Absolute expiration timestamp (TTL) |
| `final_result` | `TEXT` | Execution summary, verification output, or abort reason |
| `version` | `BIGINT` | JPA `@Version` column preventing concurrent write clobbering |

---

## 4. Concurrency & Idempotency Guarantees

### 4.1 Concurrent Lock Conflict (HTTP 409)
When a recovery operation is attempted while an unexpired lock exists for the same target, `DistributedRecoveryLockService` throws `RecoveryLockConflictException`:
```json
{
  "error": "Conflict",
  "status": 409,
  "message": "A recovery operation is already active for deployment target 'PRODUCTION'. Concurrent operation rejected to prevent split-brain state.",
  "activeAttemptId": "REC-1793033177920-749",
  "lockOwner": "operator1"
}
```

### 4.2 Idempotent Request Replay
If a network timeout or automated retry sends the same `Idempotency-Key` header, Resilio detects the existing record in `findByIdempotencyKey` and returns the existing operation without triggering redundant rollbacks:
```http
POST /api/recovery/trigger HTTP/1.1
Idempotency-Key: manual-roll-948102941
Authorization: Basic b3BlcmF0b3I6T3BlcmF0b3JAUmVzaWxpbzIwMjYh
```

### 4.3 Lease Expiration & Auto-Eviction
If a node holding a lock crashes without completing, subsequent requests check `now > leaseExpiresAt`. If the lease has expired, the stale lock is automatically transitioned to `ABORTED`, and the new requester safely acquires the lock.

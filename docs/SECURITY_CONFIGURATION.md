# RESILIO — Security Configuration & Role-Based Access Control (RBAC)

**Platform:** RESILIO — Self-Healing CI/CD Platform  
**Document:** Security Architecture & Authorization Guide  
**Phase:** Phase 3 Architecture  

---

## 1. Security Overview

Phase 3 introduces enterprise-grade authentication, authorization, secret protection, and audit correlation for the RESILIO platform:

- **Authentication Framework**: Spring Security 6.2 with `ResilioUserDetailsService`.
- **Credential Storage**: Passwords hashed using industry-standard `BCryptPasswordEncoder` (work factor 10).
- **Role-Based Access Control (RBAC)**: Enforced via `SecurityFilterChain` matchers and `@EnableMethodSecurity`.
- **Request Tracing**: High-priority `CorrelationIdFilter` generating and propagating `X-Correlation-ID` across SLF4J MDC and HTTP responses.
- **Secret Redaction**: `SecretMasker` filter utility sanitizing passwords, tokens, and Authorization headers in logs.

---

## 2. Role-Based Access Control Matrix

| Endpoint Pattern | Method | Permitted Roles | Description |
| :--- | :--- | :--- | :--- |
| `/`, `/index.html`, `/static/**`, `/favicon.ico` | `GET` | **Permit All (Public)** | Dashboard assets & UI |
| `/api/health`, `/actuator/health`, `/actuator/info` | `GET` | **Permit All (Public)** | Load balancer and health probes |
| `/api/auth/**` | `GET, POST` | **Permit All (Public)** | Login, authentication status |
| `/api/metrics`, `/api/incidents`, `/api/releases` | `GET` | `ROLE_VIEWER`, `ROLE_OPERATOR`, `ROLE_ADMIN` | Read-only telemetry |
| `/api/recovery/status`, `/api/recovery/operations` | `GET` | `ROLE_VIEWER`, `ROLE_OPERATOR`, `ROLE_ADMIN` | Distributed lock & recovery status |
| `/api/recovery/trigger` | `POST` | `ROLE_OPERATOR`, `ROLE_ADMIN` | Execute rollback & self-healing |
| `/api/releases/**/verify`, `/promote` | `POST` | `ROLE_OPERATOR`, `ROLE_ADMIN` | Candidate verification & slot promotion |
| `/api/pipelines/trigger` | `POST` | `ROLE_OPERATOR`, `ROLE_ADMIN` | Trigger CI/CD pipeline |
| `/api/admin/**`, `/api/users/**` | `*` | `ROLE_ADMIN` | User account administration |

---

## 3. Seed Accounts & Environment Overrides

On initial startup, `SecurityBootstrapService` provisions default administrative and operational accounts if not already present in the database:

| Username | Default Role | Default Password | Environment Variable Override |
| :--- | :--- | :--- | :--- |
| `admin` | `ROLE_ADMIN` | `Admin@Resilio2026!` | `ADMIN_INITIAL_PASSWORD` |
| `operator` | `ROLE_OPERATOR` | `Operator@Resilio2026!` | `OPERATOR_INITIAL_PASSWORD` |
| `viewer` | `ROLE_VIEWER` | `Viewer@Resilio2026!` | `VIEWER_INITIAL_PASSWORD` |

> [!CAUTION]
> For production environments, always override default passwords using environment variables or a secrets manager prior to initial startup.

---

## 4. API Authentication Examples

### 4.1 Authenticating via HTTP Basic Header
```bash
curl -X POST http://localhost:8090/api/recovery/trigger \
  -u operator:Operator@Resilio2026! \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: op-lock-001" \
  -d '{"reason":"Manual test rollback"}'
```

### 4.2 Handling 401 Unauthorized (Missing Credentials)
```json
{
  "error": "Unauthorized",
  "message": "Authentication required to access this resource"
}
```

### 4.3 Handling 403 Forbidden (Insufficient Permissions)
If a user with `ROLE_VIEWER` attempts to trigger a mutation endpoint:
```json
{
  "error": "Forbidden",
  "message": "Insufficient permissions for user role"
}
```

---

## 5. Distributed Request Tracing (`X-Correlation-ID`)

All requests processed by the platform pass through `CorrelationIdFilter`:
1. If the incoming request has `X-Correlation-ID` or `X-Request-ID`, it is retained.
2. Otherwise, a UUID is generated.
3. The ID is injected into SLF4J `MDC` with key `correlationId` so all log outputs include the trace context.
4. The response includes `X-Correlation-ID: <id>`.

---

## 6. Secret Redaction (`SecretMasker`)

To prevent secret leakage in logs, `SecretMasker` intercepts text strings and automatically replaces:
- Passwords and tokens (`password: "..."`, `token: "..."` &rarr; `[REDACTED]`)
- Bearer tokens (`Bearer eyJ...` &rarr; `Bearer [REDACTED]`)
- Basic authentication credentials (`Basic b3Bl...` &rarr; `Basic [REDACTED]`)

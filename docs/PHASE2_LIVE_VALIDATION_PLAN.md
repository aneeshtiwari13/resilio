# RESILIO — Phase 2 Live Validation Plan
**Document:** Live Validation Test Protocol & Environment Discovery  
**Platform:** RESILIO — Self-Healing CI/CD Platform  
**Target Architecture:** Windows Host, Docker Compose, Nginx, Jenkins, PostgreSQL, Spring Boot, React, Selenium  

---

## 1. Environment Discovery & Port Mapping

Based on the actual inspected repository:

| Component | Repository Path / Service | Host Port | Container Port | Protocol / Technology |
|---|---|---|---|---|
| **Resilio Control Backend** | `backend/` (`resilio-backend`) | `8090` | `8090` | Java 21 / Spring Boot 3.2.4 REST API |
| **Resilio Frontend UI** | `frontend/` (`resilio-frontend`) | `3000` (Docker) / `8090` (Embedded Static) | `80` | React 18, Vite, TypeScript, Tailwind CSS |
| **PostgreSQL Database** | `postgres` (Compose) | `5432` | `5432` | PostgreSQL 16 Alpine (Fallback: H2 in-memory) |
| **Nginx Reverse Proxy** | `deploy/nginx/` (`nginx-proxy`) | `80` | `80` | Nginx 1.25 Alpine reverse proxy |
| **Blue Workload Slot** | `demo-app/` (`demo-app-blue`) | `8081` | `8080` | Spring Boot Actuator (`v1.0.0`) |
| **Green Workload Slot** | `demo-app/` (`demo-app-green`) | `8082` | `8080` | Spring Boot Actuator (`v1.1.0`) |
| **Selenium Browser Grid** | `selenium-chrome` (Compose) | `4444`, `7900` | `4444`, `7900` | Selenium Standalone Chrome (VNC on 7900) |
| **Jenkins Automation Server** | `jenkins/` (`jenkins`) | `8085` | `8080` | Jenkins LTS (pipeline: `jenkins/Jenkinsfile`) |

---

## 2. Docker & Infrastructure Dependency Check

### Status Check Procedure:
```powershell
# 1. Verify Docker Daemon availability
docker info
docker ps

# 2. Verify Java and Maven versions
java -version
.\mvnw.cmd -version
```

### Dependency Availability Finding:
- **Java**: Eclipse Adoptium JDK 25.0.4.1 installed and operational.
- **Maven**: Standalone Apache Maven 3.9.6 wrapper (`mvnw.cmd`) operational.
- **Docker Daemon**: `docker` CLI is not installed / not present in PATH on this Windows host.
  - *Status*: `BLOCKED` for live container execution on host.
  - *Remediation Steps*:
    1. Install Docker Desktop for Windows (with WSL 2 backend).
    2. Start Docker Desktop and ensure `docker.exe` is added to system PATH.
    3. Run `docker-compose up -d` in `C:\Users\tiwar\.gemini\antigravity\scratch\resilio`.

---

## 3. Test Suites & Validation Matrix

The platform validation is structured into separate, unblurred categories:

### Category 1: Unit & In-Process Integration Tests (Pass independently on host)
- **Suite**: `com.resilio.platform.Phase2AcceptanceScenariosTest` (8 scenarios)
- **Suite**: `com.resilio.platform.RecoveryDecisionServiceTest` (6 tests)
- **Suite**: `com.resilio.platform.FailureClassificationServiceTest` (7 tests)
- **Suite**: `com.resilio.platform.DecisionEngineTest` (5 tests)
- **Execution Command**:
  ```powershell
  powershell -ExecutionPolicy Bypass -File scripts\test_phase2_scenarios.ps1
  ```

### Category 2: Live Docker Container Tests (Requires Docker daemon)
- **Test ID**: `DOCKER-01` (PostgreSQL container health: `pg_isready`)
- **Test ID**: `DOCKER-02` (Blue container startup on port 8081)
- **Test ID**: `DOCKER-03` (Green container startup on port 8082)
- **Test ID**: `DOCKER-04` (Nginx proxy container binding port 80)
- **Test ID**: `DOCKER-05` (Jenkins container startup on port 8085)
- **Execution Command**:
  ```powershell
  docker-compose up -d
  docker-compose ps
  ```

### Category 3: Live Nginx Routing & Atomic Swap Tests
- **Test ID**: `NGINX-01` (Syntax check: `nginx -t`)
- **Test ID**: `NGINX-02` (Atomic file swap: `upstream.conf.tmp` -> `upstream.conf`)
- **Test ID**: `NGINX-03` (Live reload: `nginx -s reload` without dropped connections)
- **Test ID**: `NGINX-04` (Public proxy response verification on `http://localhost:80/api/version`)

### Category 4: Real Selenium WebDriver Browser Tests
- **Suite**: `com.resilio.tests.CandidateValidationTest` (pre-switch candidate UI validation)
- **Suite**: `com.resilio.tests.RecoveryVerificationTest` (post-rollback UI verification)
- **Execution Command**:
  ```powershell
  cd selenium-tests
  .\mvnw.cmd test -Dtest=CandidateValidationTest -Dtarget.base.url=http://localhost:8082
  .\mvnw.cmd test -Dtest=RecoveryVerificationTest -Dtarget.base.url=http://localhost:80 -Dexpected.version=1.0.0
  ```

### Category 5: Live End-to-End Rollback & Verified Recovery Test
- **Test ID**: `E2E-ROLLBACK-01` (Candidate promotion -> Chaos injection -> 3x Health failure detection -> Nginx upstream swap -> 7-point post-recovery verification -> Incident resolution).

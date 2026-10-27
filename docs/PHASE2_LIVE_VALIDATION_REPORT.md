# RESILIO — Phase 2 Live Validation Report
**Execution Date**: October 26, 2026  
**Environment**: Windows Host (Build: Windows 10/11 x64, Java 25.0.4.1 Adoptium, Apache Maven 3.9.6)  
**Execution Context**: Local Developer Machine  

---

## 1. Validation Categories & Outcome Summary

As mandated by Phase 3 rules, validation results are partitioned into strict categories rather than reported as an undifferentiated pass rate. No mock or test double is used to claim live infrastructure verification.

| Category | Total | PASS | FAIL | BLOCKED | NOT RUN | Pass Rate (Executable) |
|---|---|---|---|---|---|---|
| **1. Unit & Decision Tests** | 18 | 18 | 0 | 0 | 0 | 100% |
| **2. In-Process Integration Tests** | 8 | 8 | 0 | 0 | 0 | 100% |
| **3. Live Docker Containers** | 5 | 0 | 0 | 5 | 0 | 0% (BLOCKED) |
| **4. Live Nginx Process Reload** | 4 | 0 | 0 | 4 | 0 | 0% (BLOCKED) |
| **5. Real Selenium WebDriver Tests** | 2 | 0 | 0 | 2 | 0 | 0% (BLOCKED) |
| **6. Live End-to-End Rollback** | 1 | 0 | 0 | 1 | 0 | 0% (BLOCKED) |

---

## 2. Granular Test Breakdown

### Category 1: Unit & Decision Tests (Executed)
| Test ID | Test Class / Suite | Command | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|
| **UNIT-01** | `DecisionEngineTest` (5 tests) | `.\mvnw.cmd test -Dtest=DecisionEngineTest` | Rules 1-5 return correct state mappings | 5/5 tests passed in 0.103s | **PASS** |
| **UNIT-02** | `FailureClassificationServiceTest` (7 tests) | `.\mvnw.cmd test -Dtest=FailureClassificationServiceTest` | Surefire logs, exit codes classified accurately | 7/7 tests passed in 0.021s | **PASS** |
| **UNIT-03** | `RecoveryDecisionServiceTest` (6 tests) | `.\mvnw.cmd test -Dtest=RecoveryDecisionServiceTest` | Concurrency lock, candidate status transitions | 6/6 tests passed in 0.502s | **PASS** |

### Category 2: In-Process Integration Tests (Executed)
| Test ID | Test Class / Suite | Command | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|
| **INT-01** | `Phase2AcceptanceScenariosTest.testScenario1` | `.\mvnw.cmd test -Dtest=Phase2AcceptanceScenariosTest#testScenario1*` | Build failure halts pipeline, active release kept | Exit code 0, `NO_DEPLOYMENT_REQUIRED` | **PASS** |
| **INT-02** | `Phase2AcceptanceScenariosTest.testScenario2` | `.\mvnw.cmd test -Dtest=Phase2AcceptanceScenariosTest#testScenario2*` | Unit test regression halts deployment | Exit code 0, `NO_DEPLOYMENT_REQUIRED` | **PASS** |
| **INT-03** | `Phase2AcceptanceScenariosTest.testScenario3` | `.\mvnw.cmd test -Dtest=Phase2AcceptanceScenariosTest#testScenario3*` | Container startup failure quarantines candidate | Exit code 0, `CANDIDATE_REJECTED` | **PASS** |
| **INT-04** | `Phase2AcceptanceScenariosTest.testScenario4` | `.\mvnw.cmd test -Dtest=Phase2AcceptanceScenariosTest#testScenario4*` | Selenium regression aborts traffic promotion | Exit code 0, `CANDIDATE_REJECTED` | **PASS** |
| **INT-05** | `Phase2AcceptanceScenariosTest.testScenario5` | `.\mvnw.cmd test -Dtest=Phase2AcceptanceScenariosTest#testScenario5*` | Post-activation failure triggers rollback | Exit code 0, `EXECUTE_ROLLBACK` & `RECOVERED` | **PASS** |
| **INT-06** | `Phase2AcceptanceScenariosTest.testScenario6` | `.\mvnw.cmd test -Dtest=Phase2AcceptanceScenariosTest#testScenario6*` | Missing known-good escalates to operator | Exit code 0, `MANUAL_INTERVENTION_REQUIRED` | **PASS** |
| **INT-07** | `Phase2AcceptanceScenariosTest.testScenario7` | `.\mvnw.cmd test -Dtest=Phase2AcceptanceScenariosTest#testScenario7*` | Restored release failing audit tagged failed | Exit code 0, `RECOVERY_FAILED` | **PASS** |
| **INT-08** | `Phase2AcceptanceScenariosTest.testScenario8` | `.\mvnw.cmd test -Dtest=Phase2AcceptanceScenariosTest#testScenario8*` | Concurrency lock prevents flapping | Exit code 0, lock state verified | **PASS** |

### Category 3: Live Docker Container Tests (Blocked)
| Test ID | Target Service | Command | Dependency | Root Cause / Reason | Status |
|---|---|---|---|---|---|
| **DOCKER-01** | `resilio-postgres` (5432) | `docker-compose up -d postgres` | Docker daemon | `docker` command not found on host PATH | **BLOCKED** |
| **DOCKER-02** | `demo-app-blue` (8081) | `docker-compose up -d demo-app-blue` | Docker daemon | Docker Desktop not running | **BLOCKED** |
| **DOCKER-03** | `demo-app-green` (8082) | `docker-compose up -d demo-app-green` | Docker daemon | Docker Desktop not running | **BLOCKED** |
| **DOCKER-04** | `nginx-proxy` (80) | `docker-compose up -d nginx-proxy` | Docker daemon | Docker Desktop not running | **BLOCKED** |
| **DOCKER-05** | `resilio-jenkins` (8085) | `docker-compose up -d jenkins` | Docker daemon | Docker Desktop not running | **BLOCKED** |

### Category 4: Live Nginx Routing Tests (Blocked)
| Test ID | Operation | Target | Dependency | Root Cause / Reason | Status |
|---|---|---|---|---|---|
| **NGINX-01** | `nginx -t` | Syntax verification | `nginx` binary | Nginx is containerized; requires Docker | **BLOCKED** |
| **NGINX-02** | Atomic file write | `deploy/nginx/upstream.conf` | Filesystem | File writing succeeded on host; reload blocked | **PASS** (file) / **BLOCKED** (live) |
| **NGINX-03** | `nginx -s reload` | Live proxy reload | `nginx` process | Requires Docker container or Windows Nginx service | **BLOCKED** |
| **NGINX-04** | Port 80 Traffic Route | `http://localhost:80/api/version` | Port 80 listener | No active listener on host port 80 | **BLOCKED** |

### Category 5: Real Selenium WebDriver Tests (Blocked)
| Test ID | Test Suite | Target URL | Dependency | Root Cause / Reason | Status |
|---|---|---|---|---|---|
| **SELENIUM-01** | `CandidateValidationTest` | `http://localhost:8082` | Chrome / Remote Grid | Requires running container `selenium-chrome:4444` | **BLOCKED** |
| **SELENIUM-02** | `RecoveryVerificationTest` | `http://localhost:80` | Chrome / Remote Grid | Requires running container `selenium-chrome:4444` | **BLOCKED** |

### Category 6: Live End-to-End Rollback (Blocked)
| Test ID | Procedure | Target | Dependency | Root Cause / Reason | Status |
|---|---|---|---|---|---|
| **E2E-01** | Real Chaos Fault Injection & Rollback | Live Blue/Green containers | Docker + Nginx | Requires active container topology | **BLOCKED** |

---

## 3. Clear Remediation Plan for Blocked Tests

To transition blocked tests to executed:
1. **Docker Desktop**: Install Docker Desktop for Windows and enable the WSL 2 engine.
2. **Launch Services**:
   ```powershell
   docker-compose up -d
   ```
3. **Run Live Verification**:
   ```powershell
   # 1. Run live Selenium suite against Chrome container
   cd selenium-tests
   .\mvnw.cmd test -Dselenium.grid.url=http://localhost:4444/wd/hub -Dtarget.base.url=http://localhost:8082

   # 2. Verify Nginx live reload
   docker exec resilio-nginx-proxy nginx -s reload
   ```

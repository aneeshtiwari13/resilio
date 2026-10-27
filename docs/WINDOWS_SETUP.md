# RESILIO — Windows Development & Operations Guide

**Platform:** RESILIO — Self-Healing CI/CD Platform  
**Document:** Windows Environment & Infrastructure Setup  
**Target OS:** Windows 10 / Windows 11 (PowerShell 5.1 / PowerShell 7+)  

---

## 1. System Requirements & Current Host Discovery

| Component | Minimum Version | Discovered on Current Host | Status |
| :--- | :--- | :--- | :--- |
| **Operating System** | Windows 10/11 x64 | Windows 10/11 (Build 26100) | Supported |
| **Java Development Kit** | Java 21+ | Java 25.0.4.1 (Eclipse Adoptium) | Operational |
| **Build Tool** | Apache Maven 3.9+ | Maven 3.9.6 (via `mvnw.cmd`) | Operational |
| **Shell** | PowerShell 5.1+ | PowerShell (ExecutionPolicy Bypass) | Operational |
| **Web Browser** | Chrome or Edge | Microsoft Edge installed | Operational |
| **Container Engine** | Docker Desktop 4.25+ | **Not Installed on Host** | BLOCKED (Live Containers) |

---

## 2. In-Process Validation Mode (Zero-Docker Prerequisite)

You can build, test, run, and evaluate the full Resilio platform in-process on Windows without Docker installed. The platform uses in-memory H2 (in PostgreSQL compatibility mode), embedded mock drivers, and virtual routing to test all 7 recovery rules:

### 2.1 Compiling and Running Backend Tests
From the root or `backend/` directory in PowerShell:
```powershell
cd backend
.\mvnw.cmd test
```
*Expected Result:* 34 tests run across 5 test suites with 0 failures.

### 2.2 Running the Phase 2 Acceptance Scenarios Suite
```powershell
powershell -ExecutionPolicy Bypass -File scripts\test_phase2_scenarios.ps1
```
*Expected Result:* 26 of 26 acceptance tests pass across all 8 failure and recovery scenarios.

### 2.3 Starting the Resilio Control Center
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
Open your browser at `http://localhost:8090` to access the interactive dashboard.

---

## 3. Full Live Infrastructure Mode (Unblocking Docker, Nginx, & Selenium)

To execute live Docker container builds, real Nginx traffic switching, and standalone Selenium Grid browser tests, perform the following setup:

### Step 1: Install Docker Desktop for Windows
1. Download Docker Desktop from [https://www.docker.com/products/docker-desktop/](https://www.docker.com/products/docker-desktop/).
2. Run the installer and ensure **Use WSL 2 instead of Hyper-V** is selected.
3. Once installed, start Docker Desktop and verify the daemon in PowerShell:
   ```powershell
   docker --version
   docker compose version
   ```

### Step 2: Spin Up the Supporting Stack
From the project root:
```powershell
docker compose -f docker-compose.yml up -d postgres jenkins selenium nginx
```
This provisions:
- PostgreSQL on `localhost:5432`
- Jenkins CI/CD on `localhost:8085`
- Selenium Chrome Grid on `localhost:4444`
- Nginx Reverse Proxy on `localhost:80`

### Step 3: Run the Full Live Acceptance Suite
```powershell
powershell -ExecutionPolicy Bypass -File scripts\test_live_infrastructure.ps1
```

---

## 4. Common Windows Troubleshooting

- **PowerShell Script Execution Policy Error**:
  Run PowerShell with `-ExecutionPolicy Bypass`:
  ```powershell
  powershell -ExecutionPolicy Bypass -File scripts\<script-name>.ps1
  ```
- **Port Conflicts**:
  If port 8090, 8085, or 5432 is already in use by local services, update the port mappings in `backend/src/main/resources/application.properties` or set environment variables:
  ```powershell
  $env:SERVER_PORT = "8095"
  ```

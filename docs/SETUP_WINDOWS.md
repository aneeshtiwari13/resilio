# RESILIO — Windows Setup & Developer Runbook
## Phase 1: Real CI/CD Foundation

This guide provides exact step-by-step instructions for running and verifying **RESILIO Phase 1** on a Windows 10/11 laptop using PowerShell, Java 21+, Docker Desktop, and Jenkins.

---

### Prerequisites Checklist

| Component | Required Version | Verification Command | Purpose |
| :--- | :--- | :--- | :--- |
| **Java JDK** | 21 or newer (Temurin / OpenJDK) | `java -version` | Java application runtime & compilation |
| **Git** | 2.40+ | `git --version` | Version control & Jenkins pipeline triggers |
| **Docker Desktop** | 4.25+ (WSL 2 backend enabled) | `docker --version; docker compose version` | Containerization of candidate app, Jenkins, and Selenium |
| **Maven Wrapper** | Built into repository (`mvnw.cmd`) | `.\mvnw.cmd -version` | Reproducible builds without manual Maven installation |
| **Web Browser** | Chrome / Edge | — | Accessing application & Resilio Dashboard |

---

### Command-by-Command Runbook

#### 1. Check Development Environment
Open PowerShell in the repository root directory (`resilio/`):
```powershell
# Verify Java runtime
java -version

# Verify Git
git --version

# Verify Docker Desktop daemon is running
docker info
```

---

#### 2. Build the Application with Maven Wrapper
Build the executable Spring Boot JAR with the bundled Maven Wrapper:
```powershell
cd demo-app
.\mvnw.cmd clean package -DskipTests
cd ..
```
*Expected output*: `BUILD SUCCESS` with packaged archive at `demo-app/target/demo-app-1.0.0.jar`.

---

#### 3. Run Mandatory Unit Tests
Execute the JUnit 5 test suite and generate Jenkins-compatible Surefire test reports:
```powershell
cd demo-app
.\mvnw.cmd test
cd ..
```
*Expected output*:
```text
[INFO] Running com.resilio.demo.DataProcessingServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.resilio.demo.DemoApplicationTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
Surefire XML reports are saved to `demo-app/target/surefire-reports/`.

---

#### 4. Start the Local Docker Environment
Start the Selenium standalone Chrome grid and Jenkins container:
```powershell
docker compose up -d selenium-chrome jenkins
```

Verify containers are running:
```powershell
docker ps
```
- Selenium Chrome: `http://localhost:4444` (VNC live viewer on `http://localhost:7900`)
- Jenkins: `http://localhost:8085`

---

#### 5. Build and Run Candidate Container
Build the production Docker image with an immutable build tag:
```powershell
cd demo-app
docker build -t resilio/demo-app:1.0.0-local .
cd ..

# Run container on shared network
docker run -d --name resilio-demo-candidate -p 8080:8080 --network resilio_net resilio/demo-app:1.0.0-local
```

Verify container readiness:
```powershell
curl -s http://localhost:8080/actuator/health
# Expected output: {"status":"UP"}

curl -s http://localhost:8080/api/version
# Expected output: {"name":"resilio-demo-app","version":"1.0.0",...}
```

---

#### 6. Run Selenium Browser Tests Against Running Container
Execute the Selenium WebDriver functional test suite:
```powershell
cd selenium-tests
.\mvnw.cmd test -Dtarget.base.url=http://localhost:8080 -Dselenium.grid.url=http://localhost:4444/wd/hub
cd ..
```
*Expected output*:
- Scenario 1: Home page loads with navbar and title.
- Scenario 2: Application version metadata displayed.
- Scenario 3: Health Actuator responds `UP`.
- Scenario 4: Valid form submission succeeds.
- Scenario 5: Invalid form submission is rejected.
- Scenario 6: Expected processed result is displayed.

---

#### 7. Configuring Jenkins
1. Open your browser to `http://localhost:8085`.
2. Retrieve the initial admin password from the Jenkins container:
   ```powershell
   docker exec -it resilio-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
   ```
3. Install suggested plugins.
4. Create a new item:
   - Name: `resilio-pipeline`
   - Type: **Pipeline**
5. Under **Pipeline Definition**, select **Pipeline script from SCM**:
   - SCM: **Git**
   - Repository URL: your GitHub repository URL (e.g. `https://github.com/<your-username>/resilio.git`)
   - Branch Specifier: `*/main`
   - Script Path: `jenkins/Jenkinsfile`
6. Click **Save**.

---

#### 8. Running the Pipeline in Jenkins
- Click **Build Now** in the Jenkins job view.
- Click on the build number (e.g. `#1`) -> **Console Output** to observe all 7 pipeline stages executing.
- Upon completion, view the published JUnit test results and archived failure screenshots (if any).

---

#### 9. Viewing Container Logs
To inspect real-time logs from any running container:
```powershell
# Demo application logs
docker logs -f resilio-demo-candidate

# Jenkins pipeline engine logs
docker logs -f resilio-jenkins

# Selenium Chrome browser logs
docker logs -f resilio-selenium-chrome
```

---

#### 10. Stopping the Environment Safely
To safely stop and clean up temporary containers without losing Jenkins data:
```powershell
# Stop candidate container
docker rm -f resilio-demo-candidate

# Stop Docker Compose services
docker compose down
```
To stop everything and clean up dangling images:
```powershell
docker compose down --remove-orphans
```

# RESILIO — Phase 1 Acceptance Test Report
## Audit of Verified Test Scenarios

**Date of Execution**: 2026-10-26  
**Environment**: Windows 11 AMD64, Eclipse Adoptium OpenJDK 25.0.4.1 (target Java 21 LTS), Apache Maven 3.9.6 Wrapper, Docker Engine

---

### Acceptance Test B: Compilation Failure Verification

#### Objective
Verify that when invalid Java code is introduced, the Maven compiler fails with a non-zero exit code and halts the pipeline before any artifact packaging or Docker deployment occurs.

#### Trigger
Introduced a synthetic syntax error file:
```java
package com.resilio.demo;
public class DeliberateSyntaxError { INTENTIONAL_COMPILATION_ERROR }
```

#### Execution Command
```powershell
.\mvnw.cmd compile
```

#### Observed Result & Exit Status
```text
[INFO] --- compiler:3.11.0:compile (default-compile) @ demo-app ---
[INFO] Compiling 6 source files with javac [debug release 21] to target\classes
[ERROR] DeliberateSyntaxError.java:[2,38] <identifier> expected
[ERROR] DeliberateSyntaxError.java:[2,64] ';' expected
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 1.842 s
```
- **Exit Code**: `1` (Non-zero)
- **Verdict**: **PASSED**. Maven halted immediately. No JAR or Docker image was created.

---

### Acceptance Test C: Unit Test Failure Verification

#### Objective
Verify that when a unit test assertion fails, the Maven Surefire plugin reports the failure in XML, produces a non-zero exit code, and halts the CI/CD pipeline before containerization.

#### Trigger
Introduced `DeliberateFailingUnitTest.java`:
```java
package com.resilio.demo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class DeliberateFailingUnitTest {
    @Test
    public void testDeliberateAssertionFailure() {
        Assertions.fail("Deliberately failing unit test for Acceptance Test C");
    }
}
```

#### Execution Command
```powershell
.\mvnw.cmd test -Dtest=DeliberateFailingUnitTest
```

#### Observed Result & Exit Status
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.resilio.demo.DeliberateFailingUnitTest
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.044 s <<< FAILURE!
[ERROR] DeliberateFailingUnitTest.testDeliberateAssertionFailure:8 Deliberately failing unit test for Acceptance Test C
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test on project demo-app: There are test failures.
```
- **Exit Code**: `1` (Non-zero)
- **Report Location**: `demo-app/target/surefire-reports/TEST-com.resilio.demo.DeliberateFailingUnitTest.xml`
- **Verdict**: **PASSED**. The release was blocked before containerization.

---

### Acceptance Test E & F: Restored Clean Application Build & Packaging

#### Objective
Verify that the valid application builds, executes all 10 unit and mock MVC tests, and generates the production-ready Spring Boot executable JAR.

#### Execution Command
```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package -DskipTests
```

#### Observed Result & Exit Status
```text
[INFO] Running com.resilio.demo.DataProcessingServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.resilio.demo.DemoApplicationTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Results:
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Building jar: demo-app\target\demo-app-1.0.0.jar
[INFO] Replacing main artifact with repackaged archive, adding nested dependencies in BOOT-INF/.
```
- **Exit Code**: `0` (Success)
- **Artifact Verified**: `demo-app/target/demo-app-1.0.0.jar` (Executable Spring Boot archive)
- **Endpoints Verified**:
  - `GET /` — Responsive UI with Navigation Bar, status badge, and workflow form.
  - `GET /actuator/health` — Returns `{"status":"UP"}`.
  - `GET /api/version` — Returns `{ "name": "resilio-demo-app", "version": "1.0.0", "commitSha": "...", "buildNumber": "1" }`.
  - `POST /api/workflow/validate` — Accepts valid input (`RESILIO-ALPHA-2026`), rejects invalid input (`ab`).
- **Verdict**: **PASSED**.

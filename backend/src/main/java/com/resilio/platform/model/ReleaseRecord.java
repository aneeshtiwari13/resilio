package com.resilio.platform.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "releases")
public class ReleaseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String releaseId; // e.g. rel-1.0.0-a1b2c3d4

    private String versionTag;
    private String commitSha;
    private String imageTag;
    private String deploymentSlot; // BLUE or GREEN
    private Instant deployedAt;
    private Instant promotedAt;
    private Instant verifiedAt;

    private String jenkinsBuildRef; // e.g. #1
    private String healthCheckStatus; // PASSED, FAILED, PENDING
    private String seleniumSmokeStatus; // PASSED, FAILED, SKIPPED, PENDING
    private String activationStatus; // ACTIVE, STANDBY, REJECTED, ROLLED_BACK

    private boolean isKnownGood;
    private boolean isCurrentActive;

    public ReleaseRecord() {}

    public ReleaseRecord(String releaseId, String versionTag, String commitSha, String imageTag,
                         String deploymentSlot, String jenkinsBuildRef) {
        this.releaseId = releaseId;
        this.versionTag = versionTag;
        this.commitSha = commitSha;
        this.imageTag = imageTag;
        this.deploymentSlot = deploymentSlot;
        this.jenkinsBuildRef = jenkinsBuildRef;
        this.deployedAt = Instant.now();
        this.activationStatus = "STANDBY";
        this.isKnownGood = false;
        this.isCurrentActive = false;
        this.healthCheckStatus = "PENDING";
        this.seleniumSmokeStatus = "PENDING";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReleaseId() { return releaseId; }
    public void setReleaseId(String releaseId) { this.releaseId = releaseId; }
    public String getVersionTag() { return versionTag; }
    public void setVersionTag(String versionTag) { this.versionTag = versionTag; }
    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String commitSha) { this.commitSha = commitSha; }
    public String getImageTag() { return imageTag; }
    public void setImageTag(String imageTag) { this.imageTag = imageTag; }
    public String getDeploymentSlot() { return deploymentSlot; }
    public void setDeploymentSlot(String deploymentSlot) { this.deploymentSlot = deploymentSlot; }
    public Instant getDeployedAt() { return deployedAt; }
    public void setDeployedAt(Instant deployedAt) { this.deployedAt = deployedAt; }
    public Instant getPromotedAt() { return promotedAt; }
    public void setPromotedAt(Instant promotedAt) { this.promotedAt = promotedAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getJenkinsBuildRef() { return jenkinsBuildRef; }
    public void setJenkinsBuildRef(String jenkinsBuildRef) { this.jenkinsBuildRef = jenkinsBuildRef; }
    public String getHealthCheckStatus() { return healthCheckStatus; }
    public void setHealthCheckStatus(String healthCheckStatus) { this.healthCheckStatus = healthCheckStatus; }
    public String getSeleniumSmokeStatus() { return seleniumSmokeStatus; }
    public void setSeleniumSmokeStatus(String seleniumSmokeStatus) { this.seleniumSmokeStatus = seleniumSmokeStatus; }
    public String getActivationStatus() { return activationStatus; }
    public void setActivationStatus(String activationStatus) { this.activationStatus = activationStatus; }
    public boolean isKnownGood() { return isKnownGood; }
    public void setKnownGood(boolean knownGood) { isKnownGood = knownGood; }
    public boolean isCurrentActive() { return isCurrentActive; }
    public void setCurrentActive(boolean currentActive) { isCurrentActive = currentActive; }
}

package com.resilio.platform.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "deployments")
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String versionTag;
    private String commitSha;
    private String environmentSlot; // BLUE or GREEN
    private String status; // ACTIVE, STANDBY, ROLLED_BACK, FAILED
    private String imageTag;

    private Instant deployedAt;
    private Instant promotedAt;
    private Instant verifiedAt;

    private boolean isKnownGood;

    public Deployment() {}

    public Deployment(String versionTag, String commitSha, String environmentSlot, String imageTag) {
        this.versionTag = versionTag;
        this.commitSha = commitSha;
        this.environmentSlot = environmentSlot;
        this.imageTag = imageTag;
        this.status = "STANDBY";
        this.deployedAt = Instant.now();
        this.isKnownGood = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getVersionTag() { return versionTag; }
    public void setVersionTag(String versionTag) { this.versionTag = versionTag; }
    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String commitSha) { this.commitSha = commitSha; }
    public String getEnvironmentSlot() { return environmentSlot; }
    public void setEnvironmentSlot(String environmentSlot) { this.environmentSlot = environmentSlot; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getImageTag() { return imageTag; }
    public void setImageTag(String imageTag) { this.imageTag = imageTag; }
    public Instant getDeployedAt() { return deployedAt; }
    public void setDeployedAt(Instant deployedAt) { this.deployedAt = deployedAt; }
    public Instant getPromotedAt() { return promotedAt; }
    public void setPromotedAt(Instant promotedAt) { this.promotedAt = promotedAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public boolean isKnownGood() { return isKnownGood; }
    public void setKnownGood(boolean knownGood) { isKnownGood = knownGood; }
}

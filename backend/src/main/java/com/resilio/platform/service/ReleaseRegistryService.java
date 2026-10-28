package com.resilio.platform.service;

import com.resilio.platform.model.ReleaseRecord;
import com.resilio.platform.repository.ReleaseRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ReleaseRegistryService {

    private static final Logger log = LoggerFactory.getLogger(ReleaseRegistryService.class);

    private final ReleaseRecordRepository releaseRepo;
    private final RoutingService routingService;

    public ReleaseRegistryService(ReleaseRecordRepository releaseRepo, RoutingService routingService) {
        this.releaseRepo = releaseRepo;
        this.routingService = routingService;
    }

    public List<ReleaseRecord> getAllReleases() {
        return releaseRepo.findAllByOrderByDeployedAtDesc();
    }

    public Optional<ReleaseRecord> getActiveRelease() {
        return releaseRepo.findFirstByIsCurrentActiveTrue();
    }

    public Optional<ReleaseRecord> getLatestKnownGood() {
        return releaseRepo.findFirstByIsKnownGoodTrueAndIsCurrentActiveFalseOrderByVerifiedAtDesc();
    }

    public List<ReleaseRecord> getAllKnownGoodReleases() {
        return releaseRepo.findByIsKnownGoodTrueOrderByVerifiedAtDesc();
    }

    @Transactional
    public ReleaseRecord registerCandidate(String versionTag, String commitSha, String imageTag,
                                           String slot, String jenkinsBuildRef) {
        String releaseId = "rel-" + versionTag + "-" + (commitSha != null && commitSha.length() >= 7 ? commitSha.substring(0, 7) : "init");
        log.info("Registering candidate release: id={}, slot={}, image={}", releaseId, slot, imageTag);

        Optional<ReleaseRecord> existing = releaseRepo.findByReleaseId(releaseId);
        if (existing.isPresent()) {
            ReleaseRecord r = existing.get();
            r.setDeploymentSlot(slot);
            r.setJenkinsBuildRef(jenkinsBuildRef);
            r.setImageTag(imageTag);
            r.setDeployedAt(Instant.now());
            return releaseRepo.save(r);
        }

        ReleaseRecord release = new ReleaseRecord(releaseId, versionTag, commitSha, imageTag, slot, jenkinsBuildRef);
        return releaseRepo.save(release);
    }

    @Transactional
    public void activateRelease(String releaseId) {
        log.info("Activating release in registry: {}", releaseId);

        // De-activate current active release
        releaseRepo.findFirstByIsCurrentActiveTrue().ifPresent(cur -> {
            cur.setCurrentActive(false);
            cur.setActivationStatus("STANDBY");
            releaseRepo.save(cur);
        });

        // Activate target release
        releaseRepo.findByReleaseId(releaseId).ifPresent(target -> {
            target.setCurrentActive(true);
            target.setActivationStatus("ACTIVE");
            target.setPromotedAt(Instant.now());
            releaseRepo.save(target);
        });
    }

    @Transactional
    public void markKnownGood(String releaseId, boolean healthPassed, boolean smokePassed) {
        releaseRepo.findByReleaseId(releaseId).ifPresent(r -> {
            r.setHealthCheckStatus(healthPassed ? "PASSED" : "FAILED");
            r.setSeleniumSmokeStatus(smokePassed ? "PASSED" : "FAILED");
            if (healthPassed && smokePassed) {
                r.setKnownGood(true);
                r.setVerifiedAt(Instant.now());
                log.info("Release {} successfully verified and registered as KNOWN-GOOD.", releaseId);
            } else {
                r.setKnownGood(false);
                log.warn("Release {} failed verification. NOT designated as known-good.", releaseId);
            }
            releaseRepo.save(r);
        });
    }

    @Transactional
    public void markRolledBack(String failedReleaseId, String restoredReleaseId) {
        releaseRepo.findByReleaseId(failedReleaseId).ifPresent(failed -> {
            failed.setCurrentActive(false);
            failed.setActivationStatus("ROLLED_BACK");
            releaseRepo.save(failed);
        });

        releaseRepo.findByReleaseId(restoredReleaseId).ifPresent(restored -> {
            restored.setCurrentActive(true);
            restored.setActivationStatus("ACTIVE");
            releaseRepo.save(restored);
        });
    }

    /**
     * Reconciles database state with disk routing on startup.
     */
    @Transactional
    public void reconcileStateOnStartup() {
        routingService.reconcileFromDisk();
        String activeSlot = routingService.getActiveSlot();

        Optional<ReleaseRecord> activeInDb = releaseRepo.findFirstByIsCurrentActiveTrue();
        if (activeInDb.isEmpty()) {
            log.info("No active release in DB. Seeding initial baseline release for slot: {}", activeSlot);
            ReleaseRecord baseline = new ReleaseRecord(
                    "rel-1.0.0-baseline", "1.0.0", "baseline-sha-982a17", "resilio/demo-app:1.0.0",
                    activeSlot, "#1"
            );
            baseline.setCurrentActive(true);
            baseline.setActivationStatus("ACTIVE");
            baseline.setKnownGood(true);
            baseline.setHealthCheckStatus("PASSED");
            baseline.setSeleniumSmokeStatus("PASSED");
            baseline.setPromotedAt(Instant.now());
            baseline.setVerifiedAt(Instant.now());
            releaseRepo.save(baseline);
        } else {
            ReleaseRecord current = activeInDb.get();
            if (!current.getDeploymentSlot().equalsIgnoreCase(activeSlot)) {
                log.warn("RECONCILIATION_WARNING: Active slot in DB ({}) differs from Nginx routing ({})! Syncing to routing.",
                        current.getDeploymentSlot(), activeSlot);
                current.setDeploymentSlot(activeSlot);
                releaseRepo.save(current);
            }
        }
    }
}

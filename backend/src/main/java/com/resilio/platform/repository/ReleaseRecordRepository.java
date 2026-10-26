package com.resilio.platform.repository;

import com.resilio.platform.model.ReleaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReleaseRecordRepository extends JpaRepository<ReleaseRecord, Long> {
    List<ReleaseRecord> findAllByOrderByDeployedAtDesc();
    Optional<ReleaseRecord> findByReleaseId(String releaseId);
    Optional<ReleaseRecord> findFirstByIsCurrentActiveTrue();
    Optional<ReleaseRecord> findFirstByIsKnownGoodTrueAndIsCurrentActiveFalseOrderByVerifiedAtDesc();
    Optional<ReleaseRecord> findByDeploymentSlotAndIsCurrentActiveTrue(String slot);
    List<ReleaseRecord> findByIsKnownGoodTrueOrderByVerifiedAtDesc();
}

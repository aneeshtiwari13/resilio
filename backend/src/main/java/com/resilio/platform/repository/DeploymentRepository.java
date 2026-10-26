package com.resilio.platform.repository;

import com.resilio.platform.model.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    List<Deployment> findAllByOrderByDeployedAtDesc();
    Optional<Deployment> findFirstByStatusOrderByPromotedAtDesc(String status);
    Optional<Deployment> findFirstByIsKnownGoodTrueOrderByVerifiedAtDesc();
    Optional<Deployment> findByEnvironmentSlotAndStatus(String slot, String status);
}

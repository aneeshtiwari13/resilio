package com.resilio.platform.repository;

import com.resilio.platform.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findAllByOrderByActionStartedAtDesc();
    Optional<Incident> findByIncidentId(String incidentId);
    Optional<Incident> findTopByOrderByActionStartedAtDesc();
    long countByFinalStatus(String status);
    List<Incident> findByFinalStatus(String status);
}

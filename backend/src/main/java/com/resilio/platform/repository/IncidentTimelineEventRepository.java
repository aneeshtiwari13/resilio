package com.resilio.platform.repository;

import com.resilio.platform.model.IncidentTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentTimelineEventRepository extends JpaRepository<IncidentTimelineEvent, Long> {
    List<IncidentTimelineEvent> findByIncidentIdOrderByTimestampAsc(Long incidentId);
}

package com.resilio.platform.repository;

import com.resilio.platform.model.PipelineRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PipelineRunRepository extends JpaRepository<PipelineRun, Long> {
    List<PipelineRun> findAllByOrderByStartTimeDesc();
    Optional<PipelineRun> findTopByOrderByStartTimeDesc();
    Optional<PipelineRun> findByBuildNumber(String buildNumber);
}

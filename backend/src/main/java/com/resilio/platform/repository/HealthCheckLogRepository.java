package com.resilio.platform.repository;

import com.resilio.platform.model.HealthCheckLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HealthCheckLogRepository extends JpaRepository<HealthCheckLog, Long> {
    List<HealthCheckLog> findAllByOrderByTimestampDesc(Pageable pageable);
    Optional<HealthCheckLog> findTopByOrderByTimestampDesc();
}

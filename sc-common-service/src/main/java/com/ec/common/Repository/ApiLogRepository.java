package com.ec.common.Repository;

import com.ec.common.Model.ApiLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {
    void deleteByTimestampBefore(LocalDateTime cutoffDate);
}


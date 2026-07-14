package com.ec.application.repository;

import com.ec.application.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long>,
        JpaSpecificationExecutor<ActivityLog> {

    @Transactional
    @Modifying
    @Query("DELETE FROM ActivityLog a WHERE a.activityTime < :cutoff")
    int deleteByActivityTimeBefore(@Param("cutoff") Date cutoff);

    List<ActivityLog> findByIdGreaterThanOrderByIdAsc(Long id);

    List<ActivityLog> findByEntityTypeAndEntityIdOrderByActivityTimeDesc(String entityType, String entityId);
}

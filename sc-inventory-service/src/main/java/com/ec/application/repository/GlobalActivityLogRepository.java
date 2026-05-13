package com.ec.application.repository;

import com.ec.application.model.GlobalActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GlobalActivityLogRepository extends JpaRepository<GlobalActivityLog, Long>,
        JpaSpecificationExecutor<GlobalActivityLog> {

    @Query("SELECT MAX(g.tenantActivityLogId) FROM GlobalActivityLog g WHERE g.tenantSchema = :tenantSchema")
    Long findMaxTenantActivityLogIdBySchema(@Param("tenantSchema") String tenantSchema);
}

package com.ec.application.repository;

import com.ec.application.model.DeadStockSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeadStockSummaryRepo extends JpaRepository<DeadStockSummary, Long> {
    void deleteByTenantSchema(String tenantSchema);

    List<DeadStockSummary> findByTenantSchema(String tenantSchema);
}

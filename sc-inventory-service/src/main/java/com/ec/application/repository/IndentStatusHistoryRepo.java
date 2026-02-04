package com.ec.application.repository;

import com.ec.application.data.TenantCountDTO;
import com.ec.application.model.IndentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface IndentStatusHistoryRepo extends JpaRepository<IndentStatusHistory, Long> {
    List<IndentStatusHistory> findByIndent_IndentIdOrderByIdDesc(String indentId);

    // Tenant-wise count
    @Query(
            "SELECT new com.ec.application.data.TenantCountDTO(" +
                    "   ish.indent.tenant, " +
                    "   COUNT(DISTINCT ish.indent.indentId)" +
                    ") " +
                    "FROM IndentStatusHistory ish " +
                    "WHERE ish.newStatus = :status " +
                    "AND ish.indent.isDeleted = false " +
                    "AND ish.changedAt BETWEEN :startDate AND :endDate " +
                    "GROUP BY ish.indent.tenant"
    )
    List<TenantCountDTO> findIndentCountByTenant(@Param("status") String status, @Param("startDate") Date startDate, @Param("endDate") Date endDate);

    // Total count
    @Query(
            "SELECT COUNT(DISTINCT ish.indent.indentId) " +
                    "FROM IndentStatusHistory ish " +
                    "WHERE ish.newStatus = :status " +
                    "AND ish.indent.isDeleted = false " +
                    "AND ish.changedAt BETWEEN :startDate AND :endDate"
    )
    Long countIndents(@Param("status") String status, @Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
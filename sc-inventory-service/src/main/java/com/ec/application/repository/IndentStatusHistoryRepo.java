package com.ec.application.repository;

import com.ec.application.data.IndentStatusChangeDTO;
import com.ec.application.data.StatusGroupCountDTO;
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

    @Query(
            "SELECT new com.ec.application.data.StatusGroupCountDTO(" +
                    "   ish.newStatus, " +
                    "   ish.indent.tenant, " +
                    "   COUNT(DISTINCT ish.indent.indentId)" +
                    ") " +
                    "FROM IndentStatusHistory ish " +
                    "WHERE ish.newStatus IN :statuses " +
                    "AND ish.indent.isDeleted = false " +
                    "AND ish.changedAt BETWEEN :startDate AND :endDate " +
                    "GROUP BY ish.newStatus, ish.indent.tenant"
    )
    List<StatusGroupCountDTO> fetchIndentDashboardData(@Param("statuses") List<String> statuses, @Param("startDate") Date startDate, @Param("endDate") Date endDate);

    @Query(
            "SELECT ish.newStatus, ish.changedAt " +
                    "FROM IndentStatusHistory ish " +
                    "WHERE ish.changedAt >= :fromDate"
    )
    List<Object[]> fetchIndentStatusChangesSince(@Param("fromDate") Date fromDate
    );
}
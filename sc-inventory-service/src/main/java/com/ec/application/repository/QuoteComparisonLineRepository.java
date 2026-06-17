package com.ec.application.repository;

import com.ec.application.model.QuoteComparisonLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteComparisonLineRepository extends JpaRepository<QuoteComparisonLine, Long> {

    List<QuoteComparisonLine> findByQuoteComparison_QcId(String qcId);

    @Query("select l from QuoteComparisonLine l where l.quoteComparison.qcId = :qcId and l.lineStatus = :status")
    List<QuoteComparisonLine> findByQcIdAndStatus(@Param("qcId") String qcId, @Param("status") String status);
}

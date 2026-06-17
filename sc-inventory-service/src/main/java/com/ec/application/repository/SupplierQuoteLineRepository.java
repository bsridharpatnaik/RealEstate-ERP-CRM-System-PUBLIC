package com.ec.application.repository;

import com.ec.application.model.SupplierQuoteLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierQuoteLineRepository extends JpaRepository<SupplierQuoteLine, Long> {

    List<SupplierQuoteLine> findBySupplierQuote_Id(Long supplierQuoteId);

    @Query("select l from SupplierQuoteLine l where l.qcLineId = :qcLineId")
    List<SupplierQuoteLine> findByQcLineId(@Param("qcLineId") Long qcLineId);

    // For PO linkage dropdown: finalized quote lines for a given supplier and indent
    @Query("select sqln from SupplierQuoteLine sqln" +
           " join sqln.supplierQuote sq" +
           " join sq.quoteComparison qc" +
           " join QuoteComparisonLine qcl on qcl.id = sqln.qcLineId" +
           " where sq.supplierId = :supplierId" +
           " and qcl.indentId in :indentIds" +
           " and qcl.lineStatus in ('FINALIZED', 'PO_LINKED')" +
           " and qcl.finalizedSupplierQuoteLineId = sqln.id")
    List<SupplierQuoteLine> findFinalizedLinesForSupplierAndIndents(
            @Param("supplierId") Long supplierId,
            @Param("indentIds") List<String> indentIds);
}

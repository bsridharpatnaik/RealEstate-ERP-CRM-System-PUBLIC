package com.ec.application.repository;

import com.ec.application.model.SupplierQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierQuoteRepository extends JpaRepository<SupplierQuote, Long> {

    List<SupplierQuote> findByQuoteComparison_QcId(String qcId);
}

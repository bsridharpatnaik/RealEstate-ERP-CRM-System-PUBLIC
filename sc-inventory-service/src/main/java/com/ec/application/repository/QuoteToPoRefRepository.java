package com.ec.application.repository;

import com.ec.application.model.QuoteToPoRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteToPoRefRepository extends JpaRepository<QuoteToPoRef, Long> {

    List<QuoteToPoRef> findByPurchaseOrderId(String purchaseOrderId);

    Optional<QuoteToPoRef> findByPoLineId(Long poLineId);

    boolean existsByQcLineId(Long qcLineId);
}

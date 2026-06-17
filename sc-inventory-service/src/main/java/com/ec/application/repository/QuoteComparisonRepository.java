package com.ec.application.repository;

import com.ec.application.model.QuoteComparison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface QuoteComparisonRepository extends JpaRepository<QuoteComparison, String>,
        JpaSpecificationExecutor<QuoteComparison> {
}

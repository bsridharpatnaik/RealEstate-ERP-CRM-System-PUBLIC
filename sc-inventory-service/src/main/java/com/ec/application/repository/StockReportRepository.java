package com.ec.application.repository;

import com.ec.application.model.StockReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockReportRepository extends JpaRepository<StockReport, Long> {
    List<StockReport> findAllByOrderBySrNoAsc();
}
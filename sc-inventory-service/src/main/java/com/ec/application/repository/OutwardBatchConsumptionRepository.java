package com.ec.application.repository;

import com.ec.application.model.OutwardBatchConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutwardBatchConsumptionRepository extends JpaRepository<OutwardBatchConsumption, Long> {

    List<OutwardBatchConsumption> findByOutwardIdOrderByIdAsc(Long outwardId);

    List<OutwardBatchConsumption> findByOutwardIdAndProductIdOrderByIdAsc(Long outwardId, Long productId);

    void deleteByOutwardId(Long outwardId);
}

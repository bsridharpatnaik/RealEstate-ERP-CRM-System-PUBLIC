package com.ec.application.repository;

import com.ec.application.model.OutwardBatchConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface OutwardBatchConsumptionRepository extends JpaRepository<OutwardBatchConsumption, Long> {

    List<OutwardBatchConsumption> findByOutwardIdOrderByIdAsc(Long outwardId);

    List<OutwardBatchConsumption> findByOutwardIdAndProductIdOrderByIdAsc(Long outwardId, Long productId);

    // Used when reducing a specific batch's consumption record after a partial return
    List<OutwardBatchConsumption> findByOutwardIdAndBatch_BatchId(Long outwardId, Long batchId);

    @Modifying
    @Transactional
    @Query("DELETE FROM OutwardBatchConsumption c WHERE c.outwardId = :outwardId")
    void deleteByOutwardId(@Param("outwardId") Long outwardId);
}

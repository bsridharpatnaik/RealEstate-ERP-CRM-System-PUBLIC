package com.ec.application.repository;

import com.ec.application.model.BatchWriteOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchWriteOffRepository extends JpaRepository<BatchWriteOff, Long> {

    List<BatchWriteOff> findByBatch_BatchIdOrderByWriteOffDateDesc(Long batchId);
}

package com.ec.application.repository;

import com.ec.application.model.MrnSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;

public interface MrnSequenceRepository extends JpaRepository<MrnSequence, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MrnSequence m WHERE m.id = 1")
    MrnSequence getForUpdate();
}

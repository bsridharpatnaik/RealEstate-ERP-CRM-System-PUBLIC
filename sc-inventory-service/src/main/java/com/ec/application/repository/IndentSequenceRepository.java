package com.ec.application.repository;

import com.ec.application.model.IndentSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface IndentSequenceRepository extends JpaRepository<IndentSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from IndentSequence s where s.tenantCode = :code")
    Optional<IndentSequence> findForUpdate(@Param("code") String code);
}
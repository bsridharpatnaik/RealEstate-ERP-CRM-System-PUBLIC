package com.ec.application.repository;

import com.ec.application.model.QuoteComparisonSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface QuoteComparisonSequenceRepository extends JpaRepository<QuoteComparisonSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from QuoteComparisonSequence s where s.prefix = :prefix")
    Optional<QuoteComparisonSequence> findForUpdate(@Param("prefix") String prefix);
}

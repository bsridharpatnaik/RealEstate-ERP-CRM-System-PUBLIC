package com.ec.application.repository;

import com.ec.application.model.IndentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndentStatusHistoryRepo
        extends JpaRepository<IndentStatusHistory, Long> {
}
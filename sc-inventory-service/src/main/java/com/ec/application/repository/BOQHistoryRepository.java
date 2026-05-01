package com.ec.application.repository;

import com.ec.application.model.BOQHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BOQHistoryRepository extends JpaRepository<BOQHistory, Long>, JpaSpecificationExecutor<BOQHistory> {
}

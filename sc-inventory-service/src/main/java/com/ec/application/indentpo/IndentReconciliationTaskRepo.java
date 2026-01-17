package com.ec.application.indentpo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndentReconciliationTaskRepo extends JpaRepository<IndentReconciliationTask, Long> {
    List<IndentReconciliationTask> findTop10ByStatusOrderByCreatedAtAsc(String status);
}

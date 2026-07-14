package com.ec.application.repository;

import com.ec.application.model.StockComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockCommentRepository extends JpaRepository<StockComment, Long> {

    List<StockComment> findByProductIdOrderByCreatedAtDesc(Long productId);
}

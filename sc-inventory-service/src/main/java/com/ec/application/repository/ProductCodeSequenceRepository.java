package com.ec.application.repository;

import com.ec.application.model.ProductCodeSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductCodeSequenceRepository
        extends JpaRepository<ProductCodeSequence, String> {
}
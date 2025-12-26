package com.ec.application.repository;

import com.ec.application.model.IndentSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndentSequenceRepository extends JpaRepository<IndentSequence, String> {
}
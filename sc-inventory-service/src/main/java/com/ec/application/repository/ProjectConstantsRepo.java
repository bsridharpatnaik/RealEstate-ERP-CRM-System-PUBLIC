package com.ec.application.repository;

import com.ec.application.model.ProjectConstantsTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectConstantsRepo extends JpaRepository<ProjectConstantsTable, Long> {
    Optional<ProjectConstantsTable> findByKey(String key);
}
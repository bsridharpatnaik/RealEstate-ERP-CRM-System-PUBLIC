package com.ec.application.repository;

import com.ec.application.model.ProjectConstants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectConstantsRepo extends JpaRepository<ProjectConstants, Long> {
    Optional<ProjectConstants> findByKey(String key);
}
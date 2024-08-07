package com.ec.common.Repository;

import com.ec.ReusableClasses.BaseRepository;
import com.ec.common.Model.ProjectConstants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectConstantsRepo extends JpaRepository<ProjectConstants, Long> {
    Optional<ProjectConstants> findByKey(String key);
}
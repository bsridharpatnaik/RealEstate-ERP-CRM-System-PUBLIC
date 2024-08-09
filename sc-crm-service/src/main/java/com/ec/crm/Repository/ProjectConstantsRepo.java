package com.ec.crm.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.ec.crm.Model.ProjectConstantsTable;
import java.util.Optional;

@Repository
public interface ProjectConstantsRepo extends JpaRepository<ProjectConstantsTable, Long> {
    Optional<ProjectConstantsTable> findByKey(String key);
}
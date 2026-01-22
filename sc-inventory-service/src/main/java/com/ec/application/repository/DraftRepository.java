package com.ec.application.repository;


import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.Draft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface DraftRepository extends BaseRepository<Draft, Long> {
    Optional<Draft> findFirstByDraftTypeAndUsernameAndTenant(String draftType, String username, String tenant);

    Optional<Draft> findFirstByDraftTypeAndUsernameAndTenantIsNull(String draftType, String username);

    @Query("SELECT d FROM Draft d " + "WHERE d.isDeleted = false " + "AND d.lastModifiedDate < :todayStart")
    List<Draft> findDraftsBeforeToday(@Param("todayStart") Date todayStart);
}
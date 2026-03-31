package com.ec.application.repository;


import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.Draft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface DraftRepository extends BaseRepository<Draft, Long> {
    Optional<Draft> findFirstByDraftTypeAndUsernameAndTenant(String draftType, String username, String tenant);
    Optional<Draft> findFirstByDraftTypeAndUsernameAndTenantIsNull(String draftType, String username);

    // Multi-draft: list all drafts for a user (PO = tenant-less)
    List<Draft> findAllByDraftTypeAndUsernameAndTenantIsNullOrderByCreationDateDesc(String draftType, String username);

    // Multi-draft: fetch specific draft by ID using JPQL (same path as list queries)
    @Query("SELECT d FROM Draft d WHERE d.draftId = :draftId AND d.isDeleted = false")
    Optional<Draft> findByDraftIdNotDeleted(@Param("draftId") Long draftId);

    // Multi-draft: soft delete by ID + username in one JPQL UPDATE
    // @Transactional here (not on service) so the transaction opens AFTER TenantAspect sets the schema
    @Modifying
    @Transactional
    @Query("UPDATE Draft d SET d.isDeleted = true WHERE d.draftId = :draftId AND d.username = :username AND d.isDeleted = false")
    int softDeleteByIdAndUsername(@Param("draftId") Long draftId, @Param("username") String username);

    // Uniqueness check: does this name already exist for this user+type?
    boolean existsByDraftTypeAndUsernameAndDraftNameAndTenantIsNull(String draftType, String username, String draftName);

    @Query("SELECT d FROM Draft d " + "WHERE d.isDeleted = false " + "AND d.lastModifiedDate < :todayStart")
    List<Draft> findDraftsBeforeToday(@Param("todayStart") Date todayStart);
}
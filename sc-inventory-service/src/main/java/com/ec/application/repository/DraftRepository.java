package com.ec.application.repository;


import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.Draft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DraftRepository extends BaseRepository<Draft, Long> {
    Page<Draft> findByDraftType(String draftType, Pageable pageable);
}
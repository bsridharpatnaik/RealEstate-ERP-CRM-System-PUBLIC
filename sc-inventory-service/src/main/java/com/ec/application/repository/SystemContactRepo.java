package com.ec.application.repository;

import com.ec.application.model.SystemContact;
import com.ec.application.ReusableClasses.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SystemContactRepo extends BaseRepository<SystemContact, Long> {

    @Query("SELECT s FROM SystemContact s WHERE UPPER(s.name) = UPPER(:name)")
    Optional<SystemContact> findByNameIgnoreCase(@Param("name") String name);
}
package com.ec.application.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.Supplier;

@Repository
public interface SupplierRepo extends BaseRepository<Supplier, Long> {
    @Query(value = "SELECT contactId as id, name as name FROM Supplier m WHERE m.isSystemContact = false ORDER BY name")
    List<IdNameProjections> findIdAndNames();

    @Query(value = "SELECT * FROM contacts WHERE contactId = :id", nativeQuery = true)
    Optional<Supplier> findByIdUnfiltered(@Param("id") Long id);
}

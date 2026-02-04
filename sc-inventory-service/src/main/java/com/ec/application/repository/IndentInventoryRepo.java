package com.ec.application.repository;


import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.data.ConsolidatedIndentLineDTO;
import com.ec.application.data.StatusGroupCountDTO;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.InwardInventory;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndentInventoryRepo extends BaseRepository<IndentInventory, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    InwardInventory save(InwardInventory entity);

    @Query("SELECT DISTINCT i FROM IndentInventory i " +
            "LEFT JOIN FETCH i.inventoryList " +
            "LEFT JOIN FETCH i.fileInformations " +
            "WHERE i.indentId = :id")
    Optional<IndentInventory> findByIdWithDetails(@Param("id") String id);

    List<IndentInventory> findByIndentStatusIn(List<String> statuses);

    @Query(
            "SELECT new com.ec.application.data.StatusGroupCountDTO(" +
                    "   ii.indentStatus, " +
                    "   ii.tenant, " +
                    "   COUNT(ii.indentId)" +
                    ") " +
                    "FROM IndentInventory ii " +
                    "WHERE ii.indentStatus IN :statuses " +
                    "AND ii.isDeleted = false " +
                    "GROUP BY ii.indentStatus, ii.tenant"
    )
    List<StatusGroupCountDTO> fetchCurrentIndentStatusCounts(
            @Param("statuses") List<String> statuses
    );
}

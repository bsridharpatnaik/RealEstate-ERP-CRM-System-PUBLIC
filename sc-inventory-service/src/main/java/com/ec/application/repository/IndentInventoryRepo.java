package com.ec.application.repository;


import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.data.ConsolidatedIndentLineDTO;
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

    @Query("select new com.ec.application.dto.ConsolidatedIndentLineDTO(" +
            "i.tenantSchemaCode," +
            "i.date," +
            "i.indentNo," +
            "l.lineItemCode," +
            "l.categoryName," +
            "l.productId," +
            "l.productName," +
            "l.measurementUnit," +
            "l.approvedQuantity," +
            "l.specification," +
            "l.remarks,l.status) from IndentInventory i join i.indentInventoryList l where l.status in ('APPROVED', 'PARTIALLY_ORDERED')")
    List<ConsolidatedIndentLineDTO> fetchConsolidatedIndentLines();
}

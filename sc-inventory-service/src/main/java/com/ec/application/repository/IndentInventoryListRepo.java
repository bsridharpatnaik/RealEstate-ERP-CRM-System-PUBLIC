package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.IndentInventoryList;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndentInventoryListRepo extends BaseRepository<IndentInventoryList, Long> {
    @Query("Select i from IndentInventoryList i where i.lineItemCode = :lineItemCode" )
    List<IndentInventoryList> findByLineItemCode(@Param("lineItemCode")String lineItemCode);

    @Query("Select i from IndentInventoryList i where i.lineItemCode IN :lineItemCodes" )
    List<IndentInventoryList> findByLineItemCodeIn(@Param("lineItemCodes")Collection<String> lineItemCodes);

    @Query("Select i from IndentInventoryList i where i.lineItemCode = :lineItemCode AND lineItemStatus IN :statuses" )
    List<IndentInventoryList> findByLineItemCodeAndStatuses(@Param("lineItemCode")String lineItemCode, @Param("statuses") Collection<String> statuses);

    /**
     * Returns [productId, totalQty, productName] per product for indents belonging to a given tenant.
     * Runs in master schema. tenantCode matches indent_inventory.tenant column.
     */
    @Query(value =
        "SELECT iie.productId, SUM(iie.quantity) AS total_qty, p.product_name " +
        "FROM indent_inventory_entries iie " +
        "JOIN indent_inventory ii ON ii.indent_id = iie.indent_id AND ii.is_deleted = 0 " +
        "JOIN product p ON p.productId = iie.productId AND p.is_deleted = 0 " +
        "WHERE iie.is_deleted = 0 AND ii.tenant = :tenantCode " +
        "AND ii.indent_status NOT IN ('CANCELLED','REJECTED','SHORT CLOSED','SHORT_CLOSED') " +
        "GROUP BY iie.productId, p.product_name",
        nativeQuery = true)
    List<Object[]> fetchTotalsByTenant(@Param("tenantCode") String tenantCode);
}

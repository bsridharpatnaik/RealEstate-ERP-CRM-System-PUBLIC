package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.StockInformationFromView;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface StockInformationRepo extends BaseRepository<StockInformationFromView, Long> {
    
    @Query(value="select " +
            "ai1.ProductId as productId, " +
            "        ai1.Product_name as product_name, " +
            "        p.reorderQuantity, " +
            "        p.measurementUnit, " +
            "        c.category_name, " +
            "        ROUND(SUM(ai1.closingstock),2) as totalQuantityInHand, " +
            "        CASE WHEN ROUND(SUM(ai1.closingstock),2)<=p.reorderQuantity THEN 'Low' ELSE 'High' END as stockStatus, " +
            "        JSON_ARRAYAGG(JSON_OBJECT( " +
            "'warehouseName',ai1.warehousename, " +
            "            'quantityInHand',ai1.closingstock, " +
            "            'measurementUnit',p.measurementUnit " +
            "            )) as detailedStock " +
            "FROM all_inventory ai1 " +
            "INNER JOIN " +
            "(SELECT " +
            "Productid, " +
            "        warehouseid, " +
            "        MIN(id) as id " +
            "FROM all_inventory ai " +
            "    WHERE ai.date<=:maxDate " +
            "    GROUP BY Productid,warehouseid " +
            "    ) AS ai2  ON ai1.id=ai2.id " +
            "INNER JOIN Product p on p.productId=ai1.ProductId " +
            "INNER JOIN Category c on p.categoryId=c.categoryId " +
            "GROUP BY ai1.ProductId,ai1.Product_name,p.reorderQuantity,p.measurementUnit,c.category_name", nativeQuery = true)
    List<StockInformationFromView> getHistoricalStock(@Param("maxDate") Date maxDate);
}

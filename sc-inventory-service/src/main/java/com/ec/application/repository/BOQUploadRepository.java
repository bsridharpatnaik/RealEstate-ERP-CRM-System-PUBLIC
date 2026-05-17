package com.ec.application.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.util.Assert;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.data.OutwardQuantityDtoForBoqStatus;
import com.ec.application.model.BOQUpload;
import com.ec.application.model.Contact;

public interface BOQUploadRepository extends BaseRepository<BOQUpload, Long> {

    BOQUpload findByBuildingType(int buildingType);

    List<BOQUpload> findByUsageLocation(long buildingUnit);

    List<BOQUpload> findByUsageLocationLocationId(long buildingUnit);

    List<BOQUpload> findByBuildingTypeTypeIdAndUsageLocationLocationId(long buildingTypeId, long buildingUnitId);

    BOQUpload findByUsageLocationLocationIdAndLocationUsageAreaIdAndProductProductId(long buildingUnit,
                                                                                     long usageAreaId, long productId);

    @Query(value = "Select * from BOQUpload b where b.buildingTypeId=?1 and b.usageLocationId=?2", nativeQuery = true)
    List<BOQUpload> findBOQQuantity(long buildingTypeId, long buildingUnitId);

    @Query(value = "Select Sum(quantity),buildingTypeId, usageLocationId from BOQUpload b where b.productId=?1 and b.buildingTypeId=?2 and b.usageLocationId=?3", nativeQuery = true)
    Double findQuantityByProductProductId(long productId, long buildingTypeId, long buildingUnitId);

    @Query(value = "Select DISTINCT(productId), buildingTypeId,usageLocationId from BOQUpload b where is_deleted=false", nativeQuery = true)
    List<Object> findBuildigTypeIdBuildingUnitIdProductId();

    @Query(value = "Select DISTINCT(productId) from BOQUpload b", nativeQuery = true)
    List<Integer> findByProductId();

    @Query(value = "Select DISTINCT(productId),buildingTypeId,usageLocationId from BOQUpload b where b.buildingTypeId=?1", nativeQuery = true)
    List<Object> findByBuildingTypeTypeId(long buildingTypeId);

    @Query(value = "Select DISTINCT(productId) from BOQUpload b where b.buildingTypeId=?1 and b.usageLocationId=?2", nativeQuery = true)
    List<Integer> findProductIdByBuildingTypeTypeIdAndUsageLocationLocationIds(long buildingTypeId, long buildingUnitId);

    //**********
    @Query(value = "select bu.buildingTypeId,bu.usageLocationId,bu.productId,sum(quantity),ul.location_name from BOQUpload bu, Usage_Location ul "
            + " where  "
            + " bu.usageLocationId=ul.locationId and "
            + " bu.buildingTypeId=?1 and bu.usageLocationId in(?2) and bu.is_deleted=false "
            + "	GROUP BY bu.buildingTypeId, bu.usageLocationId,bu.productId,ul.location_name", nativeQuery = true)
    List<Object> findByBuildingTypeTypeIdAndUsageLocationLocationId(long buildingTypeId, List<Long> buildingUnitIds);

    @Query(value = "select sum(ioe.quantity) as outwardQuantity,bu.buildingTypeId, bu.usageLocationId,bu.productId "
            + "	 from BOQUpload bu, outward_inventory oi, outwardinventory_entry oe, inward_outward_entries ioe, Usage_Location ul "
            + "	 where "
            + "	 bu.locationId=oi.usageAreaId and  "
            + "	 bu.usageLocationId = oi.locationId and "
            + "	 bu.productId=ioe.productId and  "
            + "	 oe.outwardid = oi.outwardid and "
            + "	 ioe.entryId = oe.entryId and "
            + "	 ul.locationId = oi.locationId and "
            + "	 bu.buildingTypeId in(?1) and bu.usageLocationId in(?2) and "
            + "	 oi.is_deleted=false and bu.is_deleted=false "
            + "	 GROUP BY bu.buildingTypeId, bu.usageLocationId,bu.productId "
            + "", nativeQuery = true)
    List<Object> findOutwardQuantityByBuildingTypeIdAndUsageLocationId(Set<Long> buildingTypeIDsForQuery, Set<Long> buildingUnitIDsForQuery);

    @Query(value = "select bu.id,ua.usageAreaId,0.0 as ioequantity,bu.quantity as boqQuantity,bu.usageLocationId,ua.usagearea_name,bu.buildingTypeId ,bu.productId "
            + " from BOQUpload bu,usage_area ua where bu.locationId=ua.usageAreaId and bu.buildingTypeId in(?1) and bu.usageLocationId in(?2) and bu.is_deleted=false and bu.id NOT IN ( "
            + "select bus.id from  "
            + "(select bu.id,oi.usageAreaId,sum(ioe.quantity),bu.quantity as boqQuantity,bu.usageLocationId,ua.usagearea_name,bu.buildingTypeId,bu.productId,bu.locationId "
            + "	from BOQUpload bu, outward_inventory oi, outwardinventory_entry oe, inward_outward_entries ioe, Usage_Location ul,usage_area ua "
            + "	where "
            + " bu.locationId=oi.usageAreaId and  "
            + "	bu.usageLocationId = oi.locationId and "
            + " oe.outwardid = oi.outwardid and "
            + " ul.locationId = oi.locationId and "
            + " ioe.entryId = oe.entryId and "
            + "	bu.productId=ioe.productId and  "
            + "	bu.locationId=ua.usageAreaId and "
            + "	bu.buildingTypeId in(?1) and bu.usageLocationId in(?2) and "
            + "	oi.is_deleted=false and bu.is_deleted=false  "
            + "	GROUP BY bu.buildingTypeId, bu.usageLocationId,bu.productId,bu.locationId,bu.id,oi.usageAreaId,ioe.productId,bu.quantity,ua.usagearea_name) bus "
            + "	) "
            + "	UNION "
            + "select bu.id,oi.usageAreaId,sum(ioe.quantity) as ioequantity ,bu.quantity as boqQuantity,bu.usageLocationId,ua.usagearea_name, bu.buildingTypeId,bu.productId "
            + "	from BOQUpload bu, outward_inventory oi, outwardinventory_entry oe, inward_outward_entries ioe, Usage_Location ul,usage_area ua "
            + "	where "
            + " bu.locationId=oi.usageAreaId and  "
            + "	bu.usageLocationId = oi.locationId and "
            + " oe.outwardid = oi.outwardid and "
            + " ul.locationId = oi.locationId and "
            + " ioe.entryId = oe.entryId and "
            + "	bu.productId=ioe.productId and  "
            + "	bu.locationId=ua.usageAreaId and "
            + "	bu.buildingTypeId in(?1) and bu.usageLocationId in(?2) and "
            + "	oi.is_deleted=false and bu.is_deleted=false "
            + "	GROUP BY bu.buildingTypeId, bu.usageLocationId,bu.productId ,ioe.productId,oi.usageAreaId,bu.quantity,ua.usagearea_name,bu.id ", nativeQuery = true)
    List<Object> findUsageAreaBoqQuantityOutwardQuantityByBuildingTypeIdAndUsageLocationId(Set<Long> buildingTypeIDsForQuery,
                                                                                           Set<Long> buildingUnitIDsForQuery);

    @Query("SELECT b FROM BOQUpload b WHERE b.product.productId=:productId AND b.location.usageAreaId=:finalLocationId AND b.usageLocation.locationId=:locationId")
    List<BOQUpload> getboqQuantityForOutward(@Param("productId") Long productId, @Param("locationId") Long locationId, @Param("finalLocationId") Long finalLocationId);

    /**
     * Returns one flat row per (buildingType, buildingUnit, finalLocation, product).
     * Columns: id, buildingTypeId, building_type, location_id, location_name,
     *          final_location_id, final_location_name, productId, product_name,
     *          category_name, boq_quantity, outward_quantity
     *
     * Uses a correlated subquery for outward aggregation so MySQL only scans
     * outward_inventory rows that match each BOQ row's (locationId, usageAreaId, productId)
     * instead of materializing the entire outward join upfront.
     */
    @Query(value =
        "SELECT bu.id, bu.buildingTypeId, btype.building_type, " +
        "  bu.usageLocationId AS location_id, ul.location_name, " +
        "  bu.locationId AS final_location_id, ua.usagearea_name AS final_location_name, " +
        "  bu.productId, p.product_name, c.category_name, " +
        "  bu.quantity AS boq_quantity, " +
        "  COALESCE((" +
        "    SELECT SUM(ioe.quantity) " +
        "    FROM outward_inventory oi " +
        "    INNER JOIN outwardinventory_entry oie ON oie.outwardid = oi.outwardid " +
        "    INNER JOIN inward_outward_entries ioe ON ioe.entryId = oie.entryId " +
        "    WHERE oi.is_deleted = 0 " +
        "      AND oi.locationId = bu.usageLocationId " +
        "      AND oi.usageAreaId = bu.locationId " +
        "      AND ioe.productId = bu.productId " +
        "  ), 0) AS outward_quantity, " +
        "  bu.wastagePercent AS wastage_percent " +
        "FROM BOQUpload bu " +
        "INNER JOIN building_type btype ON bu.buildingTypeId = btype.typeId " +
        "INNER JOIN Usage_Location ul ON bu.usageLocationId = ul.locationId " +
        "INNER JOIN usage_area ua ON ua.usageAreaId = bu.locationId " +
        "INNER JOIN Product p ON p.productId = bu.productId " +
        "INNER JOIN Category c ON c.categoryId = p.categoryId " +
        "WHERE bu.is_deleted = 0 AND ul.is_deleted = 0 " +
        "ORDER BY btype.building_type, ul.location_name, c.category_name, p.product_name",
        nativeQuery = true)
    List<Object[]> fetchBOQStatusRows();

    /**
     * Returns [effective_boq_qty, total_outward_quantity] aggregated across ALL final locations
     * for a given (buildingUnit/usageLocation, product). Used for BOQ enforcement check.
     * effective_boq_qty = SUM(quantity * (1 + wastagePercent/100)) — wastage already baked in.
     */
    @Query(value =
        "SELECT COALESCE(SUM(bu.quantity * (1 + COALESCE(bu.wastagePercent, 0) / 100)), 0) AS effective_boq_qty, " +
        "  COALESCE(SUM(ioe_sum.outward_qty), 0) AS total_outward_qty " +
        "FROM BOQUpload bu " +
        "LEFT JOIN ( " +
        "  SELECT bu2.id, COALESCE(SUM(ioe.quantity), 0) AS outward_qty " +
        "  FROM BOQUpload bu2 " +
        "  LEFT JOIN outward_inventory oi ON oi.locationId = bu2.usageLocationId " +
        "    AND oi.usageAreaId = bu2.locationId AND oi.is_deleted = 0 " +
        "  LEFT JOIN outwardinventory_entry oie ON oie.outwardid = oi.outwardid " +
        "  LEFT JOIN inward_outward_entries ioe ON ioe.entryId = oie.entryId " +
        "    AND ioe.productId = bu2.productId " +
        "  WHERE bu2.is_deleted = 0 AND bu2.usageLocationId = ?1 AND bu2.productId = ?2 " +
        "  GROUP BY bu2.id " +
        ") ioe_sum ON ioe_sum.id = bu.id " +
        "WHERE bu.is_deleted = 0 AND bu.usageLocationId = ?1 AND bu.productId = ?2",
        nativeQuery = true)
    List<Object[]> fetchAggregatedBOQAndOutward(Long usageLocationId, Long productId);

    @Query("SELECT b FROM BOQUpload b WHERE b.id = :id")
    java.util.Optional<BOQUpload> findByIntId(@Param("id") int id);

    /**
     * Returns [boq_quantity, outward_quantity] for one specific (locationId, productId, finalLocationId).
     */
    @Query(value =
        "SELECT bu.quantity AS boq_quantity, COALESCE(SUM(ioe.quantity), 0) AS outward_quantity, bu.wastagePercent AS wastage_percent " +
        "FROM BOQUpload bu " +
        "LEFT JOIN outward_inventory oi ON oi.locationId = bu.usageLocationId " +
        "  AND oi.usageAreaId = bu.locationId AND oi.is_deleted = 0 " +
        "LEFT JOIN outwardinventory_entry oie ON oie.outwardid = oi.outwardid " +
        "LEFT JOIN inward_outward_entries ioe ON ioe.entryId = oie.entryId " +
        "  AND ioe.productId = bu.productId " +
        "WHERE bu.is_deleted = 0 AND bu.usageLocationId = ?1 AND bu.productId = ?2 AND bu.locationId = ?3 " +
        "GROUP BY bu.quantity, bu.wastagePercent",
        nativeQuery = true)
    List<Object[]> fetchBOQAndOutwardForProduct(Long locationId, Long productId, Long finalLocationId);

    @Query("SELECT COUNT(b) FROM BOQUpload b WHERE b.location.usageAreaId = :id AND b.isDeleted = false")
    int usageAreaBoqCount(@Param("id") Long id);

    @Query("SELECT COUNT(b) FROM BOQUpload b WHERE b.usageLocation.locationId = :id AND b.isDeleted = false")
    int locationBoqCount(@Param("id") Long id);
}

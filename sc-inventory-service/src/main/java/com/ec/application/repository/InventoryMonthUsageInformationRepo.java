package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.data.InventoryLocationUsageDTO;
import com.ec.application.model.InventoryMonthUsageInformation;
import com.ec.application.model.InventoryNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryMonthUsageInformationRepo extends BaseRepository<InventoryMonthUsageInformation, Long> {

    @Query("SELECT DISTINCT ym  FROM InventoryMonthUsageInformation i WHERE i.locationId=:locationId ORDER BY i.ym DESC")
    List<String> findDatesByLocation(@Param("locationId") Long locationId);

    @Query("SELECT new com.ec.application.data.InventoryLocationUsageDTO(locationId, locationName, categoryId, categoryName, productId, productName, " +
            "price, SUM(totalQuantity), price*SUM(totalQuantity))  FROM " +
            "InventoryMonthUsageInformation i WHERE i.locationId=:locationId AND i.ym IN (:monthList) " +
            "GROUP BY locationId, locationName, categoryId, categoryName, productId, productName,price")
    List<InventoryLocationUsageDTO> findInventoryUsagePricing(@Param("locationId")Long locationId,@Param("monthList")List<String> monthList);
}

package com.ec.application.repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.InventoryMonthPriceMapping;
import com.ec.application.model.Warehouse;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.LockModeType;
import java.util.Date;
import java.util.List;

@Repository
public interface InventoryMonthPriceMappingRepository extends BaseRepository<InventoryMonthPriceMapping, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    InventoryMonthPriceMapping save(InventoryMonthPriceMapping entity);

    @Query("SELECT I FROM InventoryMonthPriceMapping I WHERE I.product.productId = :productId AND I.date = :date")
    List<InventoryMonthPriceMapping> findByInventoryMonth(@Param("productId") Long productId, @Param("date") Date date);

    @Query(value="SELECT count(*) from InventoryMonthPriceMapping i where i.product.productId=:productId")
    int productUsageCount(@Param("productId") Long productId);
}

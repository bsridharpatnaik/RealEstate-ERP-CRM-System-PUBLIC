package com.ec.application.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.InventoryNotification;
import com.ec.application.model.InwardInventory;

@Repository
public interface InventoryNotificationRepo extends BaseRepository<InventoryNotification, Long>
{

	@Query(value="SELECT m from InventoryNotification m where m.product.productId=:productId and m.type=:type")
	List<InventoryNotification> findByProductAndType(@Param("productId")Long productId,@Param("type") String type);

}

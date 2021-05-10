package com.ec.application.repository;

import java.util.List;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.UsageArea;
import com.ec.application.model.Warehouse;

public interface WarehouseRepo extends BaseRepository<Warehouse, Long>
{

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Warehouse save(Warehouse entity);
	
	@Query(value="SELECT m from Warehouse m where warehouseName=:name")
	List<Warehouse> findByName(@Param("name") String name);

	@Query(value="SELECT count(m) from Warehouse m where warehouseName=:warehouseName")
	int countByName(@Param("warehouseName")String warehouseName);

	@Query(value="SELECT warehouseId as id,warehouseName as name from Warehouse m  order by name")
	List<IdNameProjections> findIdAndNames();
}

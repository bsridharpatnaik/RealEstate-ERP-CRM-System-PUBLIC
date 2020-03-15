package com.ec.application.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.Stock.Warehouse;

public interface WarehouseRepo extends BaseRepository<Warehouse, String>
{

	@Query(value="SELECT m from Warehouse m where warehouseName=:name")
	List<Warehouse> findByName(@Param("name") String name);

}

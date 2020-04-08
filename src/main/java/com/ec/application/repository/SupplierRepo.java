package com.ec.application.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.application.Projections.IdNameProjections;
import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.Supplier;

@Repository
public interface SupplierRepo extends BaseRepository<Supplier, Long>
{
	@Query(value="SELECT contact_id as id,name as name from Supplier m")
	List<IdNameProjections> getSupplierNames();

}

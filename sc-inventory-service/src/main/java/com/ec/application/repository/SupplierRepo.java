package com.ec.application.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.model.Supplier;

@Repository
public interface SupplierRepo extends BaseRepository<Supplier, Long>
{
	@Query(value="SELECT contactId as id,name as name from Supplier m order by name")
	List<IdNameProjections> findIdAndNames();
}

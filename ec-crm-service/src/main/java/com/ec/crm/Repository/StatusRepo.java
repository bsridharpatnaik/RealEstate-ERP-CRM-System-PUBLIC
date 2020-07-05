package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.ec.crm.Model.Status;
import com.ec.crm.ReusableClasses.BaseRepository;
import com.ec.crm.ReusableClasses.IdNameProjections;

public interface StatusRepo extends BaseRepository<Status, Long>, JpaSpecificationExecutor<Status>{
	boolean existsByName(String name);
	
	@Query(value="SELECT statusId as id,name as name from Status S")
	List<IdNameProjections> findIdAndNames();
}

package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.ec.crm.Model.PropertyType;
import com.ec.crm.ReusableClasses.BaseRepository;
import com.ec.crm.ReusableClasses.IdNameProjections;

public interface PropertyTypeRepo extends BaseRepository<PropertyType, Long>, JpaSpecificationExecutor<PropertyType>{
	boolean existsByName(String name);
	
	@Query(value="SELECT propertyTypeId as id,name as name from PropertyType p")
	List<IdNameProjections> findIdAndNames();
}

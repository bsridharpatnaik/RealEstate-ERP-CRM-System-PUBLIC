package com.ec.crm.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ec.ReusableClasses.BaseRepository;
import com.ec.crm.Model.PropertyType;

public interface PropertyTypeRepo extends BaseRepository<PropertyType, Long>, JpaSpecificationExecutor<PropertyType>{
	boolean existsByName(String name);
}

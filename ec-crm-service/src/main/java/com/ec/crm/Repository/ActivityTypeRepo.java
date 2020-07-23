package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.ec.crm.Model.ActivityType;
import com.ec.crm.Model.Source;
import com.ec.crm.ReusableClasses.BaseRepository;
import com.ec.crm.ReusableClasses.IdNameProjections;

public interface ActivityTypeRepo  extends BaseRepository<ActivityType, Long>, JpaSpecificationExecutor<ActivityType>{
	boolean existsByName(String name);
	
	@Query(value="SELECT activityTypeId as id,name as name from ActivityType a")
	List<IdNameProjections> findIdAndNames();
}

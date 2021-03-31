package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.PropertyName;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface PropertyNameRepo extends BaseRepository<PropertyName, Long>
{
	@Query(value = "SELECT DISTINCT name from PropertyName b")
	List<String> getUniqueNames();
	
	@Query(value = "SELECT count(name) from PropertyName b where b.name=:name")
	int getCountByName(@Param("name") String name);
	
}

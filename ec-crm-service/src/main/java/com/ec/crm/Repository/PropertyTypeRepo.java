package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.PropertyType;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface PropertyTypeRepo extends BaseRepository<PropertyType, Long>
{

	boolean existsByPropertyType(String name);

	@Query("Select pt from PropertyType pt join pt.propertyNames pn where pn.propertyNameId=:id")
	List<PropertyType> getPTbyPNID(@Param("id") Long id);

}

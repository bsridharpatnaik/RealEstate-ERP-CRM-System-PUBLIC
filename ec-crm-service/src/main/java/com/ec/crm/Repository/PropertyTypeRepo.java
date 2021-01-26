package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.PropertyType;
import com.ec.crm.ReusableClasses.BaseRepository;
import com.ec.crm.ReusableClasses.IdNameProjections;

@Repository
public interface PropertyTypeRepo extends BaseRepository<PropertyType, Long>
{

	boolean existsByPropertyType(String name);

	@Query("Select pt from PropertyType pt join pt.propertyNames pn where pn.propertyNameId=:id")
	List<PropertyType> getPTbyPNID(@Param("id") Long id);

	@Query(value = "SELECT propertyTypeId as id,propertyType as name from PropertyType pt")
	List<IdNameProjections> findIdAndNames();

	@Query(value = "SELECT pn.propertyNameId as id,pn.name as name "
			+ "from PropertyType pt JOIN pt.propertyNames pn WHERE pt.propertyTypeId=:id and pn.isBooked=false")
	List<IdNameProjections> fetchAvailableProperties(@Param("id") Long id);

}

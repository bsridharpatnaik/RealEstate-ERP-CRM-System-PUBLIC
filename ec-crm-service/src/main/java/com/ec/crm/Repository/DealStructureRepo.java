
package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.DealStructure;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface DealStructureRepo extends BaseRepository<DealStructure, Long>
{

	@Query("select d from DealStructure d where d.lead.leadId=:leadId")
	List<DealStructure> getDealStructureByLeadID(@Param("leadId") Long leadId);

	@Query("select count(d) from DealStructure d where d.propertyName.propertyNameId=:propertyId")
	int countByPropertyName(@Param("propertyId") Long propertyId);
}

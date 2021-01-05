package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.CustomerDocument;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface CustomerDocumentRepo extends BaseRepository<CustomerDocument, Long>
{

	@Query("select cd from CustomerDocument cd where cd.lead.leadId=:leadId order by cd.documentName")
	List<CustomerDocument> findDocumentsForLead(@Param("leadId") Long leadId);

	@Query("select count(cd) from CustomerDocument cd where cd.lead.leadId=:leadId And cd.documentName=:document")
	int getCountByDocumentNameAndLead(@Param("document") String document, @Param("leadId") Long leadId);
}

package com.ec.application.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.ContactInfo;
import com.ec.application.model.InwardInventory;

public interface ContactInfoRepo extends BaseRepository<ContactInfo, Long>
{
	
	@Query(value="SELECT count(*) from ContactInfo m where contactId=:contactId")
	int conactUsageCount(@Param("contactId")Long contactId);
}

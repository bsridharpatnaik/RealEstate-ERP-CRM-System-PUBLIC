package com.ec.application.repository;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.Category;
import com.ec.application.model.ContactInfo;
import com.ec.application.model.InwardInventory;

public interface ContactInfoRepo extends BaseRepository<ContactInfo, Long>
{
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	ContactInfo save(ContactInfo entity);
	
	@Query(value="SELECT count(*) from ContactInfo m where contactId=:contactId")
	int conactUsageCount(@Param("contactId")Long contactId);
}

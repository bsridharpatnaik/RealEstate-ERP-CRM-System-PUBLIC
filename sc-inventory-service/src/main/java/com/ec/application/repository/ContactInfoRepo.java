package com.ec.application.repository;

import javax.persistence.LockModeType;
import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.application.ReusableClasses.BaseRepository;
import com.ec.application.model.Contact;

@Repository
@Transactional
public interface ContactInfoRepo extends BaseRepository<Contact, Long>
{

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Contact save(Contact entity);

	@Query(value = "SELECT count(*) from Contact m where mobileNo=:mobileNo")
	int getCountByMobileNo(String mobileNo);

}

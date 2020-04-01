package com.ec.application.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.application.SoftDelete.BaseRepository;
import com.ec.application.model.BasicEntities.Contact;

@Repository
public interface ContactRepo extends BaseRepository<Contact, Long>
{

	@Query(value="SELECT count(*) from Contact m where name=:name and mobileNo=:mobileNo")
	int getCountByNameNo(String name,String mobileNo);

}

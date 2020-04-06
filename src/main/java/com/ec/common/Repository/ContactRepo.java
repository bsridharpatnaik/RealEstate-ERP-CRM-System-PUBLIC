package com.ec.common.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.common.Model.Contact;

@Repository
public interface ContactRepo extends JpaRepository<Contact, Long>
{

	@Query(value="SELECT count(*) from Contact m where name=:name and mobileNo=:mobileNo")
	int getCountByNameNo(String name,String mobileNo);

}

package com.ec.common.Repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.common.Model.ContactAllInfo;

@Repository
//@Transactional
public interface ContactAllInfoRepo extends JpaRepository<ContactAllInfo, Long>
{

	@Query(value="SELECT count(*) from ContactAllInfo m where mobileNo=:mobileNo")
	int findCountByMobileNo(@Param("mobileNo")String mobileNo);
}

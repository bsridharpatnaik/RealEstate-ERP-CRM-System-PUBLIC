package com.ec.common.Repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.common.Model.ContactBasicInfo;

@Repository
@Transactional
public interface ContactBasicInfoRepo extends JpaRepository<ContactBasicInfo, Long>
{

	@Query(value="SELECT count(*) from ContactBasicInfo m where mobileNo=:mobileNo")
	int getCountByMobileNo(String mobileNo);

}

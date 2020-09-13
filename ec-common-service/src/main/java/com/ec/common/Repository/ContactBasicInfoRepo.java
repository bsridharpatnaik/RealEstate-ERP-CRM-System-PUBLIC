package com.ec.common.Repository;

import javax.persistence.LockModeType;
import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.ReusableClasses.BaseRepository;
import com.ec.common.Model.ContactBasicInfo;

@Repository
@Transactional
public interface ContactBasicInfoRepo extends BaseRepository<ContactBasicInfo, Long>
{

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	ContactBasicInfo save(ContactBasicInfo entity);
	
	@Query(value="SELECT count(*) from ContactBasicInfo m where mobileNo=:mobileNo")
	int getCountByMobileNo(String mobileNo);

}

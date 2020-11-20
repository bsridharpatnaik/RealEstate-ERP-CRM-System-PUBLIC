package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.crm.ReusableClasses.BaseRepository;
import com.ec.crm.Data.AndroidTokenDetails;


@Repository
public interface AndroidTokenDetailsRepo extends BaseRepository<AndroidTokenDetails, Long>
{

	@Query(value = "SELECT count(*) FROM AndroidTokenDetails a where a.user.userId=:userId")
	int findCountByUserId(@Param("userId") Long userId);

	@Query(value = "SELECT a FROM AndroidTokenDetails a where a.user.userId=:userId")
	List<AndroidTokenDetails> findByUserId(@Param("userId") Long userId);

}

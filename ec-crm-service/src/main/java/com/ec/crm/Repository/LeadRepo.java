package com.ec.crm.Repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.crm.Data.LeadLastUpdatedDAO;
import com.ec.crm.Model.Lead;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface LeadRepo extends BaseRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {
	@Query(value="SELECT count(*) from Lead m where primaryMobile=:mobileNo")
	int findCountByPMobileNo(@Param("mobileNo")String mobileNo);
	
	@Query(value="SELECT count(*) from Lead m where secondaryMobile=:mobileNo")
	int findCountBySMobileNo(@Param("mobileNo")String mobileNo);
	
	/*
	 * @Query(value="SELECT count(*) from Lead m where sentiment.sentimentId=:sid")
	 * int checksentimentusedinlead(@Param("sid")Long sid);
	 */
	
	@Query(value="SELECT count(*) from Lead m where broker.brokerId=:bid")
	int checkbrokerusedinlead(@Param("bid")Long bid);
	
	@Query(value="SELECT count(*) from Lead m where source.sourceId=:sid")
	int checksourceusedinlead(@Param("sid")Long sid);

	@Query(value="SELECT DISTINCT customerName from Lead m")
	List<String> getLeadNames();
	
	@Query(value="SELECT DISTINCT primaryMobile from Lead m")
	List<String> getLeadMobileNos();

	
	
	
	
}

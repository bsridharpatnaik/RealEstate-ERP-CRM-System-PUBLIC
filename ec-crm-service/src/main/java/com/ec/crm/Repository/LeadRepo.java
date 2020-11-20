package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.Lead;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface LeadRepo extends BaseRepository<Lead, Long>, JpaSpecificationExecutor<Lead>
{
	@Query(value = "SELECT count(*) from Lead m where primaryMobile=:mobileNo")
	int findCountByPMobileNo(@Param("mobileNo") String mobileNo);

	@Query(value = "SELECT count(*) from Lead m where secondaryMobile=:mobileNo")
	int findCountBySMobileNo(@Param("mobileNo") String mobileNo);

	@Query(value = "SELECT DISTINCT customerName from Lead m")
	List<String> getLeadNames();

	@Query(value = "SELECT DISTINCT primaryMobile from Lead m")
	List<String> getLeadMobileNos();

	@Query(value = "SELECT count(m) from Lead m where m.source.sourceId=:sourceId")
	int findSourceUsageCount(@Param("sourceId") Long sourceId);

	@Query(value = "SELECT count(m) from Lead m where m.broker.brokerId=:brokerId")
	int findBrokerUsageCount(@Param("brokerId") Long brokerId);
	
	//@Query(value = "SELECT DISTINCT assigneeId from Lead m")
	Long findUserIdByLeadId(@Param(value = "asigneeId") Long asigneeId);
}

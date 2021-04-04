package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.Lead;
import com.ec.crm.ReusableClasses.BaseRepository;

import javax.persistence.LockModeType;

@Repository
public interface LeadRepo extends BaseRepository<Lead, Long>, JpaSpecificationExecutor<Lead>
{

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Lead save(Lead lead);

 	@Query(value = "SELECT m from Lead m where primaryMobile=:mobileNo")
	List<Lead> findLeadsByPMobileNo(@Param("mobileNo") String mobileNo);

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

	@Query(value = "SELECT asigneeId from Lead m where leadId = :leadId")
	Long findUserIdByLeadId(@Param(value = "leadId") Long leadId);

	@Query(value = "SELECT DISTINCT primaryMobile from Lead m where m.asigneeId =:leadId ")
	List<String> getAssignedLeadMobileNos(@Param("leadId") Long leadId);
}

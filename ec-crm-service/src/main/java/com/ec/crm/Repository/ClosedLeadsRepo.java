package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.ClosedLeads;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface ClosedLeadsRepo extends BaseRepository<ClosedLeads, Long>
{

	@Query(value = "SELECT DISTINCT customerName from ClosedLeads m")
	List<String> getLeadNames();

	@Query(value = "SELECT DISTINCT primaryMobile from ClosedLeads m")
	List<String> getLeadMobileNos();

}

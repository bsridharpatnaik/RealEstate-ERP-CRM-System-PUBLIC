package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.LeadStatus;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface LeadStatusRepo extends BaseRepository<LeadStatus, Long>, JpaSpecificationExecutor<LeadStatus> {
	@Query(value="SELECT count(*) from LeadStatus l where leadId=:id and statusId=:sid")
	int checkexist(@Param("id")Long id,@Param("sid") Long sid);
	
	@Query(value="SELECT l from LeadStatus l where leadId=:id order by leadStatusId DESC")
	List<LeadStatus> checklatest(@Param("id")Long id);
}

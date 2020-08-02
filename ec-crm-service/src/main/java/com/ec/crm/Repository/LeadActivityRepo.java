package com.ec.crm.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface LeadActivityRepo extends BaseRepository<LeadActivity, Long>, JpaSpecificationExecutor<LeadActivity> 
{
	@Query(value="SELECT l from LeadActivity l where l.lead.leadId=:leadId and l.activityType=:activityType and l.isOpen=true")
	List<LeadActivity> findByLeadActivityTypeOpen(@Param("leadId")Long leadId, @Param("activityType")ActivityTypeEnum activityType);

	@Query(value="SELECT l from LeadActivity l where l.lead.leadId=:leadId and l.isOpen=true order by l.activityDateTime")
	List<LeadActivity> fetchPendingActivitiesForLead(Long leadId);

	@Query(value="SELECT l from LeadActivity l where l.lead.leadId=:leadId and l.isOpen=false order by l.activityDateTime desc")
	List<LeadActivity> fetchPastActivitiesForLead(Long leadId);
}

package com.ec.crm.Repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import com.ec.crm.Data.LeadPageData;
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

	@Query(value="SELECT l from LeadActivity l where l.lead.leadId=:leadId and l.isOpen=true")
	List<LeadActivity> findAllByOpenActivitiesByLeadId(Long leadId);
	
	@Query(value="SELECT la from LeadActivity la where la.lead.leadId=:leadId ORDER BY la.activityDateTime DESC")
	List<LeadActivity> findAllActivitiesForLead(@Param("leadId")Long leadId);

	
	@Query(value="SELECT la from LeadActivity la where la.isOpen = true and la.lead.leadId=:leadId "
			+ "and la.activityDateTime > :todayDate "
			+ "and la.activityDateTime < :tomorrowDate "
			+ "ORDER BY la.activityDateTime asc")
	List<LeadActivity> getRecentPendingActivity(@Param("leadId")Long leadId,@Param("todayDate") Date todayDate,@Param("tomorrowDate") Date tomorrowDate);

	@Query(value="SELECT la from LeadActivity la where la.isOpen = true and la.lead.leadId=:leadId order by la.activityDateTime desc")
	List<LeadActivity> getRecentClosedActivity(Long leadId);

	@Query(value="SELECT la from LeadActivity la where la.isOpen = false and la.lead.leadId=:leadId order by la.activityDateTime desc")
	List<LeadActivity> getRecentActivityIrrespectiveOfStatus(Long leadId);

	@Query(value="SELECT la from LeadActivity la where la.isOpen = true and  la.activityDateTime>:todayEndDate "
			+ "and la.lead.leadId=:leadId order by la.activityDateTime asc ")
	List<LeadActivity> getRecentUpcomingActivity(Long leadId,@Param("todayEndDate") Date todayEndDate);

	@Query(value="SELECT la from LeadActivity la where la.isOpen = true and  la.activityDateTime<:atStartOfDay "
			+ "and la.lead.leadId=:leadId order by la.activityDateTime desc ")
	List<LeadActivity> getRecentPastActivity(Long leadId, Date atStartOfDay);
}

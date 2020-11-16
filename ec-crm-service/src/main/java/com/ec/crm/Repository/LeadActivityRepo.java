package com.ec.crm.Repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ec.crm.Data.LeadLastUpdatedDAO;
import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Model.LeadActivity;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface LeadActivityRepo extends BaseRepository<LeadActivity, Long>, JpaSpecificationExecutor<LeadActivity>
{
	@Query(value = "SELECT l from LeadActivity l where l.lead.leadId=:leadId and l.activityType=:activityType and l.isOpen=true")
	List<LeadActivity> findByLeadActivityTypeOpen(@Param("leadId") Long leadId,
			@Param("activityType") ActivityTypeEnum activityType);

	@Query(value = "SELECT l from LeadActivity l where l.lead.leadId=:leadId and l.isOpen=true order by l.activityDateTime")
	List<LeadActivity> fetchPendingActivitiesForLead(@Param("leadId") Long leadId);

	@Query(value = "SELECT l from LeadActivity l where l.lead.leadId=:leadId and l.isOpen=false order by l.activityDateTime desc")
	List<LeadActivity> fetchPastActivitiesForLead(@Param("leadId") Long leadId);

	@Query(value = "SELECT l from LeadActivity l where l.lead.leadId=:leadId and l.isOpen=true")
	List<LeadActivity> findAllByOpenActivitiesByLeadId(@Param("leadId") Long leadId);

	@Query(value = "SELECT la from LeadActivity la where la.lead.leadId=:leadId ORDER BY la.activityDateTime DESC")
	List<LeadActivity> findAllActivitiesForLead(@Param("leadId") Long leadId);

	@Query(value = "SELECT la from LeadActivity la where la.isOpen = true and la.lead.leadId=:leadId "
			+ "and la.activityDateTime > :todayDate " + "and la.activityDateTime < :tomorrowDate "
			+ "ORDER BY la.activityDateTime asc")
	List<LeadActivity> getRecentPendingActivity(@Param("leadId") Long leadId, @Param("todayDate") Date todayDate,
			@Param("tomorrowDate") Date tomorrowDate);

	@Query(value = "SELECT la from LeadActivity la where la.isOpen = true and la.lead.leadId=:leadId order by la.activityDateTime desc")
	List<LeadActivity> getRecentClosedActivity(@Param("leadId") Long leadId);

	@Query(value = "SELECT la from LeadActivity la where la.isOpen = false and la.lead.leadId=:leadId order by la.activityDateTime desc")
	List<LeadActivity> getRecentActivityIrrespectiveOfStatus(@Param("leadId") Long leadId);

	@Query(value = "SELECT la from LeadActivity la where la.isOpen = true and  la.activityDateTime>:todayEndDate "
			+ "and la.lead.leadId=:leadId order by la.activityDateTime asc ")
	List<LeadActivity> getRecentUpcomingActivity(@Param("leadId") Long leadId,
			@Param("todayEndDate") Date todayEndDate);

	@Query(value = "SELECT la from LeadActivity la where la.isOpen = true and  la.activityDateTime<:atStartOfDay "
			+ "and la.lead.leadId=:leadId order by la.activityDateTime desc ")
	List<LeadActivity> getRecentPastActivity(@Param("leadId") Long leadId, @Param("atStartOfDay") Date atStartOfDay);

	@Query(value = "SELECT new com.ec.crm.Data.LeadLastUpdatedDAO(l.lead.leadId,max(l.modified)) from LeadActivity l group by l.lead.leadId")
	List<LeadLastUpdatedDAO> fetchLastUpdatedDetails();

	@Query(value = "SELECT MAX(modified) FROM LeadActivity la where la.lead.leadId=:leadId and la.lead.status not in ('Deal_closed','Deal_Lost')")
	Date fetchLastModified(@Param("leadId") Long leadId);

	@Query(value = "SELECT la from LeadActivity la where la.created BETWEEN :fromdate AND :todate")
	List<LeadActivity> getActivity(@Param("fromdate") Date fromdate, @Param("todate") Date todate);

	@Query(value = "SELECT la.lead.asigneeId, count(la.lead), count(la) from LeadActivity la where "
			+ "la.activityType='Deal_Close' group by la.lead.asigneeId")
	List getConversionRatio();

	@Query(value = "SELECT count(distinct la) from LeadActivity la where la.activityType='Property_Visit' and la.creatorId=:id")
	Long getpropertyvisit(@Param("id") Long id);

	@Query(value = "SELECT la from LeadActivity la where la.lead.leadId=:leadId ORDER BY la.created desc")
	List<LeadActivity> fetchMostRecentLeadActivity(@Param("leadId") Long leadId);

	@Query(value = "SELECT la from LeadActivity la where la.created>created and la.activityType IN (com.ec.crm.Enums.ActivityTypeEnum.Deal_Close"
			+ ",com.ec.crm.Enums.ActivityTypeEnum.Deal_Lost,com.ec.crm.Enums.ActivityTypeEnum.Property_Visit)")
	List<LeadActivity> fetchActivitiesAfter(@Param("created") Date created);

	@Query(value = "SELECT count(la) from LeadActivity la where la.lead.leadId=:leadId")
	int findLeadUsageCount(@Param("leadId") Long leadId);

	@Query(value = "SELECT la.leadActivityId from LeadActivity la where TIMESTAMPDIFF(MINUTE,now(),activityDateTime) < 10 and TIMESTAMPDIFF(MINUTE,now(),activityDateTime) > 0",nativeQuery = false) 
	List<Long> findUpcomingActivities();

	@Query(value = "SELECT count(la) from LeadActivity la where la.lead.leadId=:leadId AND (la.description LIKE '%Default activity created for new Lead%' OR la.description LIKE 'Call activity rescheduled from default call activity') AND la.isOpen=true")
	int getOpenDefaultActivities(@Param("leadId") Long leadId);

	@Query(value = "SELECT count(la) from LeadActivity la where la.lead.leadId=:leadId AND la.activityType IN (com.ec.crm.Enums.ActivityTypeEnum.Call) AND la.isOpen=true")
	int getOpenCallActivities(@Param("leadId") Long leadId);
}

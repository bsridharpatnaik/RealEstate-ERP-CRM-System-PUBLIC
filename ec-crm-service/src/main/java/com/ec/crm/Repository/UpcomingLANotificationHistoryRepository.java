package com.ec.crm.Repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.LeadActivityNotificationHistory;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface UpcomingLANotificationHistoryRepository extends BaseRepository<LeadActivityNotificationHistory, Long>,
		JpaSpecificationExecutor<LeadActivityNotificationHistory>
{

}

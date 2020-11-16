package com.ec.crm.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ec.crm.Model.LeadActivityNotificationHistory;
import com.ec.crm.Model.NotificationSent;
import com.ec.crm.ReusableClasses.BaseRepository;
import org.springframework.data.repository.CrudRepository;

@Repository
public interface NotificationSentRepository extends CrudRepository<NotificationSent, Long> 
{
	
     NotificationSent save(NotificationSent ns);
     NotificationSent findByLeadActivityId(Long activityId);
	
}

package com.ec.crm.Repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ec.crm.Model.NotificationSent;

@Repository
public interface NotificationSentRepository extends CrudRepository<NotificationSent, Long> {

	@SuppressWarnings("unchecked")
	NotificationSent save(NotificationSent ns);

	NotificationSent findByLeadActivityIdAndStatus(Long activityId, String status);

}

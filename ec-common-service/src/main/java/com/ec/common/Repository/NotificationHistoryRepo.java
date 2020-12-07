package com.ec.common.Repository;

import org.springframework.stereotype.Repository;

import com.ec.ReusableClasses.BaseRepository;
import com.ec.common.Model.NotificationHistory;

@Repository
public interface NotificationHistoryRepo extends BaseRepository<NotificationHistory, Long>
{

}

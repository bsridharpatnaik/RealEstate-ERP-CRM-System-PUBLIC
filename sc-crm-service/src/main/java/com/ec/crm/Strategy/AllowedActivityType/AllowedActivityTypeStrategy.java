package com.ec.crm.Strategy.AllowedActivityType;

import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Enums.LeadStatusEnum;

import java.util.List;

public interface AllowedActivityTypeStrategy {
    List<ActivityTypeEnum>  fetchAllowedActivities(LeadStatusEnum status);
}

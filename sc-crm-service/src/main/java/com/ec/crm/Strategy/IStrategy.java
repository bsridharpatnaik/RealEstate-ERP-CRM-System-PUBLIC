package com.ec.crm.Strategy;

import com.ec.crm.Data.StrategyEnum;
import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Enums.LeadStatusEnum;

import java.util.List;

public interface IStrategy {
    List<ActivityTypeEnum>  fetchAllowedActivities(LeadStatusEnum status);
    public StrategyEnum getStrategyName();
}

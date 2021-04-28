package com.ec.crm.Strategy;

import com.ec.crm.Data.StrategyEnum;
import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Enums.LeadStatusEnum;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StarategyEgcity implements IStrategy {
    @Override
    public List<ActivityTypeEnum> fetchAllowedActivities(LeadStatusEnum status) {
        return null;
    }

    @Override
    public StrategyEnum getStrategyName() {
        return StrategyEnum.egcity;
    }
}

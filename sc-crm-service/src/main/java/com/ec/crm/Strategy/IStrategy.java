package com.ec.crm.Strategy;

import com.ec.crm.Enums.InstanceEnum;
import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Model.ConversionRatio;
import com.ec.crm.Model.StagnantStats;

import java.util.List;

public interface IStrategy {
    List<ActivityTypeEnum>  fetchAllowedActivities(LeadStatusEnum status);
    public InstanceEnum getStrategyName();

    public List<ConversionRatio> fetchConversionRatio();

    public List<StagnantStats> returnStagnantStats();
}

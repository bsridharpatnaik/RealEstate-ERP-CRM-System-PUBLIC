package com.ec.crm.Strategy;

import com.ec.crm.Enums.InstanceEnum;
import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Model.ConversionRatio;
import com.ec.crm.Model.StagnantStats;
import com.ec.crm.Repository.ConvertionRatioRepo;
import com.ec.crm.Repository.StagnantStatsRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StarategyEgcity implements IStrategy {

    @Autowired
    StagnantStatsRepo stagnantStatsRepo;

    @Autowired
    ConvertionRatioRepo convertionRatioRepo;

    @Override
    public List<ActivityTypeEnum> fetchAllowedActivities(LeadStatusEnum status) {

        if (status.equals(LeadStatusEnum.Deal_Closed))
        {
            List<ActivityTypeEnum> allowedActivities = new ArrayList<ActivityTypeEnum>();
            allowedActivities.add(ActivityTypeEnum.Call);
            allowedActivities.add(ActivityTypeEnum.Meeting);
            allowedActivities.add(ActivityTypeEnum.Reminder);
            allowedActivities.add(ActivityTypeEnum.Email);
            allowedActivities.add(ActivityTypeEnum.Message);
            allowedActivities.add(ActivityTypeEnum.Deal_Cancelled);
            return allowedActivities;
        } else
        {
            List<ActivityTypeEnum> allowedActivities = new ArrayList<ActivityTypeEnum>();
            allowedActivities.add(ActivityTypeEnum.Call);
            allowedActivities.add(ActivityTypeEnum.Meeting);
            allowedActivities.add(ActivityTypeEnum.Reminder);
            allowedActivities.add(ActivityTypeEnum.Message);
            allowedActivities.add(ActivityTypeEnum.Deal_Close);
            allowedActivities.add(ActivityTypeEnum.Deal_Lost);
            allowedActivities.add(ActivityTypeEnum.Property_Visit);
            allowedActivities.add(ActivityTypeEnum.Email);
            return allowedActivities;
        }
    }

    @Override
    public InstanceEnum getStrategyName() {
        return InstanceEnum.egcity;
    }

    @Override
    public List<ConversionRatio> fetchConversionRatio() {
       return convertionRatioRepo.findAll();
    }

    @Override
    public List<StagnantStats> returnStagnantStats() {
         return stagnantStatsRepo.findAll();
    }
}

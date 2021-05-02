package com.ec.crm.Strategy;

import com.ec.crm.Enums.InstanceEnum;
import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Enums.LeadStatusEnum;

import java.util.ArrayList;
import java.util.List;

public class StrategySuncity implements IStrategy {
    @Override
    public List<ActivityTypeEnum> fetchAllowedActivities(LeadStatusEnum status) {

        if (status.equals(LeadStatusEnum.Deal_Closed))
        {
            List<ActivityTypeEnum> allowedActivities = new ArrayList<ActivityTypeEnum>();
            allowedActivities.add(ActivityTypeEnum.Call);
            allowedActivities.add(ActivityTypeEnum.Meeting);
            allowedActivities.add(ActivityTypeEnum.Reminder);
            //allowedActivities.add(ActivityTypeEnum.Email);
            //allowedActivities.add(ActivityTypeEnum.Message);
            return allowedActivities;
        } else
        {
            List<ActivityTypeEnum> allowedActivities = new ArrayList<ActivityTypeEnum>();
            allowedActivities.add(ActivityTypeEnum.Call);
            allowedActivities.add(ActivityTypeEnum.Meeting);
            allowedActivities.add(ActivityTypeEnum.Reminder);
            //allowedActivities.add(ActivityTypeEnum.Message);
            allowedActivities.add(ActivityTypeEnum.Deal_Close);
            allowedActivities.add(ActivityTypeEnum.Deal_Lost);
            allowedActivities.add(ActivityTypeEnum.Property_Visit);
            //allowedActivities.add(ActivityTypeEnum.Email);
            return allowedActivities;
        }
    }

    @Override
    public InstanceEnum getStrategyName() {
        return InstanceEnum.suncity;
    }
}

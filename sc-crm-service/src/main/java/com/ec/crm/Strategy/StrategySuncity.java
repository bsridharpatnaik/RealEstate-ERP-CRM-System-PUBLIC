package com.ec.crm.Strategy;

import com.ec.crm.Enums.InstanceEnum;
import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Model.ConversionRatio;
import com.ec.crm.Model.ConversionRatioPropertyType;
import com.ec.crm.Model.StagnantStats;
import com.ec.crm.Repository.ConvertionRatioPropertyTypeRepo;
import com.ec.crm.Repository.StagnantStatsPropertyTypeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class StrategySuncity implements IStrategy {

    @Autowired
    ConvertionRatioPropertyTypeRepo convertionRatioRepo;

    @Autowired
    StagnantStatsPropertyTypeRepo stagnantStatsRepo;

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

    @Override
    public List<ConversionRatio> fetchConversionRatio() {
        List<ConversionRatio> cr = convertionRatioRepo.findAll().stream()
                                                                .map(c -> new ConversionRatio(c))
                                                                .collect(toList());
        return cr;
    }

    @Override
    public List<StagnantStats> returnStagnantStats() {
        List<StagnantStats> ss = stagnantStatsRepo.findAll().stream()
                                                            .map(c -> new StagnantStats(c))
                                                            .collect(toList());
        return ss;
    }
}

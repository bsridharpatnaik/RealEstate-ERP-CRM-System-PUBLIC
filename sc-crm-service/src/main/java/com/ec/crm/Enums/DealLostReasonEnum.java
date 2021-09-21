package com.ec.crm.Enums;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public enum DealLostReasonEnum {
    Location_Issue,
    Construction_Quality_Issue,
    High_Pricing,
    Inner_Plan_Issue,
    Small_Plot_Area,
    High_Maintenance_Charge,
    Long_Possession_Period,
    FlyAsh_Brick_Issue,
    Costly_Society_Electrification,
    Empty,
    Other;

    public static List<DealLostReasonEnum> getValidDealLostReasons()
    {
        List<DealLostReasonEnum> validReasons = new ArrayList<DealLostReasonEnum>();
        for(DealLostReasonEnum dr : DealLostReasonEnum.values())
        {
            if(!dr.equals(DealLostReasonEnum.Empty))
                validReasons.add(dr);
        }
        return validReasons;
    }
}
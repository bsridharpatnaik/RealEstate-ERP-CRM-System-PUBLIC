package com.ec.crm.Service;

import com.ec.crm.Data.StrategyEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InstanceService {

    @Value("${spring.profiles.active}")
    private String profile;

    public StrategyEnum getInstance()
    {
        if(profile.contains("ec-"))
            return StrategyEnum.egcity;
        else if(profile.contains("sc-"))
            return StrategyEnum.suncity;
        return null;
    }
}

package com.ec.crm.Service;

import com.ec.crm.Enums.InstanceEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InstanceService {

    @Value("${spring.profiles.active}")
    private String profile;

    public InstanceEnum getInstance() {
        if (profile.contains("ec-"))
            return InstanceEnum.egcity;
        else if (profile.contains("sc-"))
            return InstanceEnum.suncity;
        return null;
    }

    public String getEnvironment(){
        if(profile.toLowerCase().contains("local"))
            return "local";
        else if(profile.toLowerCase().contains("qa"))
            return "qa";
        else if(profile.toLowerCase().contains("prod"))
            return "prod";
        else return null;
    }
}

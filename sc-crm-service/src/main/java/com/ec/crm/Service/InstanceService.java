package com.ec.crm.Service;
import com.ec.crm.Enums.InstanceEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InstanceService {
    @Value("${spring.profiles.active}")
    private String profile;

    public InstanceEnum getInstance()
    {
        if(profile.contains("ec-"))
            return InstanceEnum.egcity;
        else if(profile.contains("sc-"))
            return InstanceEnum.suncity;
        return null;
    }
}

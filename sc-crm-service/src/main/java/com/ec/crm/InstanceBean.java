package com.ec.crm;

import com.ec.crm.Enums.InstanceEnum;
import com.ec.crm.Service.InstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InstanceBean {

    @Autowired
    InstanceService iService;

    @Bean("currentInstance")
    public InstanceEnum fetchInstance() throws Exception {
        return iService.getInstance();
    }
}

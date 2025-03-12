package com.ec.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ActiveProfileService {
    @Value("${spring.profiles.active}")
    private String profile;

    public String fetchProfile() {
        if (profile.contains("sc-")) {
            return "sc-";
        }
        return "others";
    }
}

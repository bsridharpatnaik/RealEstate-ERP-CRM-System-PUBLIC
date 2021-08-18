package com.ec.application.controller;

import com.ec.application.service.SMSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sms")
public class SMSController {

    @Autowired
    SMSService smsService;

    @GetMapping("/iostats")
    public void sendIOStats() throws Exception {
        smsService.sendIOStats();
    }
}

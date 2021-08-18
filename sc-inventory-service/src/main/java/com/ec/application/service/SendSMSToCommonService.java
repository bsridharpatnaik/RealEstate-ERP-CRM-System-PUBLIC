package com.ec.application.service;

import com.ec.application.data.SMSCommonPayload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
public class SendSMSToCommonService {

    public void sendSmsToCommonService(List<HashMap<String,String>> contents, String flowId){
        SMSCommonPayload smsCommonPayload = new SMSCommonPayload();
    }
}

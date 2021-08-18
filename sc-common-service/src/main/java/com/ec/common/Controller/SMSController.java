package com.ec.common.Controller;

import com.ec.common.Data.SMSCommonPayload;
import com.ec.common.Service.SMSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/sms")
public class SMSController {
    @Autowired
    SMSService smsService;

    //@PreAuthorize("#request.getRemoteAddr().equals(#request.getLocalAddr())")
    @PostMapping("/send")
    public HttpEntity<? extends Object> sendSMS(@RequestBody SMSCommonPayload payload ) throws URISyntaxException {
        try {
            smsService.sendSms(payload.getSmsBody(),payload.getFlowId());
            return new ResponseEntity<String>("Success", HttpStatus.OK);
        }catch(Exception e){
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        }

    }
}

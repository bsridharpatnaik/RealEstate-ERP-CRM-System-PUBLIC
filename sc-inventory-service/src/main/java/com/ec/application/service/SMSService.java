package com.ec.application.service;

import com.ec.application.config.ProjectConstants;
import com.ec.application.data.InstanceList;
import com.ec.application.data.SMSCommonPayload;
import com.ec.application.model.InwardOutwardTrend;
import com.ec.application.model.SMSDeliveryList;
import com.ec.application.repository.InwardOutwardTrendRepo;
import com.ec.application.repository.SMSDeliveryListRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class SMSService {
    
    @Autowired
    InstanceService instanceService;

    @Autowired
    DashboardServiceV2 dashboardServiceV2;

    @Autowired
    SMSDeliveryListRepo smsDeliveryListRepo;

    @Autowired
    InwardOutwardTrendRepo ioTrendRepo;

    Logger log = LoggerFactory.getLogger(UserDetailsService.class);

    public void sendIOStats() throws Exception {
        log.info("Sending IO Stats");
        InstanceList instance = instanceService.getInstance();
        log.info("Selected Instance - "+instance.toString());
        if (instance.equals(InstanceList.egcity))
            fetchIOStatsAndSendSMSforEGCity();
    }

    private void fetchIOStatsAndSendSMSforEGCity() throws Exception {
        String flowId = ProjectConstants.flowIdForEGCityIOStats;
        com.ec.application.multitenant.ThreadLocalStorage.setTenantName(ProjectConstants.egCityTenantName);
        InwardOutwardTrend latestTrend = fetchLatestDataFromTRend();
        Optional<SMSDeliveryList> deliveryList = smsDeliveryListRepo.findById(ProjectConstants.IODeliveryListForEgcity);

        if(!deliveryList.isPresent())
            throw new Exception("Delivery List not found for ProjectConstants.IODeliveryListForEgcity");

        List<String> numbers = Arrays.asList(deliveryList.get().getNumbers().split(","));

        SMSCommonPayload smsCommonPayload = convertEgcityNumbersToPayload(numbers,latestTrend,flowId);

    }

    private SMSCommonPayload convertEgcityNumbersToPayload(List<String> numbers, InwardOutwardTrend latestTrend,String flowId) {
        SMSCommonPayload smsCommonPayload = new SMSCommonPayload();
        smsCommonPayload.setFlowId(flowId);
        List<HashMap<String,String>> body = new ArrayList<HashMap<String,String>>();
        for(String number:numbers){
            HashMap<String,String > childBody = new HashMap<>();
            childBody.put("mobiles",number);
            childBody.put("inward",latestTrend.getInwardCount().toString());
            childBody.put("outward",latestTrend.getOutwardCount().toString());
            body.add(childBody);
        }
        smsCommonPayload.setSmsBody(body);
        return smsCommonPayload;
    }

    public InwardOutwardTrend fetchLatestDataFromTRend() throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String strDate = dateFormat.format(new Date());
        log.info("Searching for Trend for date - "+strDate);
        Optional<InwardOutwardTrend> ioTrend = ioTrendRepo.findById(strDate);

        if(!ioTrend.isPresent())
            throw new Exception("Not able to find IO Trend with today's date");

        return ioTrend.get();
    }
}

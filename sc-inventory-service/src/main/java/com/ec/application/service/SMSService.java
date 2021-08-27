package com.ec.application.service;

import com.ec.application.ReusableClasses.CommonUtils;
import com.ec.application.config.ProjectConstants;
import com.ec.application.data.InstanceList;
import com.ec.application.data.SMSExternalPayloadData;
import com.ec.application.data.SMSGatewayResponse;
import com.ec.application.model.InwardOutwardTrend;
import com.ec.application.model.SMSDeliveryList;
import com.ec.application.repository.InwardOutwardTrendRepo;
import com.ec.application.repository.SMSDeliveryListRepo;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
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

    @Value("${sendsms}")
    private Boolean sendSms;

    public SMSGatewayResponse sendSMStoGateway(SMSExternalPayloadData payloadData) throws UnirestException {

        if (sendSms) {
            HttpResponse<JsonNode> response = Unirest.post(ProjectConstants.smsGatewayURL)
                    .header("authkey", ProjectConstants.authKey)
                    .header("content-type", "application/JSON")
                    .body(CommonUtils.convertObjectToJson(payloadData))
                    .asJson();
            JSONObject myObj = response.getBody().getObject();
            SMSGatewayResponse res = new SMSGatewayResponse(myObj.getString("message"), myObj.getString("type"));
            return res;
        } else {
            SMSGatewayResponse res = new SMSGatewayResponse("message", "SMS Service Disabled");
            log.info("SMS Gateway Response - " + res.toString());
            return res;
        }
    }

    public SMSGatewayResponse sendIOStats() throws Exception {
        log.info("Sending IO Stats");
        InstanceList instance = instanceService.getInstance();
        log.info("Selected Instance - " + instance.toString());
        if (instance.equals(InstanceList.egcity))
            return fetchIOStatsAndSendSMSforEGCity();
        else if (instance.equals(InstanceList.suncity))
            return fetchIOStatsAndSendSMSforSunCity();
        return null;
    }

    private SMSGatewayResponse fetchIOStatsAndSendSMSforSunCity() throws Exception {
        String flowId = ProjectConstants.flowIdForSuncityIOStats;
        com.ec.application.multitenant.ThreadLocalStorage.setTenantName(ProjectConstants.sunCityTenantName);
        Optional<SMSDeliveryList> deliveryList = smsDeliveryListRepo.findById(ProjectConstants.IODeliveryListForSuncity);

        if (!deliveryList.isPresent())
            throw new Exception("Delivery List not found for " + ProjectConstants.IODeliveryListForSuncity);

        List<String> numbers = Arrays.asList(deliveryList.get().getNumbers().split(","));
        SMSExternalPayloadData smsPayload = formPayloadDataForSuncity(numbers);
        return sendSMStoGateway(smsPayload);
    }

    private SMSExternalPayloadData formPayloadDataForSuncity(List<String> numbers) throws Exception {
        List<HashMap<String, String>> body = new ArrayList<HashMap<String, String>>();
        InwardOutwardTrend scLatestTrend = fetchLatestDataFromTRend("suncitynx");
        InwardOutwardTrend kpLatestTrend = fetchLatestDataFromTRend("kalpavrish");
        InwardOutwardTrend rsLatestTrend = fetchLatestDataFromTRend("riddhisiddhi");
        InwardOutwardTrend smcLatestTrend = fetchLatestDataFromTRend("smartcity");
        InwardOutwardTrend bpLatestTrend = fetchLatestDataFromTRend("businesspark");

        for (String number : numbers) {
            HashMap<String, String> childBody = new HashMap<>();
            childBody.put("mobiles", number);
            childBody.put("date", scLatestTrend.getDate());
            childBody.put("sc", scLatestTrend.getInwardCount().toString() + "/" + scLatestTrend.getOutwardCount().toString());
            childBody.put("kp", kpLatestTrend.getInwardCount().toString() + "/" + kpLatestTrend.getOutwardCount().toString());
            childBody.put("rs", rsLatestTrend.getInwardCount().toString() + "/" + rsLatestTrend.getOutwardCount().toString());
            childBody.put("smc", smcLatestTrend.getInwardCount().toString() + "/" + smcLatestTrend.getOutwardCount().toString());
            childBody.put("bp", bpLatestTrend.getInwardCount().toString() + "/" + bpLatestTrend.getOutwardCount().toString());
            body.add(childBody);
        }
        return new SMSExternalPayloadData(ProjectConstants.flowIdForSuncityIOStats, body);
    }

    private SMSGatewayResponse fetchIOStatsAndSendSMSforEGCity() throws Exception {
        String flowId = ProjectConstants.flowIdForEGCityIOStats;
        InwardOutwardTrend latestTrend = fetchLatestDataFromTRend(ProjectConstants.egCityTenantName);
        Optional<SMSDeliveryList> deliveryList = smsDeliveryListRepo.findById(ProjectConstants.IODeliveryListForEgcity);

        if (!deliveryList.isPresent())
            throw new Exception("Delivery List not found for " + ProjectConstants.IODeliveryListForEgcity);

        List<String> numbers = Arrays.asList(deliveryList.get().getNumbers().split(","));
        SMSExternalPayloadData smsPayload = convertEgcityNumbersToPayload(numbers, latestTrend, flowId);
        return sendSMStoGateway(smsPayload);
    }

    private SMSExternalPayloadData convertEgcityNumbersToPayload(List<String> numbers, InwardOutwardTrend latestTrend, String flowId) {
        List<HashMap<String, String>> body = new ArrayList<HashMap<String, String>>();
        for (String number : numbers) {
            HashMap<String, String> childBody = new HashMap<>();
            childBody.put("mobiles", number);
            childBody.put("date", latestTrend.getDate());
            childBody.put("inward", latestTrend.getInwardCount().toString());
            childBody.put("outward", latestTrend.getOutwardCount().toString());
            body.add(childBody);
        }
        return new SMSExternalPayloadData(ProjectConstants.flowIdForEGCityIOStats, body);
    }

    public InwardOutwardTrend fetchLatestDataFromTRend(String tenantName) throws Exception {
        com.ec.application.multitenant.ThreadLocalStorage.setTenantName(tenantName);
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String strDate = dateFormat.format(new Date());
        log.info("Searching for Trend for date - " + strDate);
        Optional<InwardOutwardTrend> ioTrend = ioTrendRepo.findById(strDate);

        if (!ioTrend.isPresent())
            throw new Exception("Not able to find IO Trend with today's date");

        return ioTrend.get();
    }
}

package com.ec.common.Service;

import com.ec.common.Configuration.ProjectConstants;
import com.ec.common.Data.SMSGatewayResponse;
import com.ec.common.Data.SMSPayloadData;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Service
public class SMSService {

    @Autowired
    RestTemplate restTemplate;

    @Value("${smsAuthKey}")
    private String smsAuthKey;

    public void sendSms(List<HashMap<String, String>> recipients, String flowId) throws URISyntaxException {

        try {

            URI uri = new URI(ProjectConstants.smsGatewayURL);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("content-type", "application/JSON");
            headers.set("authkey", smsAuthKey);
            SMSPayloadData smsPayloadData = new SMSPayloadData(flowId, ProjectConstants.senderId, recipients);
            HttpEntity<SMSPayloadData> request = new HttpEntity<SMSPayloadData>(smsPayloadData, headers);

            MappingJackson2HttpMessageConverter jsonHttpMessageConverter = new MappingJackson2HttpMessageConverter();
            MappingJackson2HttpMessageConverter jsonHttpMessageConverter2 = new MappingJackson2HttpMessageConverter();
            jsonHttpMessageConverter.getObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            jsonHttpMessageConverter2.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
            restTemplate.getMessageConverters().add(jsonHttpMessageConverter);
            restTemplate.getMessageConverters().add(jsonHttpMessageConverter2);
            ResponseEntity<SMSGatewayResponse> result = restTemplate.postForEntity(uri, request, SMSGatewayResponse.class);
            System.out.println("RESULT = ########"+result.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

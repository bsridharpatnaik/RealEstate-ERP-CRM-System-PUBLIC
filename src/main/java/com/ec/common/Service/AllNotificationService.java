package com.ec.common.Service;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.ec.common.Data.ReturnAllNotificationsData;

@Service
public class AllNotificationService 
{
	
	@Autowired
	WebClient.Builder webClientBuilder;
	
	@Autowired
	HttpServletRequest request;
	
	@Autowired
	RestTemplate restTemplate;
	
	@Value("${inven.serverurl}")
	private String reqUrl;
    
	public ReturnAllNotificationsData getAllNotifications() throws Exception 
	{
		ReturnAllNotificationsData inventoryNotificationsData = new ReturnAllNotificationsData();
		inventoryNotificationsData = getInventoryNotification();
		return inventoryNotificationsData;
	}

	private ReturnAllNotificationsData getInventoryNotification() throws Exception 
	{
		//try
		//{
			String url = reqUrl+"notification";
			System.out.println(url);
			HttpHeaders headers = new HttpHeaders();
		    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		    headers.add("Authorization", request.getHeader("Authorization"));
		    HttpEntity requestEntity = new HttpEntity(headers);
		    ResponseEntity<ReturnAllNotificationsData> response = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, ReturnAllNotificationsData.class, 1);
		    if(response.getStatusCode() == HttpStatus.OK) {
		        return response.getBody();
		    } else {
		        throw new Exception ("Error fetching notification");
		    }
		//}
		//catch(Exception e)
		//{
		//	throw new Exception ("Error fetching notification");
		//}
	}

}

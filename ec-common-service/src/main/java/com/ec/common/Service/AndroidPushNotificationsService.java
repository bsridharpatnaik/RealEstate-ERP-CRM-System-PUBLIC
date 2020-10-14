package com.ec.common.Service;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ec.commonInterceptors.HeaderRequestInterceptor;

@Service
public class AndroidPushNotificationsService
{

	private static final String FIREBASE_SERVER_KEY = "AAAA8X9BPWI:APA91bEed98dFyFYq07vc1auLOLIR4A4-qQoWXYvCgBPQqgNEuUfAb9ChnhtrgLAJ9j5838-Rrtrg2vw-AliBxmho6ZAh3mood-H9UVLUy4Q35ZhuUi-PVCSzQ4cIWzEKJXpNqFgoVrr";
	private static final String FIREBASE_API_URL = "https://fcm.googleapis.com/fcm/send";

	@Async
	public CompletableFuture<String> send(HttpEntity<String> entity)
	{

		RestTemplate restTemplate = new RestTemplate();

		/**
		 * https://fcm.googleapis.com/fcm/send Content-Type:application/json
		 * Authorization:key=FIREBASE_SERVER_KEY
		 */

		ArrayList<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add(new HeaderRequestInterceptor("Authorization", "key=" + FIREBASE_SERVER_KEY));
		interceptors.add(new HeaderRequestInterceptor("Content-Type", "application/json"));
		restTemplate.setInterceptors(interceptors);

		String firebaseResponse = restTemplate.postForObject(FIREBASE_API_URL, entity, String.class);

		return CompletableFuture.completedFuture(firebaseResponse);
	}
}
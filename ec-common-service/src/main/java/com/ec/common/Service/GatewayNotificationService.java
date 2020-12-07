package com.ec.common.Service;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.ec.common.Data.NotificationRequestDto;
import com.ec.common.Model.NotificationHistory;
import com.ec.common.Repository.NotificationHistoryRepo;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GatewayNotificationService
{

	@Value("${app.firebase-config}")
	private String firebaseConfig;

	private FirebaseApp firebaseApp;

	@Autowired
	AndroidTokenDetailsService androidTokenDetailsService;

	@Autowired
	NotificationHistoryRepo notificationHistoryRepo;

	@PostConstruct
	private void initialize()
	{
		try
		{
			FirebaseOptions options = new FirebaseOptions.Builder()
					.setCredentials(
							GoogleCredentials.fromStream(new ClassPathResource(firebaseConfig).getInputStream()))
					.build();

			if (FirebaseApp.getApps().isEmpty())
			{
				this.firebaseApp = FirebaseApp.initializeApp(options);
			} else
			{
				this.firebaseApp = FirebaseApp.getInstance();
			}
		} catch (IOException e)
		{
			log.error("Create FirebaseApp Error", e);
		}
	}

	public String sendPnsToDevice(NotificationRequestDto notificationRequestDto) throws Exception
	{
		String token = androidTokenDetailsService.findTokenForUser(notificationRequestDto.getTargetUserId());
		Message message = Message.builder().setToken(token)
				.setNotification(new Notification(notificationRequestDto.getTitle(), notificationRequestDto.getBody()))
				.putData("content", notificationRequestDto.getTitle()).putData("body", notificationRequestDto.getBody())
				.build();

		String response = null;
		try
		{
			response = FirebaseMessaging.getInstance().send(message);
			saveNotificationToHistory(notificationRequestDto);
		} catch (FirebaseMessagingException e)
		{
			log.error("Fail to send firebase notification", e);
		}

		return response;
	}

	private void saveNotificationToHistory(NotificationRequestDto notificationRequestDto)
	{
		NotificationHistory nh = new NotificationHistory();
		nh.setBody(notificationRequestDto.getBody());
		nh.setTitle(notificationRequestDto.getTitle());
		nh.setUserId(notificationRequestDto.getTargetUserId());
		notificationHistoryRepo.save(nh);
	}
}

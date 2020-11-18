package com.ec.common.Service;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LeadActivityNotificationService
{

	/*
	 * @Value("${app.firebase-config}") private String firebaseConfig;
	 * 
	 * private FirebaseApp firebaseApp;
	 * 
	 * @PostConstruct private void initialize() { try { FirebaseOptions options =
	 * new FirebaseOptions.Builder() .setCredentials(
	 * GoogleCredentials.fromStream(new
	 * ClassPathResource(firebaseConfig).getInputStream())) .build();
	 * 
	 * if (FirebaseApp.getApps().isEmpty()) { this.firebaseApp =
	 * FirebaseApp.initializeApp(options); } else { this.firebaseApp =
	 * FirebaseApp.getInstance(); } } catch (IOException e) {
	 * log.error("Create FirebaseApp Error", e); } }
	 * 
	 * public String sendPnsToDevice(LANotificationRequestDto
	 * notificationRequestDto) { Message message =
	 * Message.builder().setToken(notificationRequestDto.getTarget())
	 * .setNotification(new Notification(notificationRequestDto.getTitle(),
	 * notificationRequestDto.getBody())) .putData("content",
	 * notificationRequestDto.getTitle()).putData("body",
	 * notificationRequestDto.getBody()) .build();
	 * 
	 * String response = null; try { response =
	 * FirebaseMessaging.getInstance().send(message); } catch
	 * (FirebaseMessagingException e) {
	 * log.error("Fail to send firebase notification", e); }
	 * 
	 * return response; }
	 */
}

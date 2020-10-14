package com.ec.common.Controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ec.common.Service.AndroidPushNotificationsService;

@RestController
public class AndroidNotificationController
{
	private final String TOPIC = "JavaSampleApproach";

	@Autowired
	AndroidPushNotificationsService androidPushNotificationsService;

	@RequestMapping(value = "/send", method = RequestMethod.GET, produces = "application/json")
	public ResponseEntity<String> send() throws JSONException
	{

		JSONObject body = new JSONObject();
		body.put("to",
				"fFZ3o9aXSHaYSQ2oZZXccP:APA91bG0EyAMyOIrtPNqzNDiPut5k7oSpUxqF483yp8OZ4UdgKBPVDbI_VEBJ8RZ0pkDyJOzMsRSOvdOatoXtAHJPP2neSukSvhLApbs5TOyrso5tVL8WOnX7gXhbAKPAa02y2gnoVQI");
		body.put("priority", "high");

		JSONObject notification = new JSONObject();
		notification.put("title", "Upcoming Meeting");
		notification.put("body", "Ashish Verma - 11:30 AM");

		JSONObject data = new JSONObject();
		data.put("Key-1", "JSA Data 1");
		data.put("Key-2", "JSA Data 2");

		body.put("notification", notification);
		body.put("data", data);

		/**
		 * { "notification": { "title": "JSA Notification", "body": "Happy Message!" },
		 * "data": { "Key-1": "JSA Data 1", "Key-2": "JSA Data 2" }, "to":
		 * "/topics/JavaSampleApproach", "priority": "high" }
		 */

		HttpEntity<String> request = new HttpEntity<>(body.toString());

		CompletableFuture<String> pushNotification = androidPushNotificationsService.send(request);
		CompletableFuture.allOf(pushNotification).join();

		try
		{
			String firebaseResponse = pushNotification.get();

			return new ResponseEntity<>(firebaseResponse, HttpStatus.OK);
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		} catch (ExecutionException e)
		{
			e.printStackTrace();
		}

		return new ResponseEntity<>("Push Notification ERROR!", HttpStatus.BAD_REQUEST);
	}
}

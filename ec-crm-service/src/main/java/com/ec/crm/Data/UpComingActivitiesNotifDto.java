package com.ec.crm.Data;

import lombok.Data;

@Data
public class UpComingActivitiesNotifDto
{
	private Long targetUserId;
	private String title;
	private String body;
}

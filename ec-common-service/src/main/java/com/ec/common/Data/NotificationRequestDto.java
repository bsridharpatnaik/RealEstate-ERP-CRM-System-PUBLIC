package com.ec.common.Data;

import lombok.Data;

@Data
public class NotificationRequestDto
{
	private Long targetUserId;
	private String title;
	private String body;
}

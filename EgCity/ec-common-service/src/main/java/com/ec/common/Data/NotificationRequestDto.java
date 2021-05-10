package com.ec.common.Data;

public class NotificationRequestDto
{
	private Long targetUserId;
	private String title;
	private String body;

	public Long getTargetUserId()
	{
		return targetUserId;
	}

	public void setTargetUserId(Long targetUserId)
	{
		this.targetUserId = targetUserId;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getBody()
	{
		return body;
	}

	public void setBody(String body)
	{
		this.body = body;
	}

}

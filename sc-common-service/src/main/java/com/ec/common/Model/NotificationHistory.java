package com.ec.common.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Where;

import com.ec.ReusableClasses.ReusableFields;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_history")
//@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@AllArgsConstructor
@NoArgsConstructor
public class NotificationHistory
{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "history_id", updatable = false, nullable = false)
	Long historyId;
	Long userId;
	String body;
	String title;

	public Long getHistoryId()
	{
		return historyId;
	}

	public void setHistoryId(Long historyId)
	{
		this.historyId = historyId;
	}

	public Long getUserId()
	{
		return userId;
	}

	public void setUserId(Long userId)
	{
		this.userId = userId;
	}

	public String getBody()
	{
		return body;
	}

	public void setBody(String body)
	{
		this.body = body;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}
}

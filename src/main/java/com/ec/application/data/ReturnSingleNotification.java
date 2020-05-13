package com.ec.application.data;

import java.util.Date;

import javax.persistence.Column;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReturnSingleNotification
{
	public static final String SOFT_DELETED_CLAUSE = "is_deleted = 'false'";

	Long notificationId;
	String type;
	String message;
	String source;
	
    @Column(name="is_deleted", columnDefinition="BOOLEAN DEFAULT true")
    public boolean isDeleted;

    @CreationTimestamp
	@Column(name = "created_at")
	@JsonProperty("created")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
	private Date created;
	
	
	@Column(name = "updated_at")
	@JsonProperty("updated")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
	@UpdateTimestamp
	private Date modified;

	public ReturnSingleNotification() {
		super();
	}

	public ReturnSingleNotification(Long notificationId,String source,String type, String message,Date created,
			Date modified) {
		super();
		this.notificationId=notificationId;
		this.type = type;
		this.message = message;
		this.isDeleted = false;
		this.source=source;
		this.created = created;
		this.modified = modified;
	}


	public String getSource() {
		return source;
	}


	public void setSource(String source) {
		this.source = source;
	}


	public Long getNotificationId() {
		return notificationId;
	}


	public void setNotificationId(Long notificationId) {
		this.notificationId = notificationId;
	}


	public String getType() {
		return type;
	}


	public void setType(String type) {
		this.type = type;
	}


	public String getMessage() {
		return message;
	}


	public void setMessage(String message) {
		this.message = message;
	}


	public boolean isDeleted() {
		return isDeleted;
	}


	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}


	public Date getCreated() {
		return created;
	}


	public void setCreated(Date created) {
		this.created = created;
	}


	public Date getModified() {
		return modified;
	}


	public void setModified(Date modified) {
		this.modified = modified;
	}


	public static String getSoftDeletedClause() {
		return SOFT_DELETED_CLAUSE;
	}
	
}

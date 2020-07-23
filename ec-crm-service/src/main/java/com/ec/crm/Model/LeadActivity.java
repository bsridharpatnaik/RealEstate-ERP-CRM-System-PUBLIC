package com.ec.crm.Model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.ec.crm.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@Entity
@Table(name = "`LeadActivity`")
public class LeadActivity extends ReusableFields implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "leadactivity_id", updatable = false, nullable = false)
	Long leadActivityId;

	@Column(name="activity_date")
	Date activityDate;
	
	@Column(name="creation_date")
	Date creationDate;
	
	@Column(name="title")
	String title;
	
	@Column(name="description")
	String description;
	
	@Column(name="tags")
	String tags;
	
	@Column(name="status")
	String status;
	
	@Column(name="time")
	Date time;
	
	@Column(name="user_id")
	Long userId;
	
	@OneToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="activitytype_id",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	@NotFound(action=NotFoundAction.IGNORE)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	ActivityType activityType;

	public Long getLeadActivityId() {
		return leadActivityId;
	}

	public void setLeadActivityId(Long leadActivityId) {
		this.leadActivityId = leadActivityId;
	}

	public Date getActivityDate() {
		return activityDate;
	}

	public void setActivityDate(Date activityDate) {
		this.activityDate = activityDate;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public ActivityType getActivityType() {
		return activityType;
	}

	public void setActivityType(ActivityType activityType) {
		this.activityType = activityType;
	}
	
	
}

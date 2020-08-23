package com.ec.crm.Data;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.springframework.lang.NonNull;

import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.Model.Lead;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import Deserializers.ToUsernameSerializer;
import lombok.Data;

//@Data
public class LeadActivityDTO 
{
	
	Long leadActivityId;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm")
	Date activityDateTime;
	
	@NonNull
	String title;
	
	@Column(name="description")
	String description;
	
	List<String> tags;
	
	@NonNull
	Boolean isOpen;
	
	@JsonSerialize(using=ToUsernameSerializer.class)
	Long creatorId;
	
	@JsonSerialize(using=ToUsernameSerializer.class)
	Long closedBy;
	
	Lead lead;
	
	String closingComment;
	
	@Enumerated(EnumType.STRING)
	ActivityTypeEnum activityType;

	public LeadActivityDTO() {
		super();
	}

	public LeadActivityDTO(Long leadActivityId, Date activityDateTime, String title, String description,
			List<String> tags, Boolean isOpen, Long creatorId, Long closedBy, Lead lead, String closingComment,
			ActivityTypeEnum activityType) {
		super();
		this.leadActivityId = leadActivityId;
		this.activityDateTime = activityDateTime;
		this.title = title;
		this.description = description;
		this.tags = tags;
		this.isOpen = isOpen;
		this.creatorId = creatorId;
		this.closedBy = closedBy;
		this.lead = lead;
		this.closingComment = closingComment;
		this.activityType = activityType;
	}

	public Long getLeadActivityId() {
		return leadActivityId;
	}

	public void setLeadActivityId(Long leadActivityId) {
		this.leadActivityId = leadActivityId;
	}

	public Date getActivityDateTime() {
		return activityDateTime;
	}

	public void setActivityDateTime(Date activityDateTime) {
		this.activityDateTime = activityDateTime;
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

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public Boolean getIsOpen() {
		return isOpen;
	}

	public void setIsOpen(Boolean isOpen) {
		this.isOpen = isOpen;
	}

	public Long getCreatorId() {
		return creatorId;
	}

	public void setCreatorId(Long creatorId) {
		this.creatorId = creatorId;
	}

	public Long getClosedBy() {
		return closedBy;
	}

	public void setClosedBy(Long closedBy) {
		this.closedBy = closedBy;
	}

	public Lead getLead() {
		return lead;
	}

	public void setLead(Lead lead) {
		this.lead = lead;
	}

	public String getClosingComment() {
		return closingComment;
	}

	public void setClosingComment(String closingComment) {
		this.closingComment = closingComment;
	}

	public ActivityTypeEnum getActivityType() {
		return activityType;
	}

	public void setActivityType(ActivityTypeEnum activityType) {
		this.activityType = activityType;
	}
	
	
}

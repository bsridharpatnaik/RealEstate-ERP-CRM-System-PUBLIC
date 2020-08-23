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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
}

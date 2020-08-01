package com.ec.crm.Model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.springframework.lang.NonNull;

import com.ec.crm.Enums.ActivityTypeEnum;
import com.ec.crm.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import Deserializers.ToUsernameSerializer;
import lombok.Data;
@Entity
@Table(name = "LeadActivity")
@Data
public class LeadActivity extends ReusableFields implements Serializable 
{
	/**
	 * 
	 */
	
	
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "leadactivity_id", updatable = false, nullable = false)
	Long leadActivityId;

	@Column(name="activity_date_time")
	@NonNull
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm")
	Date activityDateTime;
	
	@Column(name="title")
	@NonNull
	String title;
	
	@Column(name="duration",columnDefinition = "int default 0")
	Long duration;
	
	@Column(name="description")
	String description;
	
	@ElementCollection
	@CollectionTable(name="lead_activity_tags", joinColumns=@JoinColumn(name="leadActivityId"))
	@Column(name="tags")
	List<String> tags;
	
	@Column(name="isOpen",columnDefinition = "boolean default true")
	@NonNull
	Boolean isOpen;
	
	@JsonSerialize(using=ToUsernameSerializer.class)
	@Column(name="creator_id")
	Long creatorId;
	
	@JsonSerialize(using=ToUsernameSerializer.class)
	@Column(name="closed_by")
	Long closedBy;
	
	@OneToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="lead_id",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	@NotFound(action=NotFoundAction.IGNORE)
	Lead lead;
	
	String closingComment;
	@NonNull
	@Column(nullable=false)
	@Enumerated(EnumType.STRING)
	ActivityTypeEnum activityType;
	
	public LeadActivity() {
		super();
		// TODO Auto-generated constructor stub
	}
}

package com.ec.application.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "usage_area")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class UsageArea extends ReusableFields
{
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
	Long usageAreaId;
	
	@NonNull
	@Column(name="usagearea_name")
	String usageAreaName;
	
	String usageAreaDescription;

	public Long getUsageAreaId() {
		return usageAreaId;
	}

	public void setUsageAreaId(Long usageAreaId) {
		this.usageAreaId = usageAreaId;
	}

	public String getUsageAreaName() {
		return usageAreaName;
	}

	public void setUsageAreaName(String usageAreaName) {
		this.usageAreaName = usageAreaName;
	}

	public String getUsageAreaDescription() {
		return usageAreaDescription;
	}

	public void setUsageAreaDescription(String usageAreaDescription) {
		this.usageAreaDescription = usageAreaDescription;
	}
	
	
}

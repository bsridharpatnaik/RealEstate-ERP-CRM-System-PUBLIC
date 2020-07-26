package com.ec.crm.Model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.springframework.lang.NonNull;

import com.ec.crm.ReusableClasses.ReusableFields;

@Entity
@Table(name = "LeadStatus")
public class LeadStatus extends ReusableFields implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "leadstatus_id", updatable = false, nullable = false)
	Long leadStatusId;

	@NonNull
	@Enumerated(EnumType.STRING)
	@Column(name="status",nullable=false)
	StatusEnum status;
	
	@Column(name="lead_id")
	Long leadId;
	
	@Column(name="createdBy")
	Long createdById;
	
	@Column(name="modifiedBy")
	Long modifiedById;

	public LeadStatus(Long leadId2, String  string2, Long currentUserId, Long currentUserId2) 
	{
		this.leadId = leadId2;
		this.createdById = currentUserId;
		this.modifiedById = currentUserId2;
		this.status= StatusEnum.valueOf(string2);
	}

	public Long getLeadStatusId() {
		return leadStatusId;
	}

	public void setLeadStatusId(Long leadStatusId) {
		this.leadStatusId = leadStatusId;
	}
	
	public StatusEnum getStatus() {
		return status;
	}

	public void setStatus(StatusEnum status) {
		this.status = status;
	}

	public Long getLeadId() {
		return leadId;
	}

	public void setLeadId(Long leadId) {
		this.leadId = leadId;
	}

	public Long getCreatedById() {
		return createdById;
	}

	public void setCreatedById(Long createdById) {
		this.createdById = createdById;
	}

	public Long getModifiedById() {
		return modifiedById;
	}

	public void setModifiedById(Long modifiedById) {
		this.modifiedById = modifiedById;
	}
}

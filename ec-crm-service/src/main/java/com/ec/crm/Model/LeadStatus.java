package com.ec.crm.Model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.ec.crm.ReusableClasses.ReusableFields;

@Entity
@Table(name = "`LeadStatus`")
public class LeadStatus extends ReusableFields implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "leadstatus_id", updatable = false, nullable = false)
	Long leadStatusId;

	@Column(name="status_id")
	Long statusId;
	
	@Column(name="lead_id")
	Long leadId;
	
	@Column(name="user_id")
	Long userId;

	public Long getLeadStatusId() {
		return leadStatusId;
	}

	public void setLeadStatusId(Long leadStatusId) {
		this.leadStatusId = leadStatusId;
	}

	public Long getStatusId() {
		return statusId;
	}

	public void setStatusId(Long statusId) {
		this.statusId = statusId;
	}

	public Long getLeadId() {
		return leadId;
	}

	public void setLeadId(Long leadId) {
		this.leadId = leadId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}
	
	
}

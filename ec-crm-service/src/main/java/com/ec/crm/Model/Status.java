package com.ec.crm.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import com.ec.crm.ReusableClasses.ReusableFields;
@Entity
@Table(name = "Status")
public class Status extends ReusableFields{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "status_id", updatable = false, nullable = false)
	Long statusId;

	@NotBlank(message = "Name is mandatory")
	String name;

	public Long getStatusId() {
		return statusId;
	}

	public void setStatusId(Long statusId) {
		this.statusId = statusId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
}

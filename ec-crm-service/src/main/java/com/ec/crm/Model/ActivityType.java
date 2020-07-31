package com.ec.crm.Model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import com.ec.crm.Data.AssigneeDAO;
import com.ec.crm.ReusableClasses.ReusableFields;

import lombok.Data;

@Entity
@Table(name = "ActivityType")
@Data
public class ActivityType extends ReusableFields implements Serializable{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "activitytype_id", updatable = false, nullable = false)
	Long activityTypeId;
	
	@NotBlank(message = "Name is mandatory")
	String name;
}

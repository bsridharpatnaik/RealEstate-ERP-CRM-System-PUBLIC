package com.ec.crm.Model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.crm.Data.AssigneeDAO;
import com.ec.crm.ReusableClasses.ReusableFields;

import lombok.Data;
@Entity
@Table(name = "broker")
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Audited
@Data
public class Broker extends ReusableFields implements Serializable
{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "broker_id", updatable = false, nullable = false)
	Long brokerId;
	
	@NotBlank(message = "Name is mandatory")
	@Column(name="broker_name")
	String brokerName;
	
	@Column(name="broker_address")
	String brokerAddress;
	
	@NotBlank(message = "Mobile number is mandatory")
	@Column(name="broker_phoneno")
	String brokerPhoneno;
		
}

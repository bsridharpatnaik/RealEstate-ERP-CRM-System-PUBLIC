package com.ec.crm.Model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import com.ec.crm.ReusableClasses.ReusableFields;
@Entity
@Table(name = "broker")
public class Broker extends ReusableFields implements Serializable{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "broker_id", updatable = false, nullable = false)
	Long brokerId;
	
	@NotBlank(message = "Name is mandatory")
	@Column(name="broker_name")
	String brokerName;
	
	@NotBlank(message = "Address is mandatory")
	@Column(name="broker_address")
	String brokerAddress;
	
	@NotBlank(message = "Phone is mandatory")
	@Column(name="broker_phoneno")
	String brokerPhoneno;
	
	public Long getBrokerId() {
		return brokerId;
	}
	public void setBrokerId(Long brokerId) {
		this.brokerId = brokerId;
	}
	public String getBrokerName() {
		return brokerName;
	}
	public void setBrokerName(String brokerName) {
		this.brokerName = brokerName;
	}
	public String getBrokerAddress() {
		return brokerAddress;
	}
	public void setBrokerAddress(String brokerAddress) {
		this.brokerAddress = brokerAddress;
	}
	public String getBrokerPhoneno() {
		return brokerPhoneno;
	}
	public void setBrokerPhoneno(String brokerPhoneno) {
		this.brokerPhoneno = brokerPhoneno;
	}
	
	
}

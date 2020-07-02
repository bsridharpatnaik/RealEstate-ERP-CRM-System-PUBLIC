package com.ec.crm.Model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.ec.ReusableClasses.ReusableFields;
@Entity
@Table(name = "broker")
public class Broker extends ReusableFields implements Serializable{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "broker_id", updatable = false, nullable = false)
	Long brokerId;
	String broker_name;
	String broker_address;
	String broker_phoneno;
	public Long getBrokerId() {
		return brokerId;
	}
	public void setBrokerId(Long brokerId) {
		this.brokerId = brokerId;
	}
	public String getBroker_name() {
		return broker_name;
	}
	public void setBroker_name(String broker_name) {
		this.broker_name = broker_name;
	}
	public String getBroker_address() {
		return broker_address;
	}
	public void setBroker_address(String broker_address) {
		this.broker_address = broker_address;
	}
	public String getBroker_phoneno() {
		return broker_phoneno;
	}
	public void setBroker_phoneno(String broker_phoneno) {
		this.broker_phoneno = broker_phoneno;
	}
	
}

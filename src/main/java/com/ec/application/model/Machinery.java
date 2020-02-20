package com.ec.application.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.springframework.lang.NonNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "Machinery")
public class Machinery 
{

private static final long serialVersionUID = 1L;
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
	Long machineryId;
	@NonNull
	String machineryName;
	String machineryDescription;
	
	public Long getMachineryId() {
		return machineryId;
	}
	public void setMachineryId(Long machineryId) {
		this.machineryId = machineryId;
	}
	
	
	public String getMachineryName() {
		return machineryName;
	}
	public void setMachineryName(String machineryName) {
		this.machineryName = machineryName;
	}
	public String getMachineryDescription() {
		return machineryDescription;
	}
	public void setMachineryDescription(String machineryDescription) {
		this.machineryDescription = machineryDescription;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	
}

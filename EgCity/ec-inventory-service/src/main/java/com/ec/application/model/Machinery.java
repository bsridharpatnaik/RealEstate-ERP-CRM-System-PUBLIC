package com.ec.application.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.ec.application.Deserializers.ToTitleCaseDeserializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Entity
@Table(name = "Machinery")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class Machinery extends ReusableFields
{

private static final long serialVersionUID = 1L;
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	Long machineryId;
	
	@NonNull
	@Column
	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	String machineryName;
	
	@JsonDeserialize(using = ToSentenceCaseDeserializer.class)
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

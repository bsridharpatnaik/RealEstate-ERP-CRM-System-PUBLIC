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

import com.ec.application.SoftDelete.SoftDeletableEntity;

@Entity
@Table(name = "Machinery")
@Audited
@Where(clause = SoftDeletableEntity.SOFT_DELETED_CLAUSE)
public class Machinery extends SoftDeletableEntity
{

private static final long serialVersionUID = 1L;
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	Long machineryId;
	
	@NonNull
	@Column(unique=true)
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

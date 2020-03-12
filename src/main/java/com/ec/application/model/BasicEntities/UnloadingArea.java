package com.ec.application.model.BasicEntities;

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
@Table(name = "Unloading_Location")
@Audited
@Where(clause = SoftDeletableEntity.SOFT_DELETED_CLAUSE)
public class UnloadingArea 
{

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
	Long unloadingAreaId;
	
	@NonNull
	@Column(unique=true,name="unloadingAreaName")
	String unloadingAreaName;
	String unloadingAreaDescription;
	public Long getUnloadingAreaId() {
		return unloadingAreaId;
	}
	public void setUnloadingAreaId(Long unloadingAreaId) {
		this.unloadingAreaId = unloadingAreaId;
	}
	public String getUnloadingAreaName() {
		return unloadingAreaName;
	}
	public void setUnloadingAreaName(String unloadingAreaName) {
		this.unloadingAreaName = unloadingAreaName;
	}
	public String getUnloadingAreaDescription() {
		return unloadingAreaDescription;
	}
	public void setUnloadingAreaDescription(String unloadingAreaDescription) {
		this.unloadingAreaDescription = unloadingAreaDescription;
	}
	
}

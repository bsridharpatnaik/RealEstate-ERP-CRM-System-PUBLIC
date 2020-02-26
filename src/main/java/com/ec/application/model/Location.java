package com.ec.application.model;

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
@Table(name = "Usage_Location")
@Audited
@Where(clause = SoftDeletableEntity.SOFT_DELETED_CLAUSE)
public class Location extends SoftDeletableEntity
{
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
	Long loationId;
	
	@NonNull
	@Column(unique=true,name="location_name")
	String locationName;
	String locationDescription;
	public Long getLoationId() {
		return loationId;
	}
	public void setLoationId(Long loationId) {
		this.loationId = loationId;
	}
	public String getLocationName() {
		return locationName;
	}
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}
	public String getLocationDescription() {
		return locationDescription;
	}
	public void setLocationDescription(String locationDescription) {
		this.locationDescription = locationDescription;
	}
	
	
	
}

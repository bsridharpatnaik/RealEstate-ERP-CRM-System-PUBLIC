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

import com.ec.application.ReusableClasses.ReusableFields;

@Entity
@Table(name = "Usage_Location")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class UsageLocation extends ReusableFields
{
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
	Long locationId;
	
	@NonNull
	@Column(unique=true,name="location_name")
	String locationName;
	String locationDescription;
	
	public Long getLocationId() {
		return locationId;
	}
	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}
	public Long getLoationId() {
		return locationId;
	}
	public void setLoationId(Long loationId) {
		this.locationId = loationId;
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

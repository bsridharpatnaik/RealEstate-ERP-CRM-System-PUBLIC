package com.ec.application.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.ec.application.Deserializers.ToTitleCaseDeserializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Entity
@Table(name = "Usage_Location")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Proxy(lazy = false)
public class UsageLocation extends ReusableFields
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	Long locationId;

	@NonNull
	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	@Column(name = "location_name")
	String locationName;
	@JsonDeserialize(using = ToSentenceCaseDeserializer.class)
	String locationDescription;

	@ManyToOne(fetch = FetchType.EAGER, cascade =
	{ CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "typeId", nullable = true)
	@NotFound(action = NotFoundAction.IGNORE)
	BuildingType buildingType;

	public static long getSerialversionuid()
	{
		return serialVersionUID;
	}

	public BuildingType getBuildingType()
	{
		return buildingType;
	}

	public void setBuildingType(BuildingType buildingType)
	{
		this.buildingType = buildingType;
	}

	public Long getLocationId()
	{
		return locationId;
	}

	public void setLocationId(Long locationId)
	{
		this.locationId = locationId;
	}

	public Long getLoationId()
	{
		return locationId;
	}

	public void setLoationId(Long loationId)
	{
		this.locationId = loationId;
	}

	public String getLocationName()
	{
		return locationName;
	}

	public void setLocationName(String locationName)
	{
		this.locationName = locationName;
	}

	public String getLocationDescription()
	{
		return locationDescription;
	}

	public void setLocationDescription(String locationDescription)
	{
		this.locationDescription = locationDescription;
	}

}

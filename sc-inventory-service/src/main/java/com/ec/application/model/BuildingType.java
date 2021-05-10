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

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.ec.application.Deserializers.ToTitleCaseDeserializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Entity
@Table(name = "building_type")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class BuildingType extends ReusableFields
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	Long typeId;

	@NonNull
	@JsonDeserialize(using = ToTitleCaseDeserializer.class)
	@Column(name = "building_type")
	String typeName;

	@JsonDeserialize(using = ToSentenceCaseDeserializer.class)
	String typeDescription;

	public Long getTypeId()
	{
		return typeId;
	}

	public void setTypeId(Long typeId)
	{
		this.typeId = typeId;
	}

	public String getTypeName()
	{
		return typeName;
	}

	public void setTypeName(String typeName)
	{
		this.typeName = typeName;
	}

	public String getTypeDescription()
	{
		return typeDescription;
	}

	public void setTypeDescription(String typeDescription)
	{
		this.typeDescription = typeDescription;
	}

	public static long getSerialversionuid()
	{
		return serialVersionUID;
	}

}

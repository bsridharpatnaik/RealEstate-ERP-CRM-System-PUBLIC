package com.ec.crm.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Where;

import com.ec.crm.ReusableClasses.ReusableFields;

@Entity
@Table(name = "property_name")
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class PropertyName extends ReusableFields
{
	public PropertyName()
	{
		super();
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "property_name_id", updatable = false, nullable = false)
	Long propertyNameId;

	private String name;

	public PropertyName(String name2)
	{
		this.name = name2;
	}

	public static long getSerialversionuid()
	{
		return serialVersionUID;
	}

	public Long getPropertyNameId()
	{
		return propertyNameId;
	}

	public void setPropertyNameId(Long propertyNameId)
	{
		this.propertyNameId = propertyNameId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

}

package com.ec.crm.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.ec.crm.ReusableClasses.ReusableFields;

@Entity
@Table(name = "property_name")
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Audited
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

	@NotAudited
	@Formula("(select case when count(*)>0 then true else false end from   customer_deal_structure cds where cds.property_name_id=property_name_id and is_deleted=0)")
	Boolean isBooked;

	public Boolean getIsBooked()
	{
		return isBooked;
	}

	public void setIsBooked(Boolean isBooked)
	{
		this.isBooked = isBooked;
	}

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

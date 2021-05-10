package com.ec.crm.Model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.crm.ReusableClasses.ReusableFields;

import lombok.Data;

@Entity
@Data
@Table(name = "property_type")
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Audited
public class PropertyType extends ReusableFields implements Serializable
{

	public PropertyType(String propertyType, Set<PropertyName> propertyNames)
	{
		super();
		this.propertyType = propertyType;
		this.propertyNames = propertyNames;
	}

	public PropertyType()
	{
		super();
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "property_type_id", updatable = false, nullable = false)
	Long propertyTypeId;

	private String propertyType;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(name = "property_type_name", joinColumns =
	{ @JoinColumn(name = "property_type_id", referencedColumnName = "property_type_id") }, inverseJoinColumns =
	{ @JoinColumn(name = "property_name_id", referencedColumnName = "property_name_id") })

	private Set<PropertyName> propertyNames = new HashSet<>();

	Long totalProperties;
	Long bookedProperties;

}

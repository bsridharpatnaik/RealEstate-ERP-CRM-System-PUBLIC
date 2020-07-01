package com.ec.crm.Model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.lang.NonNull;

import com.ec.ReusableClasses.ReusableFields;
@Entity
@Table(name = "property_type")
public class PropertyType extends ReusableFields implements Serializable{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "propertytype_id", updatable = false, nullable = false)
	Long propertyTypeId;
	
	@NotBlank(message = "Name is mandatory")
	String name;
	
	@NotBlank(message = "Description is mandatory")
	String description;
	public Long getPropertyTypeId() {
		return propertyTypeId;
	}
	public void setPropertyTypeId(Long propertyTypeId) {
		this.propertyTypeId = propertyTypeId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
}

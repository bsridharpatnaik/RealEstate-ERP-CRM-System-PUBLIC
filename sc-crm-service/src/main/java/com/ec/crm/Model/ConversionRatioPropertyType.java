package com.ec.crm.Model;

import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Subselect("select * from convertion_ratio_prop_type")
@Immutable
@Data
public class ConversionRatioPropertyType
{
	@Id
	@Column(name="propertyType")
	String propertyType;
	Long totalcount;
	Long convertedcount;
	Double ratio;
	
}

package com.ec.crm.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import lombok.Data;

@Entity
@Subselect("select * from convertion_ratio")
@Immutable
@Data
public class ConversionRatio 
{
	@Id
	@Column(name="assigneename")
	String asigneeName;
	
	Long totalcount;
	Long convertedcount;
	Double ratio;
}

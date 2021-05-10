package com.ec.crm.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import lombok.Data;

@Entity
@Subselect("select * from convertion_ratio")
@Immutable
@Data
@NoArgsConstructor
public class ConversionRatio 
{
	@Id
	@Column(name="user_id")
	Long userId;
	
	@Column(name="asigneename")
	String asigneeName;
	
	Long totalcount;
	Long convertedcount;
	Double ratio;

	public ConversionRatio(ConversionRatioPropertyType ct) {
		this.userId = null;
		this.asigneeName=ct.getPropertyType();
		this.totalcount=ct.getTotalcount();
		this.convertedcount = ct.getConvertedcount();
		this.ratio = ct.getRatio();
	}
}

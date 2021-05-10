package com.ec.crm.Model;

import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Subselect("select * from stangnant_details_proptype")
@Immutable
@Data
public class StagnantStatsPropertyType
{
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name="property_type")
	String property_type;
	
	@Column(name="lessthan10days")
	Integer lessThan10Days;
	
	@Column(name="tento20days")
	Integer tenTo20Days;
	
	@Column(name="twentyto30days")
	Integer twentyTo30Days;
	
	@Column(name="greaterthan30days")
	Integer greaterThan30Days;
}

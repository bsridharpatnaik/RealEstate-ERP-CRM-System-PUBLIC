package com.ec.crm.Model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.envers.Audited;

import lombok.Data;

@Entity
@Subselect("select * from stangnantdetails")
@Immutable
@Data
@NoArgsConstructor
public class StagnantStats 
{
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name="assigneename")
	String assigneename;
	
	@Column(name="lessthan10days")
	Integer lessThan10Days;
	
	@Column(name="tento20days")
	Integer tenTo20Days;
	
	@Column(name="twentyto30days")
	Integer twentyTo30Days;
	
	@Column(name="greaterthan30days")
	Integer greaterThan30Days;

	public StagnantStats(StagnantStatsPropertyType st) {
		this.assigneename = st.getProperty_type();
		this.greaterThan30Days = st.getGreaterThan30Days();
		this.lessThan10Days = st.getLessThan10Days();
		this.tenTo20Days = st.getTenTo20Days();
		this.twentyTo30Days = st.getTwentyTo30Days();
	}
}

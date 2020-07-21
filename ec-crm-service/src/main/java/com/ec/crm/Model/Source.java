package com.ec.crm.Model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.crm.ReusableClasses.ReusableFields;

@Entity
@Table(name = "source")
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Audited
public class Source extends ReusableFields implements Serializable{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "source_id", updatable = false, nullable = false)
	Long sourceId;
	
	@NotBlank(message = "Name is mandatory")
	@Column(name="source_name")
	String sourceName;
	
	@NotBlank(message = "Description is mandatory")
	@Column(name="source_description")
	String sourceDescription;
	
	public Long getSourceId() {
		return sourceId;
	}
	public void setSourceId(Long sourceId) {
		this.sourceId = sourceId;
	}
	public String getSourceName() {
		return sourceName;
	}
	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}
	public String getSourceDescription() {
		return sourceDescription;
	}
	public void setSourceDescription(String sourceDescription) {
		this.sourceDescription = sourceDescription;
	}
	
	
}

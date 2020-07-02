package com.ec.crm.Model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.ec.ReusableClasses.ReusableFields;

@Entity
@Table(name = "source")
public class Source extends ReusableFields implements Serializable{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "source_id", updatable = false, nullable = false)
	Long sourceId;
	String source_name;
	String source_description;
	public Long getSourceId() {
		return sourceId;
	}
	public void setSourceId(Long sourceId) {
		this.sourceId = sourceId;
	}
	public String getSource_name() {
		return source_name;
	}
	public void setSource_name(String source_name) {
		this.source_name = source_name;
	}
	public String getSource_description() {
		return source_description;
	}
	public void setSource_description(String source_description) {
		this.source_description = source_description;
	}
	
}

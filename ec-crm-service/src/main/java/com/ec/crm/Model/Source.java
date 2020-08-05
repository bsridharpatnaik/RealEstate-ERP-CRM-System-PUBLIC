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

import com.ec.crm.Data.AssigneeDAO;
import com.ec.crm.ReusableClasses.ReusableFields;

import lombok.Data;

@Entity
@Table(name = "source")
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Audited
@Data
public class Source extends ReusableFields implements Serializable{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "source_id", updatable = false, nullable = false)
	Long sourceId;
	
	@NotBlank(message = "Name is mandatory")
	@Column(name="source_name")
	String sourceName;
	
	@Column(name="source_description")
	String sourceDescription;
}

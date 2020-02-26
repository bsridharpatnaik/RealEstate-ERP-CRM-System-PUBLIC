package com.ec.application.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.application.SoftDelete.SoftDeletableEntity;

@Entity
@Table(name = "measurement_unit")
@Audited
@Where(clause = SoftDeletableEntity.SOFT_DELETED_CLAUSE)
public class MeasurementUnit extends SoftDeletableEntity
{

	@NotNull
	@Size(max = 50)
	@Id
	@Column(length = 50,unique=true)
	String name;
}

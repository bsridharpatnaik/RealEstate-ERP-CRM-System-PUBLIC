package com.ec.application.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.ec.application.SoftDelete.SoftDeletableEntity;


@Entity
@Table(name = "role")
public class Role extends SoftDeletableEntity {

	private static final long serialVersionUID = 1L;

	@NotNull
	@Size(max = 50)
	@Id
	@Column(length = 50,unique=true)
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	
	
}
package com.ec.application.model;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "project_constants")
@Data
public class ProjectConstants implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.AUTO)
	Long id;

	@Column(name = "key_name")
	String key;

	@Column(name = "key_value")
	Integer value;
}

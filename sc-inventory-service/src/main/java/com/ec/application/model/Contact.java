package com.ec.application.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ec.application.ReusableClasses.ReusableFields;

import lombok.Data;

@Entity
@Table(name = "contacts")
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Data
@Audited
@EntityListeners(AuditingEntityListener.class)
public class Contact extends ContactMappedSuperClass
{

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "contactId", updatable = false, nullable = false)
	Long contactId;
}

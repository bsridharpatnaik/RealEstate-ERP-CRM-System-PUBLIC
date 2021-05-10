package com.ec.application.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.application.ReusableClasses.ReusableFields;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contacts")
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE + " AND contacttype='contractor'")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class Contractor extends ContactMappedSuperClass
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "contactId", updatable = false, nullable = false)
	Long contactId;
}

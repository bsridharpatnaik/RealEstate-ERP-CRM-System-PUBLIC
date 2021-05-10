package com.ec.application.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Entity
@Table(name = "boq_inventory")
@Audited
@Data
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class BOQInventoryMapping extends ReusableFields
{
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	Long entryId;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "locationId", nullable = true)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	UsageLocation location;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "productId", nullable = false)
	@JsonIgnoreProperties(
	{ "hibernateLazyInitializer", "handler" })
	Product product;

	@ManyToOne(fetch = FetchType.EAGER, cascade =
	{ CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "typeId", nullable = true)
	@NotFound(action = NotFoundAction.IGNORE)
	BuildingType buildingType;

	Double quantity;

	@Column(nullable = true)
	@Enumerated(EnumType.STRING)
	BOQLocationTypeEnum boqType;
}

package com.ec.crm.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.ec.crm.ReusableClasses.ReusableFields;

@Entity
@Table(name = "property_name")
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Audited
public class PropertyName extends ReusableFields
{
	public PropertyName() {
		super();
	}

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "property_name_id", updatable = false, nullable = false)
	Long propertyNameId;

	private String name;

	@NotAudited
	@Formula("(select case when count(*)>0 then true else false end from   customer_deal_structure cds where cds.property_name_id=property_name_id and cds.is_deleted=0)")
	Boolean isBooked;

	String unitDetails;
	String phase;
	String plotSize;
	String superBuiltupArea;

	@NotAudited
	@Formula("(select cl.Lead_id from property_name pn INNER JOIN customer_deal_structure cds on cds.property_name_id = pn.property_name_id " +
			"INNER JOIN customer_lead cl on cl.lead_id=cds.lead_id where pn.property_name_id=property_name_id AND " +
			"cds.is_deleted=false AND cl.is_deleted=false)")
	Long customerId;

	@NotAudited
	@Formula("(select cl.name from property_name pn INNER JOIN customer_deal_structure cds on cds.property_name_id = pn.property_name_id " +
			"INNER JOIN customer_lead cl on cl.lead_id=cds.lead_id where pn.property_name_id=property_name_id AND " +
			"cds.is_deleted=false AND cl.is_deleted=false)")
	String customerName;

	public Long getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public Boolean getIsBooked()
	{
		return isBooked;
	}

	public void setIsBooked(Boolean isBooked)
	{
		this.isBooked = isBooked;
	}

	public PropertyName(String name2)
	{
		this.name = name2;
	}

	public static long getSerialversionuid()
	{
		return serialVersionUID;
	}

	public Long getPropertyNameId()
	{
		return propertyNameId;
	}

	public void setPropertyNameId(Long propertyNameId)
	{
		this.propertyNameId = propertyNameId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

	public Boolean getBooked() {
		return isBooked;
	}

	public void setBooked(Boolean booked) {
		isBooked = booked;
	}

	public String getUnitDetails() {
		return unitDetails;
	}

	public void setUnitDetails(String unitDetails) {
		this.unitDetails = unitDetails;
	}

	public String getPhase() {
		return phase;
	}

	public void setPhase(String phase) {
		this.phase = phase;
	}

	public String getPlotSize() {
		return plotSize;
	}

	public void setPlotSize(String plotSize) {
		this.plotSize = plotSize;
	}

	public String getSuperBuiltupArea() {
		return superBuiltupArea;
	}

	public void setSuperBuiltupArea(String superBuiltupArea) {
		this.superBuiltupArea = superBuiltupArea;
	}
}

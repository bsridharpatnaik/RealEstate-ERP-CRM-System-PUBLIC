package com.ec.application.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import lombok.Data;

@Entity
@Subselect("select * from stock_verification")
@Immutable
@Data
public class StockValidation 
{
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name="inventory")
	String inventory;
	
	@Column(name="total_inward")
	Double totalInward;
	
	@Column(name="total_outward")
	Double totalOutward;
	
	@Column(name="current_stock")
	Double currentStock;
	
	@Column(name="diff_in_Stock")
	Double diffInStock;
}

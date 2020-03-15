package com.ec.application.model.Stock;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.application.SoftDelete.SoftDeletableEntity;
import com.ec.application.model.BasicEntities.Product;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.NonNull;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Audited
@Where(clause = SoftDeletableEntity.SOFT_DELETED_CLAUSE)
public class Stock extends SoftDeletableEntity
{
	
	private static final long serialVersionUID = 1L;
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
	Long stockId;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="productId",nullable=false	)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	@NotFound(action=NotFoundAction.IGNORE)
	@NonNull
	Product product;
	
	@ManyToOne(fetch=FetchType.LAZY,cascade = CascadeType.ALL)
	@JoinColumn(name="warehouseName",nullable=false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	@NotFound(action=NotFoundAction.IGNORE)
	@NonNull
	Warehouse warehouse;
	
	@NonNull
	Float quantityInHand;

	public Stock(@NonNull Product product, @NonNull Warehouse warehouse, @NonNull Float quantityInHand) {
		super();
		this.product = product;
		this.warehouse = warehouse;
		this.quantityInHand = quantityInHand;
	}


	public Long getStockId() {
		return stockId;
	}


	public void setStockId(Long stockId) {
		this.stockId = stockId;
	}


	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public Warehouse getWarehouse() {
		return warehouse;
	}

	public void setWarehouse(Warehouse warehouse) {
		this.warehouse = warehouse;
	}

	public Float getQuantityInHand() {
		return quantityInHand;
	}

	public void setQuantityInHand(Float quantityInHand) {
		this.quantityInHand = quantityInHand;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}


	public Stock() {
		super();
	}
	
	
	
}

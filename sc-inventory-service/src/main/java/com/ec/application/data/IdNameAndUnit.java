package com.ec.application.data;

import com.ec.application.constants.BatchMode;
import lombok.Data;

@Data
public class IdNameAndUnit
{

	Long ProductId;
	String productName;
	String measurementUnit;
	String productCode;
	Boolean isManagedInventory;
	/** Batch tracking mode — null means NONE (no batch tracking). */
	BatchMode batchMode;
	/** Deprecated — kept for backward compat. Derived from batchMode. */
	Boolean isExpirable;


	public IdNameAndUnit(Long productId, String productName, String measurementUnit, String productCode, Boolean isManagedInventory) {
		super();
		this.ProductId = productId;
		this.productName = productName;
		this.measurementUnit = measurementUnit;
		this.productCode = productCode;
		this.isManagedInventory = isManagedInventory;
	}

	/** Legacy constructor — used by old JPQL queries that pass Boolean isExpirable. */
	public IdNameAndUnit(Long productId, String productName, String measurementUnit, String productCode, Boolean isManagedInventory, Boolean isExpirable) {
		super();
		this.ProductId = productId;
		this.productName = productName;
		this.measurementUnit = measurementUnit;
		this.productCode = productCode;
		this.isManagedInventory = isManagedInventory;
		this.isExpirable = isExpirable;
		this.batchMode = Boolean.TRUE.equals(isExpirable) ? BatchMode.BATCH_WITH_EXPIRY : BatchMode.NONE;
	}

	/** New constructor — used by updated JPQL queries that pass BatchMode. */
	public IdNameAndUnit(Long productId, String productName, String measurementUnit, String productCode, Boolean isManagedInventory, BatchMode batchMode) {
		super();
		this.ProductId = productId;
		this.productName = productName;
		this.measurementUnit = measurementUnit;
		this.productCode = productCode;
		this.isManagedInventory = isManagedInventory;
		this.batchMode = batchMode != null ? batchMode : BatchMode.NONE;
		this.isExpirable = this.batchMode == BatchMode.BATCH_WITH_EXPIRY;
	}
}

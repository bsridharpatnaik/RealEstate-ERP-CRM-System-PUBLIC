package com.ec.application.model;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.ec.application.data.StockInformationExportDAO;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "stock_history")
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@NoArgsConstructor
@AllArgsConstructor
public class StockHistory extends ReusableFields
{
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
	Long stockHistoryId;
	String uuid;
	Long productId;
	String inventory;
	String category;
	String totalStock;
	String warehouse;
	@JsonSerialize(using=DoubleTwoDigitDecimalSerializer.class)
	Double warehouseStock;
	String measurementUnit;
	
	public StockHistory(StockInformationExportDAO dataForInsert, UUID uuid) 
	{
		this.uuid=uuid.toString();
		this.productId=dataForInsert.getProductId();
		this.inventory=dataForInsert.getInventory();
		this.category=dataForInsert.getCategory();
		this.totalStock=dataForInsert.getTotalStock();
		this.warehouse=dataForInsert.getWarehouse();
		this.warehouseStock=dataForInsert.getWarehouseStock();
		this.measurementUnit=dataForInsert.getMeasurementUnit();
	}
}

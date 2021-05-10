package com.ec.application.data;

import java.util.Date;
import java.util.Set;

import com.ec.application.Deserializers.OutwardExportSerializer;
import com.ec.application.model.InwardOutwardList;
import com.ec.application.model.OutwardInventory;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Data;

@JsonSerialize(using = OutwardExportSerializer.class)
@Data
public class OutwardInventoryExportDAO
{

	Long outwardid;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date date;
	String warehouse;
	String contractor;
	String buildingUnit;
	String finalLocation;
	String slipNo;
	String purpose;
	String additionalInfo;
	Set<InwardOutwardList> inwardOutwardList;

	public OutwardInventoryExportDAO(OutwardInventory oi)
	{
		super();
		this.outwardid = oi.getOutwardid();
		this.date = oi.getDate();
		this.purpose = oi.getPurpose();
		this.slipNo = oi.getPurpose();
		this.inwardOutwardList = oi.getInwardOutwardList();
		this.warehouse = oi.getWarehouse().getWarehouseName();
		this.finalLocation = oi.getUsageArea().getUsageAreaName();
		this.contractor = oi.getContractor().getName();
		this.buildingUnit = oi.getUsageLocation().getLocationName();
		this.additionalInfo = oi.getAdditionalInfo();
	}

}

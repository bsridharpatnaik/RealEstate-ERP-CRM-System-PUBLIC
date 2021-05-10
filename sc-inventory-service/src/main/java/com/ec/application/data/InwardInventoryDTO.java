package com.ec.application.data;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.ec.application.model.FileInformation;
import com.ec.application.model.Supplier;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class InwardInventoryDTO
{
	Long inwardid;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date date;

	String vehicleNo;

	String supplierSlipNo;

	String ourSlipNo;

	Set<InwardOutwardListDTO> inwardOutwardList = new HashSet<>();;

	WarehouseDTO warehouse;

	Supplier supplier;

	String additionalInfo;

	Boolean invoiceReceived;

	Set<FileInformation> fileInformations = new HashSet<>();

	String purchaseOrder;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	Date purchaseOrderdate;

	Set<RejectInwardListDTO> rejectInwardList = new HashSet<>();
	boolean isDeleted;
	String createdBy;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
	Date creationDate;
	String lastModifiedBy;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
	protected Date lastModifiedDate;
}

package com.ec.application.data;

import java.util.Date;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.model.InwardOutwardList;
import com.ec.application.model.OutwardInventory;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonPropertyOrder(
        {"Outward ID", "Outward Date", "Slip No", "Category", "Inventory", "Measurement Unit", "Opening Stock", "Quantity",
                "Closing Stock", "Warehouse", "Contractor", "Building Unit", "Final Location"})
public class OutwardInventoryExportDAO2 {
    @JsonProperty("Outward ID")
    Long outwardid;

    @JsonProperty("Outward Date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    Date date;

    @JsonProperty("Purpose")
    String purpose;

    @JsonProperty("Slip No")
    String slipNo;

    @JsonProperty("Inventory")
    String product;

    @JsonProperty("Measurement Unit")
    String measurementUnit;

    @JsonProperty("Category")
    String category;

    @JsonProperty("Opening Stock")
    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    Double openingStock;

    @JsonProperty("Quantity")
    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    Double quantity;

    @JsonProperty("Closing Stock")
    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    Double closingStock;

    @JsonProperty("Warehouse")
    String warehouse;

    @JsonProperty("Contractor")
    String contractor;

    @JsonProperty("Building Unit")
    String usageArea;

    @JsonProperty("Final Location")
    String usageLocation;

    @JsonProperty("Comment")
    String comment;

    public OutwardInventoryExportDAO2(OutwardInventory oi, InwardOutwardList iol) {
        super();
        this.outwardid = oi.getOutwardid();
        this.date = oi.getDate();
        this.purpose = oi.getPurpose() == null ? "" : oi.getPurpose();
        this.slipNo = oi.getSlipNo() == null ? "" : oi.getSlipNo();
        this.product = iol.getProduct().getProductName();
        this.quantity = iol.getQuantity();
        this.closingStock = iol.getClosingStock();
        this.measurementUnit = iol.getProduct().getMeasurementUnit();
        this.warehouse = oi.getWarehouse().getWarehouseName();
        this.usageArea = oi.getUsageArea().getUsageAreaName();
        this.contractor = oi.getContractor().getName();
        this.usageLocation = oi.getUsageLocation().getLocationName();
        this.openingStock = iol.getQuantity() + iol.getClosingStock();
        this.category = iol.getProduct().getCategory().getCategoryName();
        this.comment = oi.getAdditionalInfo()==null?"": oi.getAdditionalInfo();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getOpeningStock() {
        return openingStock;
    }

    public void setOpeningStock(Double openingStock) {
        this.openingStock = openingStock;
    }

    public Long getOutwardid() {
        return outwardid;
    }

    public void setOutwardid(Long outwardid) {
        this.outwardid = outwardid;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getSlipNo() {
        return slipNo;
    }

    public void setSlipNo(String slipNo) {
        this.slipNo = slipNo;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getMeasurementUnit() {
        return measurementUnit;
    }

    public void setMeasurementUnit(String measurementUnit) {
        this.measurementUnit = measurementUnit;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getClosingStock() {
        return closingStock;
    }

    public void setClosingStock(Double closingStock) {
        this.closingStock = closingStock;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public String getUsageArea() {
        return usageArea;
    }

    public void setUsageArea(String usageArea) {
        this.usageArea = usageArea;
    }

    public String getContractor() {
        return contractor;
    }

    public void setContractor(String contractor) {
        this.contractor = contractor;
    }

    public String getUsageLocation() {
        return usageLocation;
    }

    public void setUsageLocation(String usageLocation) {
        this.usageLocation = usageLocation;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}

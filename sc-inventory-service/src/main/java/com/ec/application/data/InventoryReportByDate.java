package com.ec.application.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonPropertyOrder(
        { "Year-Month", "Category", "Item Name", "UOM", "Warehouse", "Opening Stock",
                "Inward Qty", "Transfer-In Qty", "Outward Qty", "Transfer-Out Qty", "Lost-Damaged Qty", "Excess-Found Qty", "Closing Stock"})
public interface InventoryReportByDate {

    @JsonIgnore
    Long getId();

    @JsonProperty("Year-Month")
    String getMonth();

    @JsonProperty("Item Name")
    String getProduct_name();

    @JsonProperty("UOM")
    String getMeasurementunit();

    @JsonProperty("Category")
    String getCategory_name();

    @JsonProperty("Warehouse")
    String getWarehousename();

    @JsonProperty("Opening Stock")
    @JsonSerialize(using = com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    Double getOpening_stock();

    @JsonProperty("Inward Qty")
    @JsonSerialize(using = com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    Double getTotal_inward();

    @JsonProperty("Transfer-In Qty")
    @JsonSerialize(using = com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    Double getTotal_transfer_in();

    @JsonProperty("Outward Qty")
    @JsonSerialize(using = com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    Double getTotal_outward();

    @JsonProperty("Transfer-Out Qty")
    @JsonSerialize(using = com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    Double getTotal_transfer_out();

    @JsonProperty("Lost-Damaged Qty")
    @JsonSerialize(using = com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    Double getTotal_lost_damaged();

    @JsonProperty("Excess-Found Qty")
    @JsonSerialize(using = com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    Double getTotal_excess_found();

    @JsonProperty("Closing Stock")
    @JsonSerialize(using = com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    Double getClosing_stock();
}
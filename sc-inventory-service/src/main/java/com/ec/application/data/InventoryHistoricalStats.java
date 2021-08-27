package com.ec.application.data;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class InventoryHistoricalStats {
    String productName;
    String measurementUnit;
    Double currentStock;
    @JsonSerialize(using= com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    TimelyProductStatsForDashboard inward;
    @JsonSerialize(using= com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer.class)
    TimelyProductStatsForDashboard outward;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getMeasurementUnit() {
        return measurementUnit;
    }

    public void setMeasurementUnit(String measurementUnit) {
        this.measurementUnit = measurementUnit;
    }

    public Double getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Double currentStock) {
        this.currentStock = currentStock==null?0:currentStock;
    }

    public TimelyProductStatsForDashboard getInward() {
        return inward;
    }

    public void setInward(TimelyProductStatsForDashboard inward) {
        this.inward = inward;
    }

    public TimelyProductStatsForDashboard getOutward() {
        return outward;
    }

    public void setOutward(TimelyProductStatsForDashboard outward) {
        this.outward = outward;
    }
}

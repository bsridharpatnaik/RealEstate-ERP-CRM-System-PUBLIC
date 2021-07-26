package com.ec.application.data;

import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.ec.application.Deserializers.ToTitleCaseDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class ProductCreateData {
    Long categoryId;
    @NonNull
    @JsonDeserialize(using = ToTitleCaseDeserializer.class)
    String productName;
    @JsonDeserialize(using = ToSentenceCaseDeserializer.class)
    String productDescription;
    @NonNull
    Double reorderQuantity;
    String measurementUnit;
    Boolean showOnDashboard;

    public Boolean getShowOnDashboard() {
        return showOnDashboard;
    }

    public void setShowOnDashboard(Boolean showOnDashboard) {
        this.showOnDashboard = showOnDashboard;
    }

    public Double getReorderQuantity() {
        return reorderQuantity;
    }

    public void setReorderQuantity(Double reorderQuantity) {
        this.reorderQuantity = reorderQuantity;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public String getMeasurementUnit() {
        return measurementUnit;
    }

    public void setMeasurementUnit(String measurementUnit) {
        this.measurementUnit = measurementUnit;
    }

}

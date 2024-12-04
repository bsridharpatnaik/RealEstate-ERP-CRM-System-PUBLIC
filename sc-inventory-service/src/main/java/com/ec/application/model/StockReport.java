package com.ec.application.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Subselect("SELECT * FROM stock_report")
@Immutable
@Data
@NoArgsConstructor
public class StockReport {
    @Id
    @Column(name = "sr_no")
    @JsonProperty("Sr No")
    private Long srNo;

    @Column(name = "last_inward_date")
    @JsonProperty("Last Inward Date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date lastInwardDate;

    @Column(name = "supplier_name")
    @JsonProperty("Supplier Name")
    private String supplierName;

    @Column(name = "category_name")
    @JsonProperty("Category Name")
    private String categoryName;

    @Column(name = "item_name")
    @JsonProperty("Item Name")
    private String itemName;

    @Column(name = "quantity")
    @JsonProperty("Quantity")
    private Double quantity;

    @Column(name = "measurement_unit")
    @JsonProperty("Measurement Unit")
    private String measurementUnit;

    @Column(name = "aging_period")
    @JsonProperty("Aging Period")
    private String agingPeriod;

    @Column(name = "aging_reason")
    @JsonProperty("Aging Reason")
    private String agingReason;

    @Column(name = "remark")
    @JsonProperty("Remark")
    private String remark;
}
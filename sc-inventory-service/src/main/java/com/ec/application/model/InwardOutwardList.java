package com.ec.application.model;

import javax.persistence.*;

import lombok.Data;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;

@Entity
@Table(name = "inward_outward_entries")
@Audited
@Data
//@JsonSerialize(using = InwardOutwardListClosingStockSerializer.class)
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class InwardOutwardList extends ReusableFields {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long entryid;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "productId", nullable = false)
    @JsonIgnoreProperties(
            {"hibernateLazyInitializer", "handler"})
    Product product;

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    Double quantity;

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    Double closingStock;

    String lineItemCode;

    @Column(name = "indent_remarks")
    private String indentRemarks;

    @Column(name = "indent_specification")
    private String indentSpecification;

    @Transient
    private String indentId;

    @Column(name = "brand")
    private String brand;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(name = "expiry_date")
    private Date expiryDate;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "warehouse_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    Warehouse warehouse;

    // Cumulative qty rejected against this line so far. Kept separate from `quantity` —
    // `quantity` must stay net-of-returns-only since it feeds the stock ledger view; reject
    // does not change stock (already deducted at outward time), so it must not shrink it.
    @Column(name = "rejected_quantity")
    private Double rejectedQuantity = 0.0;

    @Transient
    public String getIndentId() {
        if (lineItemCode == null || !lineItemCode.contains("/")) {
            return null; // or throw exception if you want strictness
        }
        return lineItemCode.substring(0, lineItemCode.indexOf('/'));
    }
}

package com.ec.application.model;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "purchase_order_line")
@Getter
@Setter
@NoArgsConstructor
@Audited
public class PurchaseOrderLine extends ReusableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id")
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JoinColumn(name = "product_id")
    private Product product;
    private String brand;
    private String grade;
    private String diameter;
    private String size;
    @Column(columnDefinition = "TEXT")
    private String specification;

    private Double quantity;

    /** Billing unit selected at PO creation time (null = same as product's base unit) */
    @Column(name = "billing_unit")
    private String billingUnit;

    /** Quantity in the billing unit (null when billing unit = base unit) */
    @Column(name = "billing_quantity")
    private Double billingQuantity;

    /** Conversion factor captured at PO creation time for audit */
    @Column(name = "billing_conversion_factor")
    private Double billingConversionFactor;

    private Double rate;
    private Double discountPercent;
    private Double gstPercent;
    private Double netRate;
    private Double totalAmount;

    @Column(name = "tolerance_percent")
    private Double tolerancePercent = 0.0;

    /** UUID of the DBFile used as a sample image for this line item. */
    @Column(name = "sample_image_file_uuid")
    private String sampleImageFileId;

    /** Traceability — indent refs */
    @JsonIgnoreProperties("poLine")
    @OneToMany(mappedBy = "poLine", cascade = CascadeType.ALL)
    private List<PurchaseOrderIndentRef> indentRefs = new ArrayList<>();

    /** Quote comparison line linked to this PO line (optional, for traceability) */
    @Column(name = "linked_qc_line_id")
    private Long linkedQcLineId;

    /** Supplier quote line ID linked to this PO line */
    @Column(name = "linked_supplier_quote_line_id")
    private Long linkedSupplierQuoteLineId;

    /** QC ID for display reference */
    @Column(name = "linked_qc_id")
    private String linkedQcId;

    /**
     * Raw image bytes pre-fetched in tenant context by PurchaseOrderService.
     * Used only by PurchaseOrderPdfService — never serialised to JSON.
     */
    @Transient
    @JsonIgnore
    private byte[] sampleImageData;

    @Transient
    @JsonProperty("receivedQuantity")
    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    private Double receivedQuantity;

    @Transient
    @JsonProperty("balanceQuantity")
    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    private Double balanceQuantity;

    /**
     * Indent line item status for this PO line — derived at query time from the linked
     * IndentInventoryList.lineItemStatus. Used by the frontend to decide whether the line
     * can be removed (only removable when status is "PO CREATED").
     * Not persisted — populated by PurchaseOrderService.getPurchaseOrderWithInit().
     */
    @Transient
    @JsonProperty("lineItemStatus")
    private String lineItemStatus;

    /** Effective lead time in days (product override → category fallback). Populated at query time. */
    @Transient
    @JsonProperty("leadTimeDays")
    private Integer leadTimeDays;

    /** Days remaining until lead time expires (negative = overdue). Null when no lead time set. */
    @Transient
    @JsonProperty("daysLeft")
    private Integer daysLeft;

    /** True when daysLeft < 0 and PO is still open. */
    @Transient
    @JsonProperty("isOverdue")
    private Boolean isOverdue;
}

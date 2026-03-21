package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private String specification;

    private Double quantity;
    private Double rate;
    private Double discountPercent;
    private Double gstPercent;
    private Double netRate;
    private Double totalAmount;

    /** Traceability */
    @JsonIgnoreProperties("poLine")
    @OneToMany(mappedBy = "poLine", cascade = CascadeType.ALL)
    private List<PurchaseOrderIndentRef> indentRefs = new ArrayList<>();

    /**
     * Per-line expected delivery date derived from the linked indent line item.
     * Not persisted — populated at query time by PurchaseOrderService.
     * @JsonProperty forces Jackson to include this @Transient field in serialization.
     */
    @Transient
    @JsonProperty("needByDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date needByDate;
}

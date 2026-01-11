package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.HashSet;
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
    @JoinColumn(name = "product_id")
    private Product product;
    private String brand;
    private String grade;
    private String diameter;
    private String specification;

    private Double quantity;
    private Double rate;
    private Double gstPercent;
    private Double netRate;
    private Double totalAmount;

    /** Traceability */
    @OneToMany(mappedBy = "poLine", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("poLine")
    private Set<PurchaseOrderIndentRef> indentRefs = new HashSet<>();
}

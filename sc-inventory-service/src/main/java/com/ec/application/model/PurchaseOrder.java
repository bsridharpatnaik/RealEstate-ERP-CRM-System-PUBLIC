package com.ec.application.model;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.ReusableClasses.ReusableFields;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.*;

import com.ec.application.Deserializers.ActiveIndentInventoryListSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;


@Entity
@Table(name = "purchase_order")
@Getter
@Setter
@NoArgsConstructor
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class PurchaseOrder extends ReusableFields {

    @Id
    @GeneratedValue(generator = "po-id-gen")
    @GenericGenerator(
            name = "po-id-gen",
            strategy = "com.ec.application.IDGenerator.GlobalPurchaseOrderIdGenerator"
    )
    @Column(name = "purchase_order_id", nullable = false, length = 20)
    private String purchaseOrderId;

    @Column(nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "firm_id")
    private Firm firm;

    private String subject;

    @JsonSerialize(using= DoubleTwoDigitDecimalSerializer.class)
    private Double grandTotal;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL)
    private Set<PurchaseOrderLine> lines = new HashSet<>();
}

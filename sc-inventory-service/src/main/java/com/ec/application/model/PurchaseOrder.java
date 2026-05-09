package com.ec.application.model;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import com.ec.application.Deserializers.ActiveIndentInventoryListSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.lang.NonNull;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;


@Entity
@Table(name = "purchase_order")
@Getter
@Setter
@NoArgsConstructor
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Audited
public class PurchaseOrder extends ReusableFields {

    @Id
    @GeneratedValue(generator = "po-id-gen")
    @GenericGenerator(
            name = "po-id-gen",
            strategy = "com.ec.application.IDGenerator.GlobalPurchaseOrderIdGenerator"
    )
    @Column(name = "purchase_order_id", nullable = false, length = 20)
    private String purchaseOrderId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(name = "po_date", nullable = false)
    @NonNull
    Date poDate;

    @Column(nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "firm_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Firm firm;

    private String subject;

    @JsonSerialize(using= DoubleTwoDigitDecimalSerializer.class)
    private Double grandTotal;

    @JsonSerialize(using= DoubleTwoDigitDecimalSerializer.class)
    private Double freightCharges;

    @JsonSerialize(using= DoubleTwoDigitDecimalSerializer.class)
    private Double freightGstPercent;

    @JsonSerialize(using= DoubleTwoDigitDecimalSerializer.class)
    private Double totalFreightCharges;

    private String shortCloseReason;

    @Lob
    @Column(name="notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("purchaseOrder")
    @OrderBy("id ASC")
    private Set<PurchaseOrderLine> lines = new HashSet<>();

    @OneToMany(
            mappedBy = "purchaseOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @JsonIgnoreProperties("purchaseOrder")
    @OrderBy("id ASC")
    private List<PurchaseOrderCustomCharge> customCharges = new ArrayList<>();

    @Column(name = "is_special_po", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean specialPo = false;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "po_fileinformation", joinColumns =
            {@JoinColumn(name = "purchase_order_id", referencedColumnName = "purchase_order_id")},
            inverseJoinColumns = {@JoinColumn(name = "file_information_id", referencedColumnName = "id")})
    Set<FileInformation> fileInformations = new HashSet<>();

    @OneToMany(
            mappedBy = "purchaseOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = false,
            fetch = FetchType.LAZY
    )
    @JsonIgnore   // optional – depends if you want it in API response
    private List<PurchaseOrderStatusHistory> statusHistory = new ArrayList<>();

    @Column(
            name = "last_status_updated_at",
            nullable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP"
    )
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "d MMM yyyy h:mm a")
    private Date lastStatusUpdatedAt;

    @Column(name = "override_phone_number", length = 35)
    private String overridePhoneNumber;

    @Column(name = "override_email", length = 100)
    private String overrideEmail;

    @Column(name = "project_name", length = 100)
    private String projectName;

    /** Nightly-computed priority: CRITICAL, HIGH, MEDIUM, NORMAL (null = no expected date set). */
    @Column(name = "priority", length = 20)
    private String priority;

    /** Days remaining to the earliest open-indent expected date (negative = overdue, null = no expected date). */
    @Column(name = "days_to_deadline")
    private Integer daysToDeadline;

    /** Earliest needByDate across all linked open indent line items — populated at query time, not persisted. */
    @Transient
    private java.util.Date needByDate;

    @Transient
    Boolean approvalAllowed;

    @Transient
    Boolean cancellationAllowed;
}

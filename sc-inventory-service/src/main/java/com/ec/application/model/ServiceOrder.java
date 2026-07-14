package com.ec.application.model;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Standalone entity for servicing/AMC work (e.g. "service all ACs in office", "service company cars").
 * Deliberately independent of PurchaseOrder — no inventory products, no indents, no inward/batch tracking.
 */
@Entity
@Table(name = "service_order")
@Getter
@Setter
@NoArgsConstructor
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Audited
public class ServiceOrder extends ReusableFields {

    @Id
    @GeneratedValue(generator = "so-id-gen")
    @GenericGenerator(
            name = "so-id-gen",
            strategy = "com.ec.application.IDGenerator.GlobalServiceOrderIdGenerator"
    )
    @Column(name = "service_order_id", nullable = false, length = 20)
    private String serviceOrderId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(name = "service_date", nullable = false)
    @NonNull
    private Date serviceDate;

    @Column(nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Supplier vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "firm_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Firm firm;

    private String subject;

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    private Double grandTotal;

    @Lob
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "project_name", length = 100)
    private String projectName;

    /** Flat, post-tax discount amount, deducted once from the grand total (same pattern as PO's poDiscount). */
    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    @Column(name = "special_discount")
    private Double specialDiscount;

    /** Optional reminder date for when this asset/vendor is next due for servicing. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(name = "next_service_date")
    private Date nextServiceDate;

    @Column(name = "override_phone_number", length = 35)
    private String overridePhoneNumber;

    @Column(name = "override_email", length = 100)
    private String overrideEmail;

    @OneToMany(mappedBy = "serviceOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("serviceOrder")
    @OrderBy("id ASC")
    private List<ServiceOrderLine> lines = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "service_order_fileinformation", joinColumns =
            {@JoinColumn(name = "service_order_id", referencedColumnName = "service_order_id")},
            inverseJoinColumns = {@JoinColumn(name = "file_information_id", referencedColumnName = "id")})
    private Set<FileInformation> fileInformations = new HashSet<>();

    @Column(
            name = "last_status_updated_at",
            nullable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP"
    )
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "d MMM yyyy h:mm a")
    private Date lastStatusUpdatedAt;
}

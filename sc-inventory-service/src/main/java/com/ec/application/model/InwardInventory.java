package com.ec.application.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "inward_inventory")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Data
@NoArgsConstructor
public class InwardInventory extends ReusableFields implements Cloneable {

    /* =========================================================
       Primary Key
       ========================================================= */

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "inwardid")
    private Long inwardId;

    /* =========================================================
       Core Inward Details
       ========================================================= */

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(nullable = false)
    private Date date;

    @NonNull
    @Column(nullable = false)
    private Boolean invoiceReceived;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean isSampleInward = false;

    private String purchaseOrderNo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date purchaseOrderDate;

    /* =========================================================
       Supplier & Transport Details
       ========================================================= */

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "contactId", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Supplier supplier;

    private String vehicleNo;

    /* =========================================================
       Reference / Document Numbers
       ========================================================= */

    private String supplierSlipNo;
    private String ourSlipNo;
    private String billNo;
    private String challanNo;
    @Column(length = 500)
    private String noChallanBillReason;

    /* =========================================================
       Reference Dates
       ========================================================= */

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(nullable = true)
    private Date challanDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(nullable = true)
    private Date billDate;

    /* =========================================================
       Line Items (Inward / Reject)
       ========================================================= */

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "inwardinventory_entry",
            joinColumns = @JoinColumn(name = "inwardid", referencedColumnName = "inwardid"),
            inverseJoinColumns = @JoinColumn(name = "entryId", referencedColumnName = "entryId")
    )
    private Set<InwardOutwardList> inwardOutwardList = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "rejectInward_entry",
            joinColumns = @JoinColumn(name = "inwardid", referencedColumnName = "inwardid"),
            inverseJoinColumns = @JoinColumn(name = "rejectentryid", referencedColumnName = "rejectentryid")
    )
    private Set<RejectInwardList> rejectInwardList = new HashSet<>();

    /* =========================================================
       Attachments
       ========================================================= */

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "inward_fileinformation",
            joinColumns = @JoinColumn(name = "inwardid", referencedColumnName = "inwardid"),
            inverseJoinColumns = @JoinColumn(name = "id", referencedColumnName = "id")
    )
    private Set<FileInformation> fileInformations = new HashSet<>();

    /* =========================================================
       Miscellaneous
       ========================================================= */

    private String additionalInfo;

    private Boolean createdFromPO;
}

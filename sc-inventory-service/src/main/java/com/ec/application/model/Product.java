package com.ec.application.model;

import javax.persistence.*;
import javax.persistence.EnumType;

import com.ec.application.IDGenerator.ProductCodeGenerator;
import com.ec.application.datasync.MultiTableSyncListener;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import com.ec.application.ReusableClasses.ReusableFields;
import com.ec.application.constants.BatchMode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity(name = "Product")
@Table(name = "Product")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Data
@NoArgsConstructor
@EntityListeners(MultiTableSyncListener.class)
public class Product extends ReusableFields {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long productId;

    @NonNull
    @Column(name = "product_name", unique = true, nullable = false)
    String productName;

    @Column(name = "product_code", unique = true, nullable = false, updatable = false)
    String productCode;

    @Column(name = "productDescription")
    String productDescription;

    @Column(name = "measurementUnit")
    String measurementUnit;

    @Column(name = "reorderQuantity")
    Double reorderQuantity;

    /** Optional lead time in calendar days. Overrides category-level lead time when set. */
    @Column(name = "lead_time_days")
    Integer leadTimeDays;

    @Column(name = "is_managed_inventory", columnDefinition = "boolean default true")
    Boolean isManagedInventory;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "categoryId", nullable = false)
    @JsonIgnoreProperties(
            {"hibernateLazyInitializer", "handler"})
    @NotFound(action = NotFoundAction.IGNORE)
    Category category;

    @Column(name = "show_on_dashboard")
    Boolean showOnDashboard;

    /**
     * Replaced boolean isExpirable.
     * NONE (default) = no batch tracking.
     * BATCH_ONLY     = track brand/lot, expiry optional.
     * BATCH_WITH_EXPIRY = track brand/lot + mandatory expiry, FEFO ordering.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "batch_mode", columnDefinition = "varchar(30) default 'NONE'")
    BatchMode batchMode = BatchMode.NONE;

    /**
     * Optional. When set (and batchMode = BATCH_WITH_EXPIRY), new batch entries default their
     * expiry date to today + this many days instead of leaving the field blank for manual entry.
     * Only applies to brand-new batch rows — never overwrites an already-set expiry date.
     */
    @Column(name = "default_expiry_days")
    Integer defaultExpiryDays;

    /** True when any batch tracking is active (BATCH_ONLY or BATCH_WITH_EXPIRY). */
    public boolean isBatchTracked() {
        return batchMode != null && batchMode != BatchMode.NONE;
    }

    /** True when expiry date is mandatory (BATCH_WITH_EXPIRY only). */
    public boolean requiresExpiry() {
        return batchMode == BatchMode.BATCH_WITH_EXPIRY;
    }

    /**
     * Backward-compat shim — code that still reads isExpirable gets correct value.
     * @deprecated use requiresExpiry() directly.
     */
    @Deprecated
    public Boolean getIsExpirable() {
        return requiresExpiry();
    }

    @PrePersist
    public void assignProductCode() {
        if (this.productCode == null) {
            this.productCode = ProductCodeGenerator.nextProductCode();
        }
    }
}

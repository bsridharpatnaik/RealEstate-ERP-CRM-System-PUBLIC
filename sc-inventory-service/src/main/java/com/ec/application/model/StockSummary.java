package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(
        name = "stock_summary",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"tenantSchema", "productId", "warehouseId"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
public class StockSummary extends ReusableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String tenantSchema;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 50)
    private String productCode;

    @Column(nullable = false, length = 150)
    private String productName;

    /** Denormalised from Product.category — synced alongside product name/code (Step 7). */
    @Column(name = "categoryName", length = 150)
    private String categoryName;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false, length = 150)
    private String warehouseName;

    @Column(nullable = false)
    private Double quantityInHand;

    @Column(name="measurement_unit", length = 20)
    String measurementUnit;

    /**
     * Effective reorder level for this product in this tenant.
     * Populated by the hourly sync job: tenant override if set, otherwise global Product.reorderQuantity.
     * All warehouse rows for the same (tenantSchema, productId) carry the same value.
     */
    @Column(name = "reorder_level")
    private Double reorderLevel;

    /** when sync job ran */
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd MMM yyyy, hh:mm a",
            timezone = "Asia/Kolkata"
    )
    private Date syncedAt;
}

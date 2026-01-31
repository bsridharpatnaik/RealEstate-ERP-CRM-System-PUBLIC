package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
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

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false, length = 150)
    private String warehouseName;

    @Column(nullable = false)
    private Double quantityInHand;

    @Column(name="measurement_unit", length = 20)
    String measurementUnit;

    /** when sync job ran */
    @Temporal(TemporalType.TIMESTAMP)
    private Date syncedAt;
}

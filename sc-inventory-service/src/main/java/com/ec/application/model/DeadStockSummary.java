package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(
        name = "dead_stock_summary",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"tenantSchema", "productId", "warehouseId"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class DeadStockSummary extends ReusableFields {

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

    /** when sync job ran */
    @Temporal(TemporalType.TIMESTAMP)
    private Date syncedAt;
}

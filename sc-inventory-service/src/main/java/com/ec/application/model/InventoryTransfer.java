package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import com.ec.application.datasync.MultiTableSyncListener;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;
import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "inventory_transfer")
@Audited
@Where(clause = ReusableFields.SOFT_DELETED_CLAUSE)
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "items")
public class InventoryTransfer extends ReusableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long transferId;

    // Source tenant
    @Column(name = "source_tenant", nullable = false)
    private String sourceTenant;

    // Target tenant
    @Column(name = "target_tenant", nullable = false)
    private String targetTenant;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(name = "transfer_date", nullable = false)
    private Date transferDate;

    @Column(name = "source_warehouse_id", nullable = false)
    private Long sourceWarehouseId;

    @Column(name = "source_warehouse_name", nullable = false)
    private String sourceWarehouseName;

    @Column(name = "target_warehouse_id", nullable = false)
    private Long targetWarehouseId;

    @Column(name = "target_warehouse_name", nullable = false)
    private String targetWarehouseName;

    @Column(name = "remarks")
    private String remarks;

    @OneToMany(
            mappedBy = "inventoryTransfer",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @JsonManagedReference
    private List<InventoryTransferItem> items = new ArrayList<>();
}
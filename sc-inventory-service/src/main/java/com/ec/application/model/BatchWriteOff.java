package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "batch_write_off")
@Data
@NoArgsConstructor
public class BatchWriteOff extends ReusableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "write_off_id")
    private Long writeOffId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private InventoryBatch batch;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "quantity", nullable = false)
    private Double quantity;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy", timezone = "Asia/Kolkata")
    @Column(name = "write_off_date", nullable = false)
    private Date writeOffDate;

    @Column(name = "written_off_by")
    private String writtenOffBy;
}

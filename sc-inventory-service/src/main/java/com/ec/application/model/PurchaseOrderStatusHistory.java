package com.ec.application.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "po_status_history")
@Data
@Audited
public class PurchaseOrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "indent_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    private String oldStatus;
    private String newStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "d MMM yyyy h:mm a")
    private Date changedAt;

    private String changedBy;

    private String changeMessage;
}
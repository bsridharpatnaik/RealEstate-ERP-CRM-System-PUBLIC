package com.ec.application.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "po_status_history")
@Data
@Audited
public class PurchaseOrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    @JsonIgnore
    private PurchaseOrder purchaseOrder;

    private String oldStatus;
    private String newStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "d MMM yyyy h:mm a")
    private Date changedAt;

    private String changedBy;

    @Column(columnDefinition = "TEXT")
    private String changeMessage;

    @OneToMany(
            mappedBy = "poHistory",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @OrderBy("id ASC")
    private List<PurchaseOrderStatusHistoryRelation> relations = new ArrayList<>();
}
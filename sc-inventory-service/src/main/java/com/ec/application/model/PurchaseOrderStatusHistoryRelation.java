package com.ec.application.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.envers.Audited;

import javax.persistence.*;

@Entity
@Table(
        name = "purchase_order_status_history_relation",
        indexes = {
                @Index(name = "idx_po_hist_rel_hist", columnList = "po_history_id")
        }
)
@Data
@Audited
public class PurchaseOrderStatusHistoryRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "po_history_id", nullable = false)
    @JsonIgnore
    private PurchaseOrderStatusHistory poHistory;

    @Column(name = "relation_type", nullable = false, length = 30)
    private String relationType;
    // HistoryRelationType.PO / INWARD / INDENT

    @Column(name = "tenant", nullable = false, length = 50)
    private String tenant;

    @Column(name = "reference_id", nullable = false, length = 100)
    private String referenceId;
}

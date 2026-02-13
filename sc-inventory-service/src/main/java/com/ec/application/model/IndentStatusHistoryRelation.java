package com.ec.application.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.envers.Audited;

import javax.persistence.*;

@Entity
@Table(
        name = "indent_status_history_relation",
        indexes = {
                @Index(name = "idx_hist_rel_history", columnList = "history_id")
        }
)
@Data
@Audited
public class IndentStatusHistoryRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "history_id", nullable = false)
    @JsonIgnore
    private IndentStatusHistory history;

    @Column(name = "relation_type", nullable = false, length = 30)
    private String relationType;
    // PO, INWARD, INDENT_LINE

    @Column(name = "tenant", nullable = false, length = 50)
    private String tenant;

    @Column(name = "reference_id", nullable = false, length = 100)
    private String referenceId;
}

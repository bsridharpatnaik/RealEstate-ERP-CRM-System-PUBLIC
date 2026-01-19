package com.ec.application.model;

import com.ec.application.constants.InwardActionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(
        name = "inward_sync_failure",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {
                        "tenant_schema",
                        "inward_id",
                        "action_type",
                        "status"
                }
        )
)
@Getter
@Setter
@NoArgsConstructor
public class InwardSyncFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_schema", nullable = false, length = 50)
    private String tenantSchema;

    @Column(name = "inward_id", nullable = false)
    private Long inwardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private InwardActionType actionType;

    /**
     * Comma-separated indent line item codes
     * Example: SMC-14/1,SMC-14/2
     */
    @Column(name = "line_item_codes", nullable = false, length = 2000)
    private String lineItemCodes;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, SUCCESS, FAILED

    @Column(name = "last_error", length = 2000)
    private String lastError;
}

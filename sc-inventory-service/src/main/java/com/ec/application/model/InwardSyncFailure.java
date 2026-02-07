package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
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
public class InwardSyncFailure extends ReusableFields {

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

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, SUCCESS, FAILED

    @Column(name = "last_error", length = 2000)
    private String lastError;
}

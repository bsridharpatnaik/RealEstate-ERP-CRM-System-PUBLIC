package com.ec.application.indentpo;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "indent_reconciliation_task")
@Data
@NoArgsConstructor
public class IndentReconciliationTask {

    @Id
    @GeneratedValue
    private Long id;

    // EXACT schema name, e.g. smartcityv2
    @Column(nullable = false)
    private String tenantSchema;

    @Column(nullable = false)
    private String lineItemCode;

    @Column(nullable = false)
    private String status; // PENDING, IN_PROGRESS, DONE, FAILED

    private int retryCount;

    @Column(length = 1000)
    private String lastError;

    private Date createdAt = new Date();
}



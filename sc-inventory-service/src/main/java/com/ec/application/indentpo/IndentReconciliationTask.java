package com.ec.application.indentpo;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "indent_reconciliation_task")
@Data
public class IndentReconciliationTask {

    @Id
    @GeneratedValue
    private Long id;

    private String tenant;

    private String lineItemCode;

    private String status; // PENDING, DONE, FAILED

    private int retryCount;

    private String lastError;

    private Date createdAt = new Date();
}


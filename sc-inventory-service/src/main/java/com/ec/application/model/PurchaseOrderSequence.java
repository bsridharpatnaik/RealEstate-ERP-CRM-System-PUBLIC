package com.ec.application.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "purchase_order_sequence")
@Getter
@Setter
public class PurchaseOrderSequence {

    @Id
    @Column(name = "tenant_code", length = 10)
    private String tenantCode;

    @Column(name = "last_id", nullable = false)
    private Long lastId;
}

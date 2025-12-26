package com.ec.application.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "indent_sequence")
@Data
public class IndentSequence {

    @Id
    @Column(name = "tenant_code", length = 5)
    private String tenantCode;

    @Column(name = "last_id", nullable = false)
    private Long lastId;
}
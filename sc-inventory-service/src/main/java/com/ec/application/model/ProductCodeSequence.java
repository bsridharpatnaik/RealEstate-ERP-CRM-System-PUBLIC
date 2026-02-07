package com.ec.application.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "product_code_sequence")
@Data
public class ProductCodeSequence {

    @Id
    @Column(name = "year", length = 4)
    private String year;

    @Column(name = "last_number", nullable = false)
    private Long lastNumber;
}
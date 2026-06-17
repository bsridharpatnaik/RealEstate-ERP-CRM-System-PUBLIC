package com.ec.application.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "quote_comparison_sequence")
@Data
public class QuoteComparisonSequence {

    @Id
    @Column(name = "prefix", length = 10)
    private String prefix;

    @Column(name = "last_id", nullable = false)
    private Long lastId;
}

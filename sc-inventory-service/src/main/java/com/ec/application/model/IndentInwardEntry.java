package com.ec.application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Date;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IndentInwardEntry {

    @Column(name = "inward_id", nullable = false, length = 50)
    private Long inwardId;

    @Column(name = "inward_date", nullable = false)
    private Date inwardDate;

    @Column(name = "inward_quantity", nullable = false)
    private Double quantity;
}
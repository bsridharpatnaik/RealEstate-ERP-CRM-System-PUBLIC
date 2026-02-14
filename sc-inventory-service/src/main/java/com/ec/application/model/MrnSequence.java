package com.ec.application.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "mrn_sequence_inward")
@Getter
@Setter
public class MrnSequence {

    @Id
    private Integer id;

    @Column(name = "sequence_value", nullable = false)
    private Long sequenceValue;
}

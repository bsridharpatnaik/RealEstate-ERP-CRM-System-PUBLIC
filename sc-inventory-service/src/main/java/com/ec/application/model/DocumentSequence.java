package com.ec.application.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "document_sequence")
@Getter
@Setter
@NoArgsConstructor
public class DocumentSequence {

    @Id
    @Column(name = "doc_type")
    private String docType; // PO

    private Long currentValue;

    @Version
    private Long version;
}
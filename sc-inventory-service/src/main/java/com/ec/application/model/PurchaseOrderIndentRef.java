package com.ec.application.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "po_indent_ref")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrderIndentRef {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_line_id")
    private PurchaseOrderLine poLine;

    private String tenantSchemaCode;
    private String indentNo;
    private String indentLineItemCode;
}

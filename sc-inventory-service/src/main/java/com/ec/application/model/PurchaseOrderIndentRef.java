package com.ec.application.model;


import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.*;

@Entity
@Table(name = "po_indent_ref")
@Getter
@Setter
@NoArgsConstructor
@Audited
public class PurchaseOrderIndentRef extends ReusableFields {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_line_id")
    @JsonIgnore
    private PurchaseOrderLine poLine;

    private String indentNo;
    private String indentLineItemCode;
}

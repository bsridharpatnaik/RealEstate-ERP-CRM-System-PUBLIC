package com.ec.application.model;


import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Objects;

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

    private String indentNo;
    private String indentLineItemCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_line_id", nullable = false)
    private PurchaseOrderLine poLine;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PurchaseOrderIndentRef)) return false;
        PurchaseOrderIndentRef that = (PurchaseOrderIndentRef) o;
        return Objects.equals(indentLineItemCode, that.indentLineItemCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indentLineItemCode);
    }
}


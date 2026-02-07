package com.ec.application.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Getter
@Setter
@Immutable
@Subselect("SELECT purchase_order_id FROM masterschema.purchase_order WHERE status = 'SHORT CLOSED'")
@Synchronize({
        "masterschema.purchase_order",
})
public class PurchaseOrderShortClosedView {

    // ---------- Identity ----------
    @Id
    @Column(name = "purchase_order_id")
    private String purchaseOrderId;
}

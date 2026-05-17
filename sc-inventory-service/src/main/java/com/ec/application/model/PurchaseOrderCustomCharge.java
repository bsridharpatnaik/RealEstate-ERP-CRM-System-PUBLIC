package com.ec.application.model;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.*;

@Entity
@Table(name = "purchase_order_custom_charge")
@Getter
@Setter
@NoArgsConstructor
@Audited
public class PurchaseOrderCustomCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "lines", "customCharges", "statusHistory", "fileInformations"})
    private PurchaseOrder purchaseOrder;

    @Column(name = "charge_name", nullable = false, length = 100)
    private String chargeName;

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    @Column(name = "charge_amount", nullable = false)
    private Double chargeAmount;

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    @Column(name = "charge_gst_percent")
    private Double chargeGstPercent;

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    @Column(name = "total_charge_amount", nullable = false)
    private Double totalChargeAmount;
}

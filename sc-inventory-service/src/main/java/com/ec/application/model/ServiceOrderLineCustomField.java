package com.ec.application.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.*;

@Entity
@Table(name = "service_order_line_custom_field")
@Getter
@Setter
@NoArgsConstructor
@Audited
public class ServiceOrderLineCustomField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_order_line_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ServiceOrderLine serviceOrderLine;

    /** e.g. "Odometer Reading", "Lift Capacity" — selectable from existing labels or freely typed. */
    @Column(name = "field_label", nullable = false)
    private String fieldLabel;

    @Column(name = "field_value")
    private String fieldValue;
}

package com.ec.application.model;

import com.ec.application.ReusableClasses.ReusableFields;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "service_order_line")
@Getter
@Setter
@NoArgsConstructor
@Audited
public class ServiceOrderLine extends ReusableFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_order_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ServiceOrder serviceOrder;

    /** Free-text description of the service performed — no product/inventory link. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    private Double quantity;
    private Double rate;
    private Double discountPercent;
    private Double gstPercent;
    private Double netRate;
    private Double totalAmount;

    /** e.g. ROUTINE, PREVENTIVE, BREAKDOWN, EMERGENCY, AMC — free text, not a hard enum. */
    @Column(name = "service_type")
    private String serviceType;

    /** Identifies the specific asset serviced, e.g. "AC Unit - 3rd Floor", "Car - KA01AB1234", "DG Set 2". */
    @Column(name = "asset_tag")
    private String assetTag;

    /** Only meaningful once the service is actually performed — set via markComplete(), not at creation. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(name = "warranty_till")
    private Date warrantyTill;

    /** Next service date for this specific line item — set via markComplete(), not at creation. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(name = "next_service_date")
    private Date nextServiceDate;

    /** Any number of scenario-specific key/value pairs the fixed schema doesn't cover,
     *  e.g. "Odometer Reading" -> "45000 km", "Lift Capacity" -> "8 person". */
    @OneToMany(mappedBy = "serviceOrderLine", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("serviceOrderLine")
    @OrderBy("id ASC")
    private List<ServiceOrderLineCustomField> customFields = new ArrayList<>();
}

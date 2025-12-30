package com.ec.application.model;

import com.ec.application.Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.application.ReusableClasses.ReusableFields;
import com.ec.application.datasync.MultiTableSyncListener;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import javax.persistence.*;

@Entity
@Table(name = "dead_stock")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DeadStock {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Long deadStockId;

    String tenantName;
    Long productId;
    String productName;
    String productCode;

    @NonNull
    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    Double quantityInHand;
}

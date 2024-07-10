package com.ec.crm.Model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Entity
@Subselect("select * from payments_page")
@Immutable
@Data
@NoArgsConstructor
public class PaymentsPage {
    @Id
    @Column(name = "id")
    String id;

    @Column(name = "lead_id")
    Long leadId;

    @Column(name = "customerName")
    String customerName;

    @Column(name="dealStructureId")
    Long dealStructureId;

    @Column(name = "paymentDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    Date paymentDate;

    @Column(name = "amount")
    Double amount;

    @Column(name = "isReceived")
    Boolean isReceived;

    @Column(name = "isCustomerPayment")
    Boolean isCustomerPayment;

    @Column(name = "user_name")
    String assignee;

    @Column(name = "user_id")
    Long assigneeId;

    @Column(name = "propertyType")
    String propertyType;

    @Column(name = "propertyName")
    String propertyName;
}

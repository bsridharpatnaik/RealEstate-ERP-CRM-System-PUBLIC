package com.ec.application.model;

import lombok.Data;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "smsdeliverylist")
@Data
public class SMSDeliveryList {
    @Id
    String type;

    @Column(name="numbers")
    String numbers;
}

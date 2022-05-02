package com.ec.crm.Data;

import Deserializers.DoubleTwoDigitDecimalSerializer;
import com.ec.crm.Enums.PaymentReceivedFromEnum;
import com.ec.crm.Enums.PaymentReceivedPaymentModeEnum;
import com.ec.crm.Model.DealStructure;
import com.ec.crm.Model.PaymentReceived;
import com.ec.crm.ReusableClasses.DynamicAuthorizationEnumJsonSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentReceivedDTO {

    Long paymentId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    Date paymentReceivedDate;

    @JsonSerialize(using = DynamicAuthorizationEnumJsonSerializer.class)
    PaymentReceivedFromEnum paymentBy;

    @JsonSerialize(using = DynamicAuthorizationEnumJsonSerializer.class)
    PaymentReceivedPaymentModeEnum paymentMode;

    @JsonSerialize(using = DoubleTwoDigitDecimalSerializer.class)
    Double amount;

    String bankName;

    String referenceNumber;

    Long dealStructureId;

    public PaymentReceivedDTO(PaymentReceived paymentReceived) {
        this.paymentId = paymentReceived.getPaymentId();
        this.dealStructureId = paymentReceived.getDs().getDealId();
        this.paymentReceivedDate = paymentReceived.getPaymentReceivedDate();
        this.paymentBy = paymentReceived.getPaymentBy();
        this.amount = paymentReceived.getAmount();
        this.bankName = paymentReceived.getBankName() == null ? null : paymentReceived.getBankName();
        this.referenceNumber = paymentReceived.getReferenceNumber() == null ? null : paymentReceived.getReferenceNumber();
        this.paymentMode = paymentReceived.getPaymentMode();
    }
}

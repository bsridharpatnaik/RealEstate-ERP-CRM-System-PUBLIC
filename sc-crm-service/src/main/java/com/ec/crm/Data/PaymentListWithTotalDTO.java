package com.ec.crm.Data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentListWithTotalDTO {
    int totalPaymentsCount;
    Double totalPaymentsAmount;
    List<PaymentReceivedDTO> paymentList;

    public PaymentListWithTotalDTO(List<PaymentReceivedDTO> payments) {
        this.totalPaymentsCount = payments.size();
        this.totalPaymentsAmount = payments.stream().filter(e -> e.amount!=null).mapToDouble(PaymentReceivedDTO::getAmount).sum();
        this.paymentList = payments;
    }
}

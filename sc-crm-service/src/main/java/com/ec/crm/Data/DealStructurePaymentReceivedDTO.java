package com.ec.crm.Data;

import com.ec.crm.Model.PaymentReceived;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DealStructurePaymentReceivedDTO {
    PaymentListWithTotalDTO customerPayments;
    PaymentListWithTotalDTO bankPayments;

    public DealStructurePaymentReceivedDTO(List<PaymentReceivedDTO> customerPayments, List<PaymentReceivedDTO> bankPayments) {
        this.customerPayments = new PaymentListWithTotalDTO(customerPayments);
        this.bankPayments = new PaymentListWithTotalDTO(bankPayments);
    }
}

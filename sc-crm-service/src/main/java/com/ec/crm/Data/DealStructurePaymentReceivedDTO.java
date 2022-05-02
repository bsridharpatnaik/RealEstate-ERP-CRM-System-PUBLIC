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
    List<PaymentReceivedDTO> customerPayments;
    List<PaymentReceivedDTO> bankPayments;
}

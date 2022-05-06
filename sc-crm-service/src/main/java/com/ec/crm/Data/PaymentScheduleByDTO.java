package com.ec.crm.Data;

import com.ec.crm.Model.PaymentSchedule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentScheduleByDTO {
    Boolean isMatchingDealAmount;
    ScheduleReturnWithTotalsDTO bank;
    ScheduleReturnWithTotalsDTO customer;

    public PaymentScheduleByDTO(List<PaymentSchedule> bankList, List<PaymentSchedule> customerList, Boolean isMatchingDealAmount) {
        this.bank = new  ScheduleReturnWithTotalsDTO(bankList);
        this.customer = new ScheduleReturnWithTotalsDTO(customerList);
        this.isMatchingDealAmount = isMatchingDealAmount;
    }
}

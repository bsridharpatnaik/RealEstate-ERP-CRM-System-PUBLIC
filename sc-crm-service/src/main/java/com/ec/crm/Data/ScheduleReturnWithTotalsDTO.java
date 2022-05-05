package com.ec.crm.Data;

import com.ec.crm.Model.PaymentSchedule;
import com.ec.crm.Service.PaymentScheduleService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ScheduleReturnWithTotalsDTO {
    int totalCount;
    Double totalAmount;
    List<ScheduleReturnDAO> scheduleList;

    public ScheduleReturnWithTotalsDTO(List<PaymentSchedule> scheduleList) {
        this.totalCount = scheduleList.size();
        this.totalAmount = scheduleList.stream().filter(e -> e.getAmount()!=null).mapToDouble(PaymentSchedule::getAmount).sum();
        this.scheduleList = scheduleList.stream().map(PaymentScheduleService::convertPStoPSDAO).collect(Collectors.toList());
    }
}

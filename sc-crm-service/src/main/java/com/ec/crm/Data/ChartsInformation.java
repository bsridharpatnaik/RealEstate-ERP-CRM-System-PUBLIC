package com.ec.crm.Data;

import com.ec.crm.Model.DealStructure;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartsInformation {
    ChartDataDTO customerAmount;
    ChartDataDTO tenPercent;
    ChartDataDTO bankAmount;

    public ChartsInformation(DealStructure ds) {
        this.customerAmount = new ChartDataDTO((ds.getCustomerAmount()==null?0:ds.getCustomerAmount()),(ds.getRemainingCustomerAmount()==null?0: ds.getRemainingCustomerAmount()));
        this.tenPercent = new ChartDataDTO(ds.getTenPercentOfTotalAmount(),ds.getRemainingOfTenPercentTotalAmount());
        this.bankAmount = new ChartDataDTO(ds.getBankAmount(),ds.getRemainingBankAmount());
    }
}

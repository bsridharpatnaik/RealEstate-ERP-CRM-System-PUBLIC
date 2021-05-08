package com.ec.crm.Data;

import com.ec.crm.Enums.LeadStatusEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SalesFunnelDTO {
    String leadStatus;
    long value=0;

    public SalesFunnelDTO(LeadStatusEnum status, long leadCountByStatus) {
        this.leadStatus = status.name();
        this.value=leadCountByStatus;
    }
}

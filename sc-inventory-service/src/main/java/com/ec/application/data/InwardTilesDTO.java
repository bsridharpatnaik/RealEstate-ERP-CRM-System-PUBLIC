package com.ec.application.data;

import lombok.Data;

@Data
public class InwardTilesDTO {
    private long thisWeekCount;
    private long thisMonthCount;
    private long missingChallanBillCount;
    private long rejectCount;
    private long fromPOCount;
    private long directCount;
    private long sampleCount;
}

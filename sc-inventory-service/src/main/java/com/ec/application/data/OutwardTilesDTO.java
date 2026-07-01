package com.ec.application.data;

import lombok.Data;

@Data
public class OutwardTilesDTO {
    private long noBoqCount;
    private long fifoOverrideCount;
    private long rejectCount;
    private long returnCount;
    private long thisWeekCount;
    private long thisMonthCount;
}

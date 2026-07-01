package com.ec.application.data;

import lombok.Data;
import java.util.Map;

@Data
public class POTilesDTO {
    private long thisWeekCount;
    private long thisMonthCount;
    private long openCount;
    private long overdueCount;
    private long specialPoCount;
    private long staleCount;
    private Map<String, Long> statusCounts;
}

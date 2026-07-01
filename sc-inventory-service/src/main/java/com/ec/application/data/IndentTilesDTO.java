package com.ec.application.data;

import lombok.Data;
import java.util.Map;

@Data
public class IndentTilesDTO {
    private long thisWeekCount;
    private long thisMonthCount;
    private long openCount;
    private long quoteRequestedCount;
    private long staleCount;
    private Map<String, Long> statusCounts;
}

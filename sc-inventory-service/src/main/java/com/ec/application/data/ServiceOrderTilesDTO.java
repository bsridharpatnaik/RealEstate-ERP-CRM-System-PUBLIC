package com.ec.application.data;

import lombok.Data;

@Data
public class ServiceOrderTilesDTO {
    private long overdueCount;
    private long next7DaysCount;
    private long next30DaysCount;
    private long next90DaysCount;
}

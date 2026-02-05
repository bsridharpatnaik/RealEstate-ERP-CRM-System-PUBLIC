package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class WeekBucket {
    private String label;   // W-3, W-2, W-1, W-0
    private Date start;
    private Date end;
}

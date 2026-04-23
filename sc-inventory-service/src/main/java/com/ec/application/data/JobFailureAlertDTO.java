package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobFailureAlertDTO {
    private String jobName;
    private String tenant;
    private String errorMessage;
    private String additionalInfo;
    private Date failedAt;
}

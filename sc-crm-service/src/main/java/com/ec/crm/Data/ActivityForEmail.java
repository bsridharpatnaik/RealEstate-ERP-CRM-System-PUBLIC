package com.ec.crm.Data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActivityForEmail {
    private String leadId;
    private String customerName;
    private String primaryMobile;
    private String source;
    private String propertyType;
    private String assignee;
    private String leadStatus;
    private String activityDateTime;
    private String title;
    private String description;
    private String isOpen;
    private String activityType;
    private String isLatestActivity;
    private int followUpCount;
}

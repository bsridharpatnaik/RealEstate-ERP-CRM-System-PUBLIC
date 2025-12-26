package com.ec.application.config;

import lombok.Data;

@Data
public class IndentStatusConstants {
    public static final String STATUS_CREATED = "NEW";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_PO_PARTIAL = "PO_PARTIAL";
    public static final String STATUS_PO_COMPLETED = "PO_COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
}


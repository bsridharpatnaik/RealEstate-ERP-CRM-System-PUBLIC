package com.ec.application.constants;

import lombok.Data;

@Data
public class IndentStatusConstants {
    public static final String STATUS_CREATED = "NEW";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_PO_PARTIAL = "PO PARTIAL";
    public static final String STATUS_PO_COMPLETED = "PO COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
}


package com.ec.application.constants;

import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class IndentStatusConstants {
    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_PO_PARTIAL = "PO PARTIAL";
    public static final String STATUS_PO_COMPLETED = "PO COMPLETED";
    public static final String STATUS_INWARD_PARTIAL = "INWARD PARTIAL";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static List<String> getAllStatuses() {
        return Arrays.stream(IndentStatusConstants.class.getDeclaredFields())
                .filter(field -> field.getType().equals(String.class))
                .map(field -> {
                    try {
                        return (String) field.get(null);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }
}


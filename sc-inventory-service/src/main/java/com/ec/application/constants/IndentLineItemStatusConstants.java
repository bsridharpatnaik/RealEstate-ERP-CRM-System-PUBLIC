package com.ec.application.constants;

import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class IndentLineItemStatusConstants {
    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_PO_CREATED = "PO CREATED";
    public static final String STATUS_INWARD_PARTIAL = "INWARD PARTIAL";
    public static final String STATUS_INWARD_COMPLETE = "INWARD COMPLETE";
    public static final String STATUS_SHORT_CLOSED = "SHORT CLOSED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_SPLIT = "SPLIT"; // not displayed to user


    public static List<String> getAllStatuses() {
        return Arrays.stream(IndentLineItemStatusConstants.class.getDeclaredFields())
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


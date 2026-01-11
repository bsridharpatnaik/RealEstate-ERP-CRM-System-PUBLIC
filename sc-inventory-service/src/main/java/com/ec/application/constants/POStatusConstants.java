package com.ec.application.constants;

import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class POStatusConstants {
    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_SHORT_CLOSED = "SHORT CLOSED";

    public static List<String> getAllStatuses() {
        return Arrays.stream(POStatusConstants.class.getDeclaredFields())
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


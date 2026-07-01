package com.ec.application.constants;

import java.util.Arrays;
import java.util.List;

public final class ServiceOrderStatusConstants {
    private ServiceOrderStatusConstants() {
    }

    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static List<String> getAllStatuses() {
        return Arrays.asList(STATUS_NEW, STATUS_COMPLETED, STATUS_CANCELLED);
    }

    public static List<String> getTerminalStatuses() {
        return Arrays.asList(STATUS_COMPLETED, STATUS_CANCELLED);
    }
}

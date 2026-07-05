package com.ec.application.constants;

import java.util.Arrays;
import java.util.List;

public final class ServiceOrderStatusConstants {
    private ServiceOrderStatusConstants() {
    }

    public static final String STATUS_NEW = "NEW";
    // Header-only status: some lines completed/cancelled, some still NEW. Not a valid line status.
    public static final String STATUS_PARTIALLY_COMPLETED = "PARTIALLY_COMPLETED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static List<String> getAllStatuses() {
        return Arrays.asList(STATUS_NEW, STATUS_PARTIALLY_COMPLETED, STATUS_COMPLETED, STATUS_CANCELLED);
    }

    public static List<String> getTerminalStatuses() {
        return Arrays.asList(STATUS_COMPLETED, STATUS_CANCELLED);
    }

    /** A line is done (no further action possible) once COMPLETED or CANCELLED. */
    public static boolean isLineTerminal(String lineStatus) {
        return STATUS_COMPLETED.equals(lineStatus) || STATUS_CANCELLED.equals(lineStatus);
    }
}

package com.ec.application.constants;

public final class HistoryRelationType {

    private HistoryRelationType() {
        // prevent instantiation
    }

    /** Relation to a Purchase Order (referenceId → PO-27) */
    public static final String PO = "PO";

    /** Relation to an Inward entry (referenceId → INW-118) */
    public static final String INWARD = "INWARD";

    /** Relation to an Indent (referenceId → IN-223) */
    public static final String INDENT = "INDENT";

    /**
     * Optional: user-friendly label for UI
     */
    public static String getDisplayName(String relationType) {
        switch (relationType) {
            case PO:
                return "Purchase Order";
            case INWARD:
                return "Inward";
            case INDENT:
                return "Indent";
            default:
                return relationType;
        }
    }
}


package com.ec.application.data;

public enum StaleAgeBucket {

    GT_3_DAYS("More than 3 days", 3),
    GT_7_DAYS("More than 7 days", 7),
    GT_15_DAYS("More than 15 days", 15),
    GT_30_DAYS("More than 30 days", 30);

    private final String label;   // Display text for UI
    private final int days;        // Cutoff days for backend logic

    StaleAgeBucket(String label, int days) {
        this.label = label;
        this.days = days;
    }

    public String getLabel() {
        return label;
    }

    public int getDays() {
        return days;
    }
}
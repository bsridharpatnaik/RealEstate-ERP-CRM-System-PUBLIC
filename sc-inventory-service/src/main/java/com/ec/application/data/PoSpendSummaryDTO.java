package com.ec.application.data;

import lombok.Data;

import java.util.List;

/** Top KPI tiles + colour config for the Procurement Spend Dashboard. */
@Data
public class PoSpendSummaryDTO {
    private double totalSpend;
    private long totalPoCount;

    /** Money committed but goods not fully received (NEW + PARTIAL). */
    private double openCommitment;
    private long openPoCount;

    private double last3MonthsSpend;

    private long highValueCount;
    private double highValueSpend;

    private double largestPoValue;
    private double avgPoValue;

    /** SHORT CLOSED POs — procurement "leakage" signal. */
    private double shortClosedValue;
    private long shortClosedCount;

    /** From application.properties — echoed so UI colours/threshold match the backend. */
    private double highValueThreshold;
    private List<Double> colorBands;
}

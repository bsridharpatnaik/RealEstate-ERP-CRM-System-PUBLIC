package com.ec.application.data;

import java.util.List;

public class ConsolidatedProductRow {

    private String productName;
    private List<Double> quantities; // one per project, in same order as projectNames
    private double total;

    public ConsolidatedProductRow(String productName, List<Double> quantities, double total) {
        this.productName = productName;
        this.quantities = quantities;
        this.total = total;
    }

    public String getProductName() { return productName; }
    public List<Double> getQuantities() { return quantities; }
    public double getTotal() { return total; }
}

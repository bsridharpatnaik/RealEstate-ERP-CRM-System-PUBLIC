package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One row of a spend breakdown (by project / supplier / firm). */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PoSpendBreakdownRow {
    private String label;
    private double totalValue;
    private long poCount;
}

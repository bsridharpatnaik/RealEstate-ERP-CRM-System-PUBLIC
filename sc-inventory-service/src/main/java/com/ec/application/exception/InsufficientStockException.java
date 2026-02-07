package com.ec.application.exception;


import com.ec.application.data.LowStockItem;
import lombok.Getter;

import java.util.List;

@Getter
public class InsufficientStockException extends RuntimeException {

    private final List<LowStockItem> lowStockItems;

    public InsufficientStockException(List<LowStockItem> lowStockItems) {
        super("Insufficient stock for one or more products");
        this.lowStockItems = lowStockItems;
    }
}
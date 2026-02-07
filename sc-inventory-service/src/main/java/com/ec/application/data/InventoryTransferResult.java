package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InventoryTransferResult {
    private Long transferId;
    private boolean fullySuccessful;
    private List<TransferItemResult> itemResults;
}
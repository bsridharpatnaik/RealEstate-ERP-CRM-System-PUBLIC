package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransferItemResult {
    private Long productId;
    private boolean success;
    private String message;
}

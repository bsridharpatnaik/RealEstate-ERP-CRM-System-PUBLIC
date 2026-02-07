package com.ec.application.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Date;

@Data
@AllArgsConstructor
public class InwardLineItemRequest {
    private String lineItemCode;
    private String inwardId;
    private Date inwardDate;
    private Double quantity;
}
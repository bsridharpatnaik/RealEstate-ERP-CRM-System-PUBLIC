package com.ec.application.data;

import lombok.Data;

/** Marks a single service-order line CANCELLED, with an optional reason. */
@Data
public class CancelServiceOrderLineRequest {
    private String reason;
}

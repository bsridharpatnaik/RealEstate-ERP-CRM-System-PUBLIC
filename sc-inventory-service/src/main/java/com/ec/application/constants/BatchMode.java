package com.ec.application.constants;

/**
 * Controls batch-tracking behaviour for a product.
 *
 * NONE              – no batch tracking (legacy default)
 * BATCH_ONLY        – batches created on inward; expiry date optional;
 *                     useful for brand/lot tracking (taps, fittings, pipes …)
 * BATCH_WITH_EXPIRY – batches created on inward; expiry date mandatory;
 *                     FEFO consumption; expiry alerts enabled
 */
public enum BatchMode {
    NONE,
    BATCH_ONLY,
    BATCH_WITH_EXPIRY
}

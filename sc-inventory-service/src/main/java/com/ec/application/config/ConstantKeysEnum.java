package com.ec.application.config;

public enum ConstantKeysEnum {
    INVENTORY_ALLOWED_DAYS_ADMIN,
    INVENTORY_ALLOWED_DAYS_MANAGER,
    INVENTORY_ALLOWED_DAYS_EXECUTIVE,
    INVENTORY_REJECT_RETURN_DAYS_ADMIN,
    INVENTORY_REJECT_RETURN_DAYS_MANAGER,
    INVENTORY_REJECT_RETURN_DAYS_EXECUTIVE,
    // Boolean flags stored as 0/1
    BOQ_BLOCK_ON_EXCEED,
    BOQ_BLOCK_WHEN_MISSING,
    NEAR_EXPIRY_DAYS;

    @Override
    public String toString() {
        return name();
    }
}

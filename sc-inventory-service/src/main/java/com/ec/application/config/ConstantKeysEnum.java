package com.ec.application.config;

public enum ConstantKeysEnum {
    INVENTORY_ALLOWED_DAYS_ADMIN,
    INVENTORY_ALLOWED_DAYS_MANAGER,
    INVENTORY_ALLOWED_DAYS_EXECUTIVE,
    INVENTORY_REJECT_RETURN_DAYS_ADMIN,
    INVENTORY_REJECT_RETURN_DAYS_MANAGER,
    INVENTORY_REJECT_RETURN_DAYS_EXECUTIVE;

    @Override
    public String toString() {
        return name();
    }
}

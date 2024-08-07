package com.ec.common.Configuration;

public enum ConstantKeys {
    INVENTORY_ALLOWED_DAYS_ADMIN,
    INVENTORY_ALLOWED_DAYS_MANAGER,
    INVENTORY_ALLOWED_DAYS_EXECUTIVE;

    @Override
    public String toString() {
        return name();
    }
}

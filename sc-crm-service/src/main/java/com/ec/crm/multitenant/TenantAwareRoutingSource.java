package com.ec.crm.multitenant;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class TenantAwareRoutingSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return com.ec.crm.multitenant.ThreadLocalStorage.getTenantName();
    }
}

package com.ec.application.service;

import com.ec.application.ReusableClasses.EmailHelper;
import com.ec.application.config.SchemaConfig;
import com.ec.application.data.StockDiscrepancyRow;
import com.ec.application.multitenant.ThreadLocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockBalanceValidationService {

    private static final Logger log = LoggerFactory.getLogger(StockBalanceValidationService.class);
    private static final double TOLERANCE = 0.001;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private SchemaConfig schemaConfig;

    @Autowired
    private EmailHelper emailHelper;

    @Autowired
    private Environment environment;

    public void runNightlyValidation() throws Exception {
        List<String> tenants = schemaConfig.getNonMasterSchemaList();
        List<StockDiscrepancyRow> allDiscrepancies = new ArrayList<>();

        for (String tenant : tenants) {
            ThreadLocalStorage.setTenantName(tenant);
            try {
                log.info("Stock balance validation running for tenant: {}", tenant);
                List<StockDiscrepancyRow> discrepancies = validateTenant(tenant);
                allDiscrepancies.addAll(discrepancies);
                log.info("Tenant {} — {} discrepancy(ies) found", tenant, discrepancies.size());
            } catch (Exception e) {
                log.error("Stock balance validation failed for tenant: {}", tenant, e);
            } finally {
                ThreadLocalStorage.setTenantName(null);
            }
        }

        boolean isProd = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.contains("prod"));
        if (!isProd) {
            log.info("Skipping stock balance validation email — not a prod profile");
            return;
        }

        if (!tenants.isEmpty()) {
            ThreadLocalStorage.setTenantName(tenants.get(0));
        }
        try {
            emailHelper.sendStockBalanceValidationEmail(allDiscrepancies);
        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }

    private List<StockDiscrepancyRow> validateTenant(String tenant) {
        // Indices: 0=inward, 1=transferIn, 2=outward, 3=lostDamaged, 4=transferOut, 5=stockInHand
        Map<Long, double[]> data = new HashMap<>();

        mergeInto(queryInward(), data, 0);
        mergeInto(queryTransferIn(tenant), data, 1);
        mergeInto(queryOutward(), data, 2);
        mergeInto(queryLostDamaged(), data, 3);
        mergeInto(queryTransferOut(tenant), data, 4);
        mergeInto(queryStockInHand(), data, 5);

        Map<Long, String> productNames = queryProductNames();

        List<StockDiscrepancyRow> result = new ArrayList<>();
        for (Map.Entry<Long, double[]> entry : data.entrySet()) {
            double[] d = entry.getValue();
            // Formula: inward + transferIn - outward - lostDamaged - transferOut = stockInHand
            double expected = d[0] + d[1] - d[2] - d[3] - d[4];
            double actual   = d[5];
            double diff     = expected - actual;
            if (Math.abs(diff) > TOLERANCE) {
                result.add(new StockDiscrepancyRow(
                    tenant,
                    productNames.getOrDefault(entry.getKey(), "Product#" + entry.getKey()),
                    round(d[0]), round(d[1]), round(d[2]), round(d[3]), round(d[4]),
                    round(expected), round(actual), round(diff)
                ));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> queryInward() {
        return entityManager.createQuery(
            "SELECT e.product.id, SUM(e.quantity) " +
            "FROM InwardInventory i JOIN i.inwardOutwardList e " +
            "GROUP BY e.product.id"
        ).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> queryOutward() {
        return entityManager.createQuery(
            "SELECT e.product.id, SUM(e.quantity) " +
            "FROM OutwardInventory o JOIN o.inwardOutwardList e " +
            "GROUP BY e.product.id"
        ).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> queryLostDamaged() {
        return entityManager.createQuery(
            "SELECT l.product.id, SUM(l.quantity) " +
            "FROM LostDamagedInventory l " +
            "GROUP BY l.product.id"
        ).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> queryTransferOut(String tenant) {
        return entityManager.createQuery(
            "SELECT ti.productId, SUM(ti.quantity) " +
            "FROM InventoryTransfer t JOIN t.items ti " +
            "WHERE t.sourceTenant = :tenant " +
            "GROUP BY ti.productId"
        ).setParameter("tenant", tenant).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> queryTransferIn(String tenant) {
        return entityManager.createQuery(
            "SELECT ti.productId, SUM(ti.quantity) " +
            "FROM InventoryTransfer t JOIN t.items ti " +
            "WHERE t.targetTenant = :tenant " +
            "GROUP BY ti.productId"
        ).setParameter("tenant", tenant).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> queryStockInHand() {
        return entityManager.createQuery(
            "SELECT s.product.id, SUM(s.quantityInHand) " +
            "FROM Stock s " +
            "GROUP BY s.product.id"
        ).getResultList();
    }

    @SuppressWarnings("unchecked")
    private Map<Long, String> queryProductNames() {
        List<Object[]> rows = entityManager.createQuery(
            "SELECT p.id, p.productName FROM Product p"
        ).getResultList();
        Map<Long, String> map = new HashMap<>();
        for (Object[] r : rows) {
            map.put(((Number) r[0]).longValue(), (String) r[1]);
        }
        return map;
    }

    private void mergeInto(List<Object[]> rows, Map<Long, double[]> data, int index) {
        for (Object[] row : rows) {
            if (row[0] == null) continue;
            Long productId = ((Number) row[0]).longValue();
            double qty = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
            data.computeIfAbsent(productId, k -> new double[6])[index] = qty;
        }
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}

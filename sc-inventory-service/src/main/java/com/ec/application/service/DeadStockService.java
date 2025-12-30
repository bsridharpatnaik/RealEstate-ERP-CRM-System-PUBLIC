package com.ec.application.service;

import com.ec.application.config.SchemaConfig;
import com.ec.application.model.DeadStock;
import com.ec.application.model.Stock;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.DeadStockRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeadStockService {

    @Autowired
    private StockService stockService;

    @Autowired
    private DeadStockRepo deadStockRepo;

    @Value("${master.schema}")
    private String masterSchema;

    private static final Logger log =
            LoggerFactory.getLogger(DeadStockService.class);

    @Transactional
    public void syncTenantDeadStock(String tenant) {

        // Tenant is already set here ✅
        List<DeadStock> rows = buildDeadStockRows(tenant);

        // Switch ONLY when writing to master
        ThreadLocalStorage.setTenantName(masterSchema);
        deadStockRepo.deleteByTenantName(tenant);
        deadStockRepo.saveAll(rows);

        log.info("Synced {} dead stock records for tenant {}",
                rows.size(), tenant);
    }

    private List<DeadStock> buildDeadStockRows(String tenant) {

        List<Stock> deadStocks = stockService.getDeadStocks();
        List<DeadStock> result = new ArrayList<>();

        for (Stock stock : deadStocks) {
            DeadStock ds = new DeadStock();
            ds.setTenantName(tenant);
            ds.setProductId(stock.getProduct().getProductId());
            ds.setProductName(stock.getProduct().getProductName());
            ds.setProductCode(stock.getProduct().getProductCode());
            ds.setQuantityInHand(stock.getQuantityInHand());
            result.add(ds);
        }
        return result;
    }
}
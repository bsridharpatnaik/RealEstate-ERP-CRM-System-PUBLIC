package com.ec.application.service;

import com.ec.application.model.InventoryTransfer;
import com.ec.application.model.InventoryTransferItem;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.InventoryTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryTransferService {

    @Autowired
    private final InventoryTransferRepository inventoryTransferRepository;

    @Autowired
    private final StockService stockService;

    @Value("${master.schema}")
    private String masterSchema;

    public InventoryTransfer createTransfer(InventoryTransfer transfer) {
        validateTransfer(transfer);
        List<InventoryTransferItem> processedItems = new ArrayList<>();

        try {
            // SOURCE tenant - OUTWARD
            ThreadLocalStorage.setTenantName(transfer.getSourceTenant());

            for (InventoryTransferItem item : transfer.getItems()) {
                stockService.updateStock(item.getProductId(), transfer.getSourceWarehouseName(), item.getQuantity(), "outward");
                processedItems.add(item);
            }

            // TARGET tenant - INWARD
            ThreadLocalStorage.setTenantName(transfer.getTargetTenant());

            for (InventoryTransferItem item : processedItems) {
                stockService.updateStock(item.getProductId(), transfer.getTargetWarehouseName(), item.getQuantity(), "inward"
                );
            }

            // 3️⃣ MASTER schema - SAVE TRANSFER
            ThreadLocalStorage.setTenantName(MasterSchemaConstants.MASTER_SCHEMA);

            transfer.setTransferDate(new Date());
            transfer.getItems()
                    .forEach(i -> i.setInventoryTransfer(transfer));

            return inventoryTransferRepository.save(transfer);

        } catch (Exception ex) {

            // 🔁 COMPENSATION LOGIC
            compensate(transfer, processedItems);
            throw ex;

        } finally {
            ThreadLocalStorage.clear();
        }
        private void compensate (
                InventoryTransfer transfer,
                List < InventoryTransferItem > processedItems
){
            try {
                // 1️⃣ Revert TARGET inward (if already done)
                ThreadLocalStorage.setTenantName(transfer.getTargetTenant());

                for (InventoryTransferItem item : processedItems) {
                    stockService.updateStock(
                            item.getProductId(),
                            transfer.getTargetWarehouseName(),
                            item.getQuantity(),
                            "outward"
                    );
                }

                // 2️⃣ Revert SOURCE outward
                ThreadLocalStorage.setTenantName(transfer.getSourceTenant());

                for (InventoryTransferItem item : processedItems) {
                    stockService.updateStock(
                            item.getProductId(),
                            transfer.getSourceWarehouseName(),
                            item.getQuantity(),
                            "inward"
                    );
                }

            } catch (Exception compensationEx) {
                // 🚨 CRITICAL: log & alert, do NOT swallow
                log.error("COMPENSATION FAILED for transfer", compensationEx);
            }
        }

    }
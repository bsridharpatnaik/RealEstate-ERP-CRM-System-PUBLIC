package com.ec.application.service;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.ActivityLogDescription;
import com.ec.application.model.InventoryNotification;
import com.ec.application.model.Product;
import com.ec.application.repository.InventoryNotificationRepo;
import com.ec.application.repository.WarehouseRepo;

@Service
@Transactional
public class InventoryNotificationService {

    @Autowired
    StockService stockService;

    @Autowired
    InventoryNotificationRepo inventoryNotificationRepo;

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    WarehouseRepo warehouseRepo;

    @Autowired
    ProductTenantConfigService productTenantConfigService;

    @Autowired
    ActivityLogService activityLogService;

    Logger log = LoggerFactory.getLogger(InventoryNotificationService.class);

    final String lowStock = "lowStock";
    final String inwardModified = "inwardStockModified";
    final String outwardModified = "outwardStockModified";
    final String lostDamagedModified = "lostDamagedStockModified";
    final String lostDamagedAdded = "lostDamagedStockAdded";
    final String excessFoundAdded = "excessFoundStockAdded";
    final String excessFoundModified = "excessFoundStockModified";

    @Transactional(rollbackOn = Exception.class)
    public void checkStockAndPushLowStockNotification(Product product) {
        Double currentStock = stockService.findTotalStockForProduct(product.getProductId());
        // Use tenant-specific override if set, otherwise fall back to global reorder level
        Double reorderQuantity = productTenantConfigService.getEffectiveReorderLevel(product);
        currentStock = currentStock == null ? 0 : currentStock;
        reorderQuantity = reorderQuantity == null ? 0 : reorderQuantity;
        if (currentStock <= reorderQuantity && reorderQuantity > 0)
            pushLowStockNotification(product, currentStock, reorderQuantity);
        else if (currentStock > reorderQuantity && reorderQuantity > 0)
            removeLowStockNotification(product, currentStock, reorderQuantity);
    }

    @Transactional(rollbackOn = Exception.class)
    public void pushQuantityEditedNotification(Product product, String warehouseName, String type, Double currentStock)
            throws Exception {

        type = getTypeFromReadableName(type);
        InventoryNotification inventoryNotificationNew = new InventoryNotification();
        inventoryNotificationNew.setProduct(product);
        inventoryNotificationNew.setType(type);
        inventoryNotificationNew.setQuantity(currentStock);
        inventoryNotificationNew.setUpdatedBy(userDetailsService.getCurrentUser().getUsername());
        inventoryNotificationNew.setWarehouseName(warehouseName);
        inventoryNotificationRepo.save(inventoryNotificationNew);
    }

    private String getTypeFromReadableName(String type) {
        switch (type) {
            case "inward":
                return inwardModified;

            case "outward":
                return outwardModified;

            case "lostdamagedmodified":
                return lostDamagedModified;

            case "lostdamagedadded":
                return lostDamagedAdded;

            case "excessfoundadded":
                return excessFoundAdded;

            case "excessfoundmodified":
                return excessFoundModified;
        }
        return "";
    }

    @Transactional(rollbackOn = Exception.class)
    private void removeLowStockNotification(Product product, Double currentStock, Double reorderQuantity) {
        List<InventoryNotification> inventoryNotifications = inventoryNotificationRepo
                .findByProductAndType(product.getProductId(), lowStock);
        if (inventoryNotifications.size() > 0) {
            for (InventoryNotification inventoryNotification : inventoryNotifications) {
                inventoryNotificationRepo.softDelete(inventoryNotification);
            }
            // Log stock restored — only when transitioning from low → normal
            String desc = ActivityLogDescription.of(product.getProductName()
                    + " stock restored. Current: " + fmt(currentStock)
                    + ", Reorder level: " + fmt(reorderQuantity));
            activityLogService.record("STOCK_RESTORED", "STOCK_ALERT",
                    String.valueOf(product.getProductId()), desc, "System");
            log.info("Stock restored for product {}", product.getProductName());
        }
    }

    @Transactional(rollbackOn = Exception.class)
    private void pushLowStockNotification(Product product, Double currentStock, Double reorderQuantity) {
        List<InventoryNotification> inventoryNotifications = inventoryNotificationRepo
                .findByProductAndType(product.getProductId(), lowStock);
        if (inventoryNotifications.size() == 0) {
            // First time crossing threshold — log activity
            InventoryNotification inventoryNotificationNew = new InventoryNotification();
            inventoryNotificationNew.setProduct(product);
            inventoryNotificationNew.setType(lowStock);
            inventoryNotificationNew.setQuantity(currentStock);
            inventoryNotificationNew.setUpdatedBy("System");
            inventoryNotificationRepo.save(inventoryNotificationNew);
            String action = currentStock <= 0 ? "OUT_OF_STOCK" : "LOW_STOCK";
            String desc = ActivityLogDescription.of(product.getProductName()
                    + (currentStock <= 0 ? " is out of stock." : " is low on stock.")
                    + " Current: " + fmt(currentStock)
                    + ", Reorder level: " + fmt(reorderQuantity));
            activityLogService.record(action, "STOCK_ALERT",
                    String.valueOf(product.getProductId()), desc, "System");
            log.info("{} alert logged for product {}", action, product.getProductName());
        } else {
            InventoryNotification inventoryNotification = inventoryNotifications.get(0);
            inventoryNotification.setQuantity(currentStock);
            inventoryNotificationRepo.save(inventoryNotification);
        }
    }

    private String fmt(Double val) {
        if (val == null) return "0";
        return val == Math.floor(val) ? String.valueOf(val.intValue()) : String.valueOf(val);
    }

    List<InventoryNotification> returnInventoryNotifications() {
        List<InventoryNotification> inventoryNotifications = inventoryNotificationRepo
                .findAll(Sort.by(Sort.Direction.DESC, "lastModifiedDate"));
        return inventoryNotifications;
    }

    @Transactional(rollbackOn = Exception.class)
    public void deleteNotificationByID(Long id) throws Exception {
        Optional<InventoryNotification> inventoryNotificationOpt = inventoryNotificationRepo.findById(id);

        if (!inventoryNotificationOpt.isPresent())
            throw new Exception("Notification not found");

        InventoryNotification inventoryNotification = inventoryNotificationOpt.get();
        if (inventoryNotification.getType().equals("lowStock"))
            throw new Exception("Cannot delete lowStock notifications");

        inventoryNotificationRepo.softDelete(inventoryNotification);
    }

    public void deleteNotificationForProduct(Long id) {
        List<InventoryNotification> notList = inventoryNotificationRepo.findByProductId(id);
        for (InventoryNotification in : notList)
            inventoryNotificationRepo.softDelete(in);
    }
}

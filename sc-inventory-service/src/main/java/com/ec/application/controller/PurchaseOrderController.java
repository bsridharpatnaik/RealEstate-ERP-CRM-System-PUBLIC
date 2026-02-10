package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.aspects.AllowOnly;
import com.ec.application.aspects.CheckAuthority;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.config.SchemaConfig;
import com.ec.application.constants.RoleConstants;
import com.ec.application.data.*;
import com.ec.application.model.IndentInventory;
import com.ec.application.model.IndentStatusHistory;
import com.ec.application.model.PurchaseOrder;
import com.ec.application.model.PurchaseOrderStatusHistory;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.service.PurchaseOrderService;
import com.ec.application.service.PurchaseOrderStatusHistoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/purchase-order")
@RequiredArgsConstructor
@UseDefaultTenant
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderStatusHistoryService purchaseOrderStatusHistoryService;
    private final SchemaConfig schemaConfig;


    private static final Logger log =
            LoggerFactory.getLogger(PurchaseOrderController.class);

    @PostMapping("/create")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.INVENTORY_MANAGER})
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrder createPurchaseOrder(@RequestBody CreatePoRequest payload) throws Exception {
        return purchaseOrderService.createPurchaseOrder(payload);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ReturnPurchaseOrderData fetchPurchaseOrdersPage(@RequestBody FilterDataList filterDataList, @PageableDefault(page = 0, size = 10, sort = "creationDate", direction = Sort.Direction.DESC) Pageable pageable) throws Exception {
        return purchaseOrderService.fetchPurchaseOrdersPage(filterDataList, pageable);
    }

    @GetMapping("/{id}")
    public PurchaseOrder findPurchaseOrderByID(@PathVariable String id) throws Exception {
        return purchaseOrderService.getPurchaseOrderWithInit(id);
    }

    @DeleteMapping(value = "/{id}")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.INVENTORY_MANAGER})
    public ResponseEntity<?> cancelPurchaseOrderById(@PathVariable String id) throws Exception {
        purchaseOrderService.cancelPurchaseOrderById(id);
        return ResponseEntity.ok("Entity deleted");
    }

    @PostMapping("/short-close")
    public PurchaseOrder shortClosePo(@RequestBody ShortClosePoRequest request) throws Exception {
        PurchaseOrder po = purchaseOrderService.shortClosePurchaseOrder(request);
        return po;
    }

    @GetMapping("/{id}/status-history")
    public List<PurchaseOrderStatusHistory> getIndentStatusHistory(@PathVariable String id) {
        return purchaseOrderStatusHistoryService.getStatusHistoryForPO(id);
    }

    @PostMapping(
            value = "/export/excel",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
    public ResponseEntity<StreamingResponseBody> exportPurchaseOrdersExcel(
            @RequestBody(required = false) FilterDataList filterDataList) {

        // 🔑 Capture tenant on request thread
        String tenant = schemaConfig.getMasterSchema();

        StreamingResponseBody stream = outputStream -> {
            try {
                // 🔑 Restore tenant inside async thread
                ThreadLocalStorage.setTenantName(
                        tenant != null ? tenant : "masterschema"
                );

                purchaseOrderService.streamPurchaseOrderExcel(
                        filterDataList,
                        outputStream
                );

            } catch (Exception e) {
                log.error("PO Excel export failed", e);
            } finally {
                ThreadLocalStorage.setTenantName(null);
            }
        };

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=purchase-orders.xlsx"
                )
                .body(stream);
    }

    @ExceptionHandler({JpaSystemException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
        return new ApiOnlyMessageAndCodeError(500, "Something went wrong while handling data. Contact Administrator.");
    }
}
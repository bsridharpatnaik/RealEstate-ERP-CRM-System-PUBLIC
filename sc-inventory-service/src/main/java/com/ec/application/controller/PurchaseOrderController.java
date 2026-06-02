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
import com.ec.application.repository.IndentInventoryRepo;
import com.ec.application.repository.PurchaseOrderIndentRefRepository;
import com.ec.application.service.PriorityComputeService;
import com.ec.application.service.PurchaseOrderPdfService;
import com.ec.application.service.PurchaseOrderService;
import com.ec.application.service.PurchaseOrderStatusHistoryService;
import com.itextpdf.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/purchase-order")
@RequiredArgsConstructor
@UseDefaultTenant
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderStatusHistoryService purchaseOrderStatusHistoryService;
    private final SchemaConfig schemaConfig;
    private final PurchaseOrderPdfService purchaseOrderPdfService;
    private final PriorityComputeService priorityComputeService;
    private final PurchaseOrderIndentRefRepository purchaseOrderIndentRefRepository;
    private final IndentInventoryRepo indentInventoryRepo;


    private static final Logger log =
            LoggerFactory.getLogger(PurchaseOrderController.class);

    @PostMapping("/create")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
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

    @PutMapping("/{id}")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public PurchaseOrder updatePurchaseOrder(@PathVariable String id, @RequestBody UpdatePoRequest payload) throws Exception {
        return purchaseOrderService.updatePurchaseOrder(id, payload);
    }

    @DeleteMapping(value = "/{id}")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public ResponseEntity<?> cancelPurchaseOrderById(@PathVariable String id) throws Exception {
        purchaseOrderService.cancelPurchaseOrderById(id);
        return ResponseEntity.ok("Entity deleted");
    }

    /**
     * Adds a new line item to an existing PO.
     * PO must be in NEW or PARTIAL status.
     * The indent line item being added must be in NEW status.
     */
    @PostMapping("/{id}/line")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public PurchaseOrder addLineItem(@PathVariable String id, @RequestBody CreatePoLineRequest payload) throws Exception {
        return purchaseOrderService.addLineItem(id, payload);
    }

    /**
     * Adds multiple new line items to an existing PO in one call.
     * PO must be in NEW or PARTIAL status.
     * All supplied indent line items must be in NEW status.
     */
    @PostMapping("/{id}/lines")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public PurchaseOrder addLineItems(@PathVariable String id, @RequestBody List<CreatePoLineRequest> payload) throws Exception {
        return purchaseOrderService.addLineItems(id, payload);
    }

    /**
     * Removes an open line item from an existing PO.
     * The line's linked indent item must be in PO CREATED status (no inward started).
     */
    @DeleteMapping("/{id}/line/{lineId}")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public PurchaseOrder removeLineItem(@PathVariable String id, @PathVariable Long lineId) throws Exception {
        return purchaseOrderService.removeLineItem(id, lineId);
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

    @GetMapping("/{id}/indents")
    public List<IndentInventory> getIndentsForPo(@PathVariable String id) {
        return getPoFilteredIndents(id);
    }

    /**
     * Fetches all indents linked to the given PO and trims each indent's
     * inventoryList to only the line items that are actually part of this PO
     * (matched via PurchaseOrderIndentRef.indentLineItemCode).
     */
    private List<IndentInventory> getPoFilteredIndents(String poId) {
        List<String> indentIds = purchaseOrderIndentRefRepository.findDistinctIndentNosByPoId(poId);
        if (indentIds.isEmpty()) return Collections.emptyList();

        List<IndentInventory> indents = indentInventoryRepo.findWithDetailsByIndentIdIn(indentIds);

        // Build a map: indentNo → Set of line-item codes that belong to this PO
        Map<String, Set<String>> allowedByIndent = new HashMap<>();
        purchaseOrderIndentRefRepository.findByPurchaseOrderIdIn(Collections.singletonList(poId))
                .forEach(ref -> allowedByIndent
                        .computeIfAbsent(ref.getIndentNo(), k -> new HashSet<>())
                        .add(ref.getIndentLineItemCode()));

        // Filter each indent's inventoryList to only PO-linked items
        indents.forEach(indent -> {
            Set<String> allowed = allowedByIndent.getOrDefault(indent.getIndentId(), Collections.emptySet());
            Set<com.ec.application.model.IndentInventoryList> filtered = indent.getInventoryList().stream()
                    .filter(item -> allowed.contains(item.getLineItemCode()))
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
            indent.setInventoryList(filtered);
        });

        return indents;
    }

    @GetMapping(value = "/print-po/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<StreamingResponseBody> printPOAsPdf(
            @PathVariable String id,
            @RequestParam(value = "hideMoneyFields", required = false, defaultValue = "false") boolean hideMoneyFields,
            @RequestParam(value = "includeIndents", required = false, defaultValue = "false") boolean includeIndents
    ) {
        String tenant = schemaConfig.getMasterSchema();
        PurchaseOrder po;
        try {
            po = purchaseOrderService.getPurchaseOrderWithInit(id);
        } catch (Exception e) {
            log.error("Failed to fetch PO ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        List<IndentInventory> indents = Collections.emptyList();
        if (includeIndents) {
            try {
                indents = getPoFilteredIndents(id);
            } catch (Exception e) {
                log.warn("Failed to fetch indents for PO {}: {}", id, e.getMessage());
            }
        }

        final List<IndentInventory> indentsForPrint = indents;
        String filename = po.getPurchaseOrderId() + ".pdf";

        StreamingResponseBody stream = outputStream -> {
            try {
                ThreadLocalStorage.setTenantName(tenant != null ? tenant : "masterschema");
                purchaseOrderPdfService.generatePdf(po, outputStream, hideMoneyFields, indentsForPrint);
                outputStream.flush();
            } catch (DocumentException e) {
                log.error("iText PDF generation failed for PO ", e);
            } catch (IOException e) {
                log.error("IO error writing PDF for PO ", e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                ThreadLocalStorage.setTenantName(null);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(stream);
    }

    /**
     * Manually triggers PO priority recomputation.
     * POs live in the master schema — @UseDefaultTenant on this class sets the correct schema context.
     * Accessible only to ADMIN and PURCHASE_MANAGER roles.
     */
    @PostMapping("/po-prioritize")
    @CheckAuthority
    @AllowOnly(roles = {RoleConstants.ADMIN, RoleConstants.PURCHASE_MANAGER})
    public ResponseEntity<String> triggerPoPrioritization() {
        log.info("Manual PO priority recompute triggered");
        priorityComputeService.recomputeAllPriorities();
        return ResponseEntity.ok("PO prioritization completed successfully");
    }

    @GetMapping("/project-list")
    public List<String> getProjectList() {
        return schemaConfig.getNonMasterSchemaList();
    }

    @ExceptionHandler({JpaSystemException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
        return new ApiOnlyMessageAndCodeError(500, "Something went wrong while handling data. Contact Administrator.");
    }
}
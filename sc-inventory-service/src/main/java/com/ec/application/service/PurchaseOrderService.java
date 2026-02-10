package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.Filters.IndentInventorySpecification;
import com.ec.application.Filters.PurchaseOrderSpecification;
import com.ec.application.ReusableClasses.ReusableFields;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.config.SchemaConfig;
import com.ec.application.constants.*;
import com.ec.application.data.*;
import com.ec.application.enricher.PurchaseOrderUiEnricher;
import com.ec.application.indentpo.PurchaseOrderLifecycleManager;
import com.ec.application.model.*;
import com.ec.application.repository.PurchaseOrderRepo;
import com.ec.application.util.PurchaseOrderPriceMasker;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.checkerframework.checker.units.qual.A;
import org.hibernate.Hibernate;
import org.hibernate.envers.Audited;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class PurchaseOrderService extends ReusableFields {

    @Autowired
    PurchaseOrderRepo purchaseOrderRepo;

    @Autowired
    PurchaseOrderValidator validator;

    @Autowired
    PurchaseOrderBuilder poBuilder;

    @Autowired
    IndentStatusUpdater indentStatusUpdater;

    @Autowired
    PurchaseOrderUiEnricher purchaseOrderUiEnricher;

    @Autowired
    PopulateDropdownService populateDropdownService;

    @Autowired
    PurchaseOrderLifecycleManager poLifecycleManager;

    @Autowired
    DraftService draftService;

    @Autowired
    PurchaseOrderPriceMasker purchaseOrderPriceMasker;

    @Autowired
    PurchaseOrderStatusHistoryService poStatusHistoryService;

    @Autowired
    UserDetailsService userDetailsService;

    @Transactional
    public PurchaseOrder createPurchaseOrder(CreatePoRequest request) throws Exception {
        validator.validateIndentLineItems(request.getLineItems());
        PurchaseOrder po = poBuilder.buildPurchaseOrder(request);
        PurchaseOrder savedPO = purchaseOrderRepo.save(po);
        indentStatusUpdater.updateIndentStatuses(savedPO, POIndentUpdateAction.CREATE_PO);
        draftService.deleteDraftForUser("PO");
        String username = userDetailsService.getCurrentUser().getUsername();
        poStatusHistoryService.logStatusChange(savedPO, null, savedPO.getStatus(), username, buildPoCreationMessage(request, username), buildPoCreationRelations(request));
        return savedPO;
    }

    @Transactional(readOnly = true)
    public ReturnPurchaseOrderData fetchPurchaseOrdersPage(FilterDataList filterDataList, Pageable pageable) throws Exception {

        ReturnPurchaseOrderData returnData = new ReturnPurchaseOrderData();
        Specification<PurchaseOrder> spec = PurchaseOrderSpecification.getSpecification(filterDataList);
        Page<PurchaseOrder> page = (spec != null)
                ? purchaseOrderRepo.findAll(spec, pageable)
                : purchaseOrderRepo.findAll(pageable);

        // Initialize lazy-loaded associations
        initializeLazyAssociations(page.getContent());

        // MASK PRICE FIELDS
        purchaseOrderPriceMasker.mask(page);

        // Enrich UI flags
        purchaseOrderUiEnricher.enrich(page.getContent());
        returnData.setPuchaseOrders(page);
        returnData.setPoDropdown(populateDropdownService.fetchData("purchaseorder"));
        return returnData;
    }

    private void initializeLazyAssociations(List<PurchaseOrder> purchaseOrders) {
        purchaseOrders.forEach(po -> {
            // Initialize supplier
            if (po.getSupplier() != null) {
                Hibernate.initialize(po.getSupplier());
                String supplierName = po.getSupplier().getName(); // Touch to load
            }

            // Initialize firm
            if (po.getFirm() != null) {
                Hibernate.initialize(po.getFirm());
                String firmName = po.getFirm().getFirmName(); // Touch to load
            }

            // Initialize lines
            if (po.getLines() != null && !po.getLines().isEmpty()) {
                Hibernate.initialize(po.getLines());
                po.getLines().forEach(line -> {
                    // Initialize product
                    if (line.getProduct() != null) {
                        Hibernate.initialize(line.getProduct());
                        String productName = line.getProduct().getProductName(); // Touch to load
                    }

                    // Initialize indent refs
                    if (line.getIndentRefs() != null) {
                        Hibernate.initialize(line.getIndentRefs());
                    }
                });
            }
        });
    }

    @Transactional(readOnly = true)
    public PurchaseOrder getPurchaseOrderWithInit(String id) throws Exception {
        PurchaseOrder po = purchaseOrderRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Purchase Order not found with ID: " + id
                        ));

        // Initialize supplier & firm
        if (po.getSupplier() != null) {
            po.getSupplier().getName();
        }
        if (po.getFirm() != null) {
            po.getFirm().getFirmName();
        }

        // Initialize lines + indentRefs + product
        for (PurchaseOrderLine line : po.getLines()) {
            line.getIndentRefs().size();
            if (line.getProduct() != null) {
                line.getProduct().getProductName();
            }
        }

        // MASK PRICE FIELDS
        purchaseOrderPriceMasker.mask(po);
        return po;
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelPurchaseOrderById(String id) throws Exception {
        poLifecycleManager.cancelIfAllowed(id);
    }

    @Transactional
    public PurchaseOrder shortClosePurchaseOrder(ShortClosePoRequest request) throws Exception {
        if (request.getPurchaseOrderNo() == null)
            throw new IllegalArgumentException("Purchase Order Number cannot be null");
        poLifecycleManager.shortClosePo(request);
        return purchaseOrderRepo.findByIdWithDetails(request.getPurchaseOrderNo()).get();
    }

    private String buildPoCreationMessage(CreatePoRequest request, String username) {
        String indentDetails = request.getLineItems().stream()
                .flatMap(line ->
                        line.getIndentRefs().stream()
                                .map(ref ->
                                        ref.getIndentLineItemCode() +
                                                " (Qty: " + line.getQuantity() + ")"
                                )
                )
                .collect(Collectors.joining(", "));

        return "Purchase Order created by user " + username + ". Indent line items: " + indentDetails;
    }

    private List<HistoryRelationInput> buildPoCreationRelations(CreatePoRequest request) {
        return request.getLineItems().stream()
                .flatMap(line ->
                        line.getIndentRefs().stream()
                                .map(ref -> extractIndentId(ref.getIndentLineItemCode()))
                )
                .distinct()
                .map(indentId ->
                        new HistoryRelationInput(
                                HistoryRelationType.INDENT,
                                "",
                                indentId
                        )
                )
                .collect(Collectors.toList());
    }

    private String extractIndentId(String indentLineItemCode) {
        if (indentLineItemCode == null || !indentLineItemCode.contains("/")) {
            throw new IllegalArgumentException(
                    "Invalid indent line item code: " + indentLineItemCode
            );
        }
        return indentLineItemCode.substring(0, indentLineItemCode.indexOf('/'));
    }

    @Transactional(readOnly = true)
    public Map<String, DashboardChartDTO> getCurrentPODashboards() {

        List<String> statuses = Arrays.asList(
                POStatusConstants.STATUS_NEW,      // Zero Inward
                POStatusConstants.STATUS_PARTIAL   // Partial
        );

        List<StatusGroupCountDTO> rows = purchaseOrderRepo.fetchCurrentPOStatusCounts(statuses);

        Map<String, Map<String, Long>> grouped = new HashMap<>();

        for (StatusGroupCountDTO row : rows) {
            grouped
                    .computeIfAbsent(row.getStatus(), k -> new HashMap<>())
                    .merge(row.getGroupKey(), row.getCount(), Long::sum);
        }

        Map<String, DashboardChartDTO> dashboards = new HashMap<>();

        for (String status : statuses) {
            Map<String, Long> firmMap =
                    grouped.getOrDefault(status, new HashMap<>());

            List<TenantCountDTO> firmCounts = new ArrayList<>();
            long total = 0;

            for (Map.Entry<String, Long> e : firmMap.entrySet()) {
                firmCounts.add(new TenantCountDTO(e.getKey(), e.getValue()));
                total += e.getValue();
            }

            dashboards.put(status,
                    new DashboardChartDTO(total, firmCounts));
        }

        return dashboards;
    }

    public void streamPurchaseOrderExcel(
            FilterDataList filterDataList,
            OutputStream os) {

        SXSSFWorkbook workbook = null;

        try {
            workbook = new SXSSFWorkbook(100);
            Sheet sheet = workbook.createSheet("Purchase Orders");

            int rowNum = 0;

            // ===== Header =====
            Row header = sheet.createRow(rowNum++);
            String[] headers = {
                    "PO Number",
                    "PO Date",
                    "PO Status",

                    "Supplier",
                    "Firm",

                    "Product",
                    "Quantity",
                    "Rate",
                    "GST %",
                    "Net Rate",
                    "Total Amount",

                    "Indent No",
                    "Indent Line Item Code"
            };

            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            Specification<PurchaseOrder> spec =
                    PurchaseOrderSpecification.getSpecification(filterDataList);

            int page = 0;
            int size = 200;
            Page<PurchaseOrder> poPage;

            do {
                Pageable pageable = PageRequest.of(page, size);
                poPage = purchaseOrderRepo.findAll(spec, pageable);

                List<String> poIds = poPage.getContent().stream()
                        .map(PurchaseOrder::getPurchaseOrderId)
                        .collect(Collectors.toList());

                if (!poIds.isEmpty()) {

                    List<PurchaseOrder> purchaseOrders =
                            purchaseOrderRepo.findWithDetailsByIdIn(poIds);

                    // ===== Flatten PO → Lines =====
                    for (PurchaseOrder po : purchaseOrders) {

                        String supplierName =
                                po.getSupplier() != null
                                        ? po.getSupplier().getName()
                                        : "";

                        String firmName =
                                po.getFirm() != null
                                        ? po.getFirm().getFirmName()
                                        : "";

                        for (PurchaseOrderLine line : po.getLines()) {

                            if (line.getIndentRefs() == null
                                    || line.getIndentRefs().isEmpty()) {

                                // Still export line even if no indent
                                Row row = sheet.createRow(rowNum++);
                                writePoRow(row, po, supplierName, firmName,
                                        line, "", "");
                            } else {
                                for (PurchaseOrderIndentRef ref : line.getIndentRefs()) {
                                    Row row = sheet.createRow(rowNum++);
                                    writePoRow(
                                            row,
                                            po,
                                            supplierName,
                                            firmName,
                                            line,
                                            extractIndentId(ref.getIndentLineItemCode()),
                                            ref.getIndentLineItemCode()
                                    );
                                }
                            }
                        }
                    }
                }

                page++;
            } while (!poPage.isLast());

            workbook.write(os);
            os.flush();

        } catch (Exception e) {
            LoggerFactory.getLogger(getClass())
                    .error("PO Excel export error", e);
        } finally {
            if (workbook != null) {
                workbook.dispose();
            }
        }
    }

    private void writePoRow(
            Row row,
            PurchaseOrder po,
            String supplierName,
            String firmName,
            PurchaseOrderLine line,
            String indentNo,
            String indentLineItemCode) {

        int col = 0;

        row.createCell(col++).setCellValue(po.getPurchaseOrderId());
        row.createCell(col++).setCellValue(
                po.getPoDate() != null ? po.getPoDate().toString() : ""
        );
        row.createCell(col++).setCellValue(
                safeExcel(po.getStatus())
        );

        row.createCell(col++).setCellValue(safeExcel(supplierName));
        row.createCell(col++).setCellValue(safeExcel(firmName));

        row.createCell(col++).setCellValue(
                safeExcel(
                        line.getProduct() != null
                                ? line.getProduct().getProductName()
                                : ""
                )
        );

        row.createCell(col++).setCellValue(
                line.getQuantity() != null ? line.getQuantity() : 0.0
        );
        row.createCell(col++).setCellValue(
                line.getRate() != null ? line.getRate() : 0.0
        );
        row.createCell(col++).setCellValue(
                line.getGstPercent() != null ? line.getGstPercent() : 0.0
        );
        row.createCell(col++).setCellValue(
                line.getNetRate() != null ? line.getNetRate() : 0.0
        );
        row.createCell(col++).setCellValue(
                line.getTotalAmount() != null ? line.getTotalAmount() : 0.0
        );

        row.createCell(col++).setCellValue(safeExcel(indentNo));
        row.createCell(col++).setCellValue(safeExcel(indentLineItemCode));
    }

    private String safeExcel(String value) {
        if (value == null) return "";
        if (value.startsWith("=") || value.startsWith("+")
                || value.startsWith("-") || value.startsWith("@")) {
            return "'" + value;
        }
        return value;
    }

}
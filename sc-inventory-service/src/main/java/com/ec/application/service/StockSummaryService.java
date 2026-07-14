package com.ec.application.service;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.config.SchemaConfig;
import com.ec.application.constants.ProjectConstants;
import com.ec.application.data.*;
import com.ec.application.model.Product;
import com.ec.application.repository.JobExecutionLogRepository;
import com.ec.application.repository.ProductRepo;
import com.ec.application.repository.StockSummaryRepo;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@UseDefaultTenant
public class StockSummaryService {

    private final StockSummaryRepo stockSummaryRepo;
    private final PopulateDropdownService populateDropdownService;
    private final JobExecutionLogRepository jobExecutionLogRepo;
    private final ProductService productService;
    private final SchemaConfig schemaConfig;
    private final ProductTenantConfigService productTenantConfigService;
    private final ProductRepo productRepo;
    private final UserDetailsService userDetailsService;

    Logger log = LoggerFactory.getLogger(StockSummaryService.class);

    /**
     * Current user's allowed tenant schemas — used to scope this cross-tenant report.
     * Fails closed: if the user can't be resolved we abort rather than return all tenants.
     */
    private List<String> resolveAllowedSchemas() {
        try {
            return userDetailsService.getCurrentUserAllowedSchemas();
        } catch (Exception e) {
            throw new RuntimeException("Unable to resolve user's allowed tenants", e);
        }
    }

    public com.ec.application.data.StockSummaryTilesDTO getTiles(FilterDataList filterDataList) {
        return stockSummaryRepo.getTileCounts(filterDataList, resolveAllowedSchemas());
    }

    public StockSummaryWithDropdownData findFilteredStockSummary(FilterDataList filterDataList, Pageable pageable) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        StockSummaryWithDropdownData returnData = new StockSummaryWithDropdownData();
        Page<StockSummaryAggregatedDTO> data = stockSummaryRepo.fetchAggregatedStock(filterDataList, pageable, resolveAllowedSchemas());
        returnData.setStockSummaries(data);
        returnData.setStockDropdown(populateDropdownService.fetchData("deadstock"));
        returnData.setLastSyncDate(jobExecutionLogRepo.findLastSuccessfulRunTime("STOCK_SYNC"));
        return returnData;
    }

    // ── Export ────────────────────────────────────────────────────────────────────

    /**
     * Streams a .xlsx file containing all rows matching the current filters.
     * The Reorder Level column is highlighted yellow to indicate it is editable.
     * The file can be downloaded, edited, and re-uploaded via importReorderLevels().
     */
    public void streamExportExcel(FilterDataList filterDataList, HttpServletResponse response) throws IOException {
        // Fetch all matching rows (no pagination cap)
        Page<StockSummaryAggregatedDTO> page =
                stockSummaryRepo.fetchAggregatedStock(filterDataList, PageRequest.of(0, Integer.MAX_VALUE), resolveAllowedSchemas());
        List<StockSummaryAggregatedDTO> rows = page.getContent();
        if (rows.size() > 5000)
            throw new IOException("Too many rows to export. Please apply filters to reduce results below 5000 and try again.");

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"stock-summary.xlsx\"");

        // SXSSFWorkbook streams rows to disk — memory-safe for large result sets
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            Sheet sheet = wb.createSheet("Stock Summary");

            // ── Styles ──────────────────────────────────────────────────────────
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle editableStyle = wb.createCellStyle();
            editableStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            editableStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle editableHeaderStyle = wb.createCellStyle();
            Font editableHeaderFont = wb.createFont();
            editableHeaderFont.setBold(true);
            editableHeaderStyle.setFont(editableHeaderFont);
            editableHeaderStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            editableHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ── Header row ──────────────────────────────────────────────────────
            String[] headers = {
                "Tenant", "Product Code", "Product Name", "Measurement Unit",
                "Reorder Level", "Current Stock", "Dead Stock", "Last Synced"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                // Column 4 (Reorder Level) gets the yellow editable header style
                cell.setCellStyle(i == 4 ? editableHeaderStyle : headerStyle);
            }

            // ── Data rows ───────────────────────────────────────────────────────
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

            int rowIdx = 1;
            for (StockSummaryAggregatedDTO dto : rows) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(dto.getTenantSchema() != null ? dto.getTenantSchema() : "");
                row.createCell(1).setCellValue(dto.getProductCode()   != null ? dto.getProductCode()   : "");
                row.createCell(2).setCellValue(dto.getProductName()   != null ? dto.getProductName()   : "");
                row.createCell(3).setCellValue(dto.getMeasurementUnit() != null ? dto.getMeasurementUnit() : "");

                // Reorder Level — yellow background, numeric if present
                Cell reorderCell = row.createCell(4);
                reorderCell.setCellStyle(editableStyle);
                if (dto.getReorderLevel() != null) {
                    reorderCell.setCellValue(dto.getReorderLevel());
                }

                if (dto.getQuantityInHand() != null) row.createCell(5).setCellValue(dto.getQuantityInHand());
                else row.createCell(5).setCellValue("");

                if (dto.getDeadStock() != null) row.createCell(6).setCellValue(dto.getDeadStock());
                else row.createCell(6).setCellValue("");

                row.createCell(7).setCellValue(
                    dto.getSyncedAt() != null ? sdf.format(dto.getSyncedAt()) : "");
            }

            wb.write(response.getOutputStream());
            wb.dispose(); // remove temp files used by SXSSF
        }
    }

    // ── Import ────────────────────────────────────────────────────────────────────

    /**
     * Parses the uploaded Excel file and updates the reorder level override
     * in each tenant's product_tenant_config table.
     *
     * Expected column order (matches the export):
     *   0: Tenant  |  1: Product Code  |  4: Reorder Level  (others are ignored)
     *
     * Returns a summary: { updated, skipped, errors }
     */
    public Map<String, Object> importReorderLevels(MultipartFile file) throws IOException {
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            // Row 0 is the header — start at 1
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) { skipped++; continue; }

                String tenantSchema = getCellString(row, 0);
                String productCode  = getCellString(row, 1);
                String reorderRaw   = getCellString(row, 4);

                // Skip rows with missing key columns
                if (tenantSchema == null || tenantSchema.trim().isEmpty() ||
                        productCode == null || productCode.trim().isEmpty()) {
                    skipped++;
                    continue;
                }

                // Skip rows with no reorder level value
                if (reorderRaw == null || reorderRaw.trim().isEmpty()) {
                    skipped++;
                    continue;
                }

                // Parse reorder level
                double reorderLevel;
                try {
                    reorderLevel = Double.parseDouble(reorderRaw.trim());
                } catch (NumberFormatException e) {
                    errors.add("Row " + (i + 1) + ": Invalid reorder level value '" + reorderRaw + "'");
                    skipped++;
                    continue;
                }

                if (reorderLevel < 0) {
                    errors.add("Row " + (i + 1) + ": Reorder level cannot be negative ('" + reorderRaw + "')");
                    skipped++;
                    continue;
                }

                // Validate tenant
                if (!schemaConfig.isValidSchema(tenantSchema)) {
                    errors.add("Row " + (i + 1) + ": Unknown tenant '" + tenantSchema + "'");
                    skipped++;
                    continue;
                }

                // Look up product by code (runs in master schema — products live there)
                List<Product> products = productRepo.findByproductCode(productCode);
                if (products == null || products.isEmpty()) {
                    errors.add("Row " + (i + 1) + ": Product code '" + productCode + "' not found");
                    skipped++;
                    continue;
                }
                Long productId = products.get(0).getProductId();

                // Save override in the tenant's product_tenant_config table
                try {
                    productTenantConfigService.saveOverrideForTenant(productId, tenantSchema, reorderLevel);
                    updated++;
                } catch (Exception e) {
                    errors.add("Row " + (i + 1) + ": Failed to update product '" + productCode
                               + "' for tenant '" + tenantSchema + "': " + e.getMessage());
                    skipped++;
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", updated);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /** Reads a cell as a String regardless of its type. */
    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:  return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default:      return "";
        }
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DashboardProductStockDTO> getDashboardProductStock() {

        List<Product> dashboardProducts = productService.getDashboardProducts();
        if (dashboardProducts.isEmpty()) {
            return Collections.emptyList();
        }

        // All tenant names from config (excluding master)
        List<String> allTenants = schemaConfig.getNonMasterSchemaList();

        Map<Long, Product> productMap = new HashMap<>();
        List<Long> productIds = new ArrayList<>();
        for (Product p : dashboardProducts) {
            productIds.add(p.getProductId());
            productMap.put(p.getProductId(), p);
        }

        // productId → DTO
        Map<Long, DashboardProductStockDTO> dtoMap = new HashMap<>();
        // productId → tenantSchema → TenantStockDTO (for merging dead stock in)
        Map<Long, Map<String, TenantStockDTO>> tenantMap = new HashMap<>();

        // ── Pre-initialise every product with ALL tenants at zero ──────────
        for (Product p : dashboardProducts) {
            DashboardProductStockDTO dto = new DashboardProductStockDTO();
            dto.setProductId(p.getProductId());
            dto.setProductCode(p.getProductCode());
            dto.setProductName(p.getProductName());
            dto.setMeasurementUnit(p.getMeasurementUnit());
            dto.setTotalStock(0.0);
            dto.setTotalDeadStock(0.0);
            dto.setTenantWiseStock(new ArrayList<>());
            dtoMap.put(p.getProductId(), dto);

            Map<String, TenantStockDTO> tenantEntries = new LinkedHashMap<>();
            for (String tenant : allTenants) {
                TenantStockDTO tenantStock = new TenantStockDTO(tenant, 0.0, 0.0, 0.0);
                dto.getTenantWiseStock().add(tenantStock);
                tenantEntries.put(tenant, tenantStock);
            }
            tenantMap.put(p.getProductId(), tenantEntries);
        }

        // ── Regular stock — update matching tenant entries ─────────────────
        List<Object[]> rows = stockSummaryRepo.fetchTenantWiseStockForProducts(productIds);
        for (Object[] row : rows) {
            Long productId = (Long)   row[0];
            String tenant  = (String) row[1];
            Double qty     = (Double) row[2];

            DashboardProductStockDTO dto = dtoMap.get(productId);
            if (dto == null) continue;

            TenantStockDTO tenantStock = tenantMap.get(productId).get(tenant);
            if (tenantStock != null) {
                tenantStock.setQuantity(qty);
                dto.setTotalStock(dto.getTotalStock() + qty);
            }
        }

        // ── Dead stock — update matching tenant entries ────────────────────
        List<Object[]> deadRows = stockSummaryRepo.fetchTenantWiseDeadStockForProducts(
                productIds, ProjectConstants.deadStockWarehouseName
        );
        for (Object[] row : deadRows) {
            Long productId = (Long)   row[0];
            String tenant  = (String) row[1];
            Double deadQty = (Double) row[2];

            DashboardProductStockDTO dto = dtoMap.get(productId);
            if (dto == null) continue;

            TenantStockDTO tenantStock = tenantMap.get(productId).get(tenant);
            if (tenantStock != null) {
                tenantStock.setDeadStock(deadQty);
                dto.setTotalDeadStock(dto.getTotalDeadStock() + deadQty);
            }
        }

        // ── Enrich each TenantStockDTO with its own effective reorder level ──
        // Queries every tenant schema in turn so overrides are respected.
        Map<String, Map<Long, Double>> reorderByTenant =
                productTenantConfigService.getEffectiveReorderLevelsByTenant(dashboardProducts);

        for (Product p : dashboardProducts) {
            DashboardProductStockDTO dto = dtoMap.get(p.getProductId());
            if (dto == null) continue;

            Double globalReorderLevel = p.getReorderQuantity();
            Double minEffective = null;
            boolean anyOverridden = false;

            for (TenantStockDTO tenantStock : dto.getTenantWiseStock()) {
                Map<Long, Double> tenantLevels = reorderByTenant.get(tenantStock.getTenantSchema());
                if (tenantLevels != null) {
                    Double effective = tenantLevels.get(p.getProductId());
                    tenantStock.setReorderLevel(effective);
                    if (effective != null) {
                        if (minEffective == null || effective < minEffective) {
                            minEffective = effective;
                        }
                        if (globalReorderLevel != null && !effective.equals(globalReorderLevel)) {
                            anyOverridden = true;
                        }
                    }
                }
            }

            // Product-level: minimum effective across all tenants (most conservative threshold)
            dto.setReorderLevel(minEffective != null ? minEffective : globalReorderLevel);
            dto.setReorderOverridden(anyOverridden);
        }

        return new ArrayList<>(dtoMap.values());
    }
}

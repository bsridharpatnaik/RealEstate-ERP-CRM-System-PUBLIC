# Feature Log — Narrative History (archive)

> Not auto-loaded into Claude context. This is the "what was built" changelog for shipped features.
> The durable rules, gotchas, and invariants distilled from these live in the root `CLAUDE.md`.
> Read a section here on demand when working on that specific feature.
>
> Source of truth is always the code + git history. This file is a convenience index/narrative.

---

# Activity Log Feature

Full activity log system — per-project logs synced to a global cross-tenant log.

### Backend — key files

| File | Role |
|---|---|
| `service/ActivityLogService.java` | Public `record()` entry point — synchronous, captures tenant, delegates to writer |
| `service/ActivityLogWriter.java` | `@Async("processExecutor")` — receives tenant explicitly, saves to correct schema |
| `service/ActivityLogGlobalSyncOrchestrator.java` | Syncs all tenant `activity_log` tables → master `global_activity_log`. Uses `getSchemaList()` (includes master) |
| `scheduled/ActivityLogSyncJob.java` | Cron every 15 min — calls orchestrator |
| `model/ActivityLog.java` | Per-tenant table: action, entityType, entityId, description, performedBy, activityTime |
| `model/GlobalActivityLog.java` | Master table: same fields + tenantSchema |
| `Filters/ActivityLogSpecification.java` | Per-tenant filter |
| `Filters/GlobalActivityLogSpecification.java` | Global filter — supports tenantSchema filter |
| `controller/ActivityLogController.java` | `/list`, `/export`, `/by-entity/{type}/{id}`, `/global/list`, `/global/export`, `/global/sync` |
| `ReusableClasses/ActivityLogDescription.java` | Builds structured JSON descriptions for tabular display |

`ActivityLogService.record()` is synchronous (no `@Async`). Reads tenant from ThreadLocal and delegates the async DB write to `ActivityLogWriter.saveAsync(entry, tenant)` with tenant passed explicitly as a safety net.

### Entity Types Logged

| Entity | Actions |
|---|---|
| `INWARD` | CREATED, UPDATED, DELETED, REJECTED |
| `OUTWARD` | CREATED, UPDATED, DELETED, RETURNED, REJECTED |
| `INDENT` | CREATED, UPDATED, DELETED, CANCELLED, APPROVED, SHORT_CLOSED, SPLIT |
| `PURCHASE_ORDER` | CREATED, UPDATED, DELETED, SHORT_CLOSED |
| `LOST_DAMAGED` | CREATED, UPDATED, DELETED |
| `EXCESS_FOUND` | CREATED, UPDATED, DELETED |
| `MACHINERY_ON_RENT` | CREATED, UPDATED, DELETED |
| `INVENTORY_TRANSFER` | CREATED, UPDATED, DELETED |
| `WRITE_OFF` | CREATED (in `BatchTrackingService.writeOffBatch()`) |
| `STOCK_SPLIT` | CREATED (in `BatchTrackingService.splitExistingStock()`) |
| `SERVICE_ORDER` | CREATED, UPDATED, COMPLETED, CANCELLED, LINE_COMPLETED, LINE_CANCELLED, LINE_REOPENED |

Entity type for lost/damaged uses the actual `entryType` field value (`LOST_DAMAGED` or `EXCESS_FOUND`).

### Description Format (JSON) — parsed by frontend into a mini-table

```json
// CREATE / REJECT / RETURN — single qty
{"summary": "Inward 7096 created by sridhar", "items": [{"product": "Steel", "qty": "10"}]}
// UPDATE — old and new qty
{"summary": "Inward 7096 updated by sridhar", "items": [{"product": "Steel", "oldQty": "5", "newQty": "10"}]}
// No items (header-only update / delete)
{"summary": "Inward 7096 deleted by sridhar"}
```

UPDATE logs for inward/outward are consolidated — ONE log per update operation with all changed lines.

### Frontend — key files

| File | Role |
|---|---|
| `Modules/Activity/list.js` | Global activity log page — `/global/list`, Sync Now + Export |
| `Modules/Activity/projectList.js` | Per-project page — `/list` |
| `Modules/Activity/filter.js` | Global filter — all entity types + Project dropdown (tenantCode) |
| `Modules/Activity/projectFilter.js` | Project filter — excludes INDENT, PURCHASE_ORDER, no tenant field |
| `Modules/Activity/table.js` | Shared table — renders description via `renderActivityDescription` |
| `Modules/Activity/renderActivityDescription.js` | Parses JSON description → mini-table |
| `Modules/InwardInventory/details.js` | History tab via `activityLogByEntity('INWARD', id)` |
| `Modules/OutwardInventory/details.js` | History tab via `activityLogByEntity('OUTWARD', id)` |

Routes: `/activityLog` (global, admin), `/projectActivityLog` (per-project, admin). Global filter's Project dropdown fetches tenants via `apiEndpoints.getTenants`, maps to `{ name: tenantName, id: tenantCode }`; `prepareRequestBody` sends `.id` (tenantCode) as `tenantSchema` filter, matched against `global_activity_log.tenantSchema`.

---

# Batch Tracking Feature

Tracks physical batches of stock per warehouse. FIFO/FEFO outward consumption, write-offs, split of untracked stock, batch-metadata editing. Lives in project (tenant) schema. (Core concepts, invariants, and sentinel values are in root `CLAUDE.md`.)

### Key Backend Files

| File | Role |
|---|---|
| `model/InventoryBatch.java` | Batch entity |
| `model/OutwardBatchConsumption.java` | Per-outward batch consumption record |
| `model/BatchWriteOff.java` | Write-off event record |
| `service/BatchTrackingService.java` | Core logic: FIFO consumption, split, write-off, batch edit, tiles |
| `service/OutwardInventoryService.java` | Calls batch tracking during create/update/return/delete of outwards |
| `repository/InventoryBatchRepository.java` | `sumQtyRemainingGroupByProduct`, `findAllByInwardId` |
| `repository/OutwardBatchConsumptionRepository.java` | `deleteByOutwardId` (bulk JPQL `@Modifying`), `findByOutwardIdAndBatch_BatchId` |
| `repository/ProductRepo.java` | `findBatchTrackedProductIds()` — IDs where batchMode ≠ NONE |
| `repository/StockInformationRepo.java` | `findByProductIdInAndTotalQuantityInHandGreaterThan` |
| `controller/BatchTrackingController.java` | All batch API endpoints |
| `data/BatchUpdateRequestDTO.java` | `brand`, `lotNumber`, `expiryDate`, `receivedDate` for PUT /batch/{id} |
| `data/WriteOffRequestDTO.java` | `quantity`, `reason`, `writeOffDate` |
| `data/StockSplitRequest.java` | `warehouseId`, `batches[]` |
| `data/StockTilesDTO.java` | `expiredCount`, `nearExpiryCount`, `untrackedCount` |

### API Endpoints (all under `/api/inventory`)

| Method | Path | Description |
|---|---|---|
| `PUT` | `/batch/{batchId}` | Edit batch metadata (brand, lotNumber, expiryDate, receivedDate) |
| `POST` | `/batch/{batchId}/write-off` | Write off qty from a batch |
| `GET` | `/batch/{batchId}/write-off/history` | Write-off history for a batch |
| `GET` | `/stock/{productId}/batches?warehouseId=` | Get batches; warehouseId optional (null = all) |
| `POST` | `/stock/{productId}/split-existing` | Split untracked stock into batches |
| `POST` | `/stock/tiles/expiry` | Stock tile counts (expired / near-expiry / untracked) |
| `GET` | `/outward/{outwardId}/batch-consumptions` | Consumption records for an outward |
| `GET` | `/inward/{inwardId}/batches` | Batches created during an inward |
| `POST` | `/outward/preview-batches` | Preview which batches FIFO would consume |

### Key Frontend Files

| File | Role |
|---|---|
| `Modules/Stock/details.js` | Batches tab: per-warehouse sections, split/write-off/edit forms |
| `Modules/Stock/list.js` | Stock list with tile chips (expired / near-expiry / untracked) |
| `Modules/OutwardInventory/add.js` | Batch override UI during outward creation |
| `Modules/OutwardInventory/edit.js` | Preserves override batches when qty unchanged on edit |
| `Modules/OutwardInventory/details.js` | `⚡ FIFO Override` badge + batch consumption table |

### Frontend State in `Stock/details.js`

`batches`, `batchesLoading`, `splitFormOpenWarehouseId`, `splitEntries` (`{qty, expiryDate, brand, lotNumber}`), `splitSubmitting`, `splitError`, `writeOffForm` (`{batchId, warehouseId, quantity, reason}`), `writeOffSubmitting`, `writeOffHistories` (`{[batchId]: [...]}`), `editBatchForm`, `stockAdjustments` (`{[warehouseId]: number}` — running write-off totals to keep the untracked banner accurate without full reload).

### Session 2 build notes

- **Expiry Alert Notifications fix** — `AllNotificationService.getInventoryNotification()` had no handlers for `EXPIRY_ALERT_30/60/EXPIRED`; created by scheduled job but rendered blank. Added three `if` blocks after `lostDamagedStockAdded`, plus a guard to only add to `inventoryNormalizedNotifications` when `message` is non-empty. Formats: `"Product X in Warehouse Y is expiring within N days. Qty remaining: Z."` / `"...has expired. Qty remaining: Z. Please write off expired stock."`
- **Daily Stock Email — expiry sections** — nightly report (`sendDailyStockEmailReport`, cron `0 0 21 * * *`) adds two conditional sections (today→+30 red, +31→+60 amber). Files: `data/ExpiryAlertRow.java` (new DTO), `service/StockEmailReportService.java` (`collectExpiryRows`), `scheduled/ScheduledTasks.java`, `ReusableClasses/EmailHelper.java` (two extra `List<ExpiryAlertRow>` params → Freemarker `expiring30`/`expiring60`), `controller/AutomaticEmailController.java` (manual `/email/dailystockreport` trigger), `templates/email-daily-stock.ftl`. `InventoryBatch.expiryDate` is Java `Date` — format with `SimpleDateFormat("dd-MM-yyyy")`. Sections render only when non-empty; both cross-project; cron and manual trigger produce identical output.
- **Inventory Transfer — Details Page (new)** — `Modules/InventoryTransfer/details.js` (fetch by ID → header + items table), `list.js` (`showDetails`/`selectedRow` + `<Slide>` panel), endpoint `getInventoryTransferById(id)` → `GET /api/inventory/inventory-transfer/{id}`. Wrapper div class `"split"` when open.
- **Inward Edit — batch modal visible in edit mode** — `add.js` batch split button now shown in create+edit. Labels: `"Set Batches *"` / `"Edit Batches"` / `"View / Edit Batches"`. `loadExistingData()` pre-populates `batchSplits` from `GET /inward/{inwardId}/batches` (`qtyReceived`→`qty`, `expiryDate` `dd-MM-yyyy`→`yyyy-MM-dd`). Old inline PO brand/expiry fields replaced with `{false && ...}` dead-code block.

### Session 3 — Conflict Scenario Analysis

Business rules confirmed:
- **Inventory Transfer**: no edit or delete. Read-only after creation.
- **Lost/Damaged & Excess Found**: edit/delete removed (correct a mistake by adding the opposite entry). Removed from `Lost/index.js`, `Lost/list.js`, `Lost/details.js`.
- **Write-off**: intentionally irreversible. History only.

Stock-modifying operations & batch impact:

| Operation | Stock | Batch |
|---|---|---|
| Inward Create | +qty | Creates `InventoryBatch` (inwardId = real inward ID) |
| Excess Found | +qty | Increases `qtyRemaining` OR creates new batch |
| Inventory Transfer In | +qty target | Creates batch at target (`inwardId = 0`) |
| Outward Return | +qty | Restores `qtyRemaining`; reduces `OutwardBatchConsumption` |
| Outward Create | −qty | Drains via FIFO/FEFO; creates `OutwardBatchConsumption` |
| Inventory Transfer Out | −qty source | Drains at source via FIFO |
| Lost/Damaged | −qty | Drains specified batches directly |
| Write Off | −qty | Drains batch directly; `BatchWriteOff` record |
| Inward Reject | −qty | Drains batches linked to inward (`inwardId` match) |

Batch safety guards (all verified present):
- **Inward Delete** — `zeroBatchesForDeletedInward()` throws if `qtyReceived - qtyRemaining > 0.001` (any downstream consumption).
- **Inward Edit qty reduce** — `reconcileBatchesForEditedInward()` reduces from last batch backward; throws if reduction exceeds available.
- **Inward Edit qty increase** — safe (adds/creates).
- **Inward Reject** — `addReturnForInward()` checks `qty > rb.getQtyRemaining()`; validates `sum(overrideBatches.qty) == quantity`.
- **Outward Delete/Edit/Return** — full reverse then fresh consume; `hasFifoOverride` reset to false before re-consume.
- **Untracked Stock Guard** — present on outward create/edit, lost/damaged create. NOT verified on inventory-transfer-out.

FIFO override — resolved frontend behavior: qty-field lock removed from outward `edit.js`. Single-batch override + qty changed → auto-sends `overrideBatches = [{batchId, qty:newQty}]` (only when `consumptions.length === 1 && overriddenConsumptions.length === 1`). Multi-batch override + qty changed → blocks save. Qty unchanged + override → re-send existing `overrideBatches` with fallback `overrideComment` ("Override preserved").

Inward Edit batch reconciliation — `add.js` state: `product.batchSplits` (original data, never overwritten by reduce), `product._reduceSplits` (reduce-path delta only), overwrite on increase. All `expiryDate` stored `dd-MM-yyyy`. Payload: `isReducePath = !!product._reduceSplits`; `batchId` only included when reduce path (prevents original splits triggering backend `hasSplitsWithBatchId` reduce-detection when user only changed qty).

`reconcileBatchesForEditedInward` backend truth table:
- delta = 0: metadata update by position, batchId ignored.
- delta < 0 single batch: auto-reduce.
- delta < 0 multi-batch, no splits with batchId: throws.
- delta < 0 multi-batch, splits with batchId: validate sum == reduction, drain per batch.
- delta > 0 no splits: throws "specify batch."
- delta > 0 splits: validate sum == delta, create/merge.

External review findings — fixed (Session 3):

| # | File | Issue → Fix |
|---|---|---|
| 1+7 | `InventoryTransferService` | Override total check was AFTER individual saves → partial deductions survived. Two-pass: validate+sum before saves; catch block reverses source deductions + soft-deletes partial target batches. `createTargetBatchesForTransfer` returns `List<Long>`. |
| 2 | `OutwardInventoryService.restoreBatchesForReturn` | Added: filter valid, dedup, sum==quantity, product match, outward consumption match, qty ≤ consumed. Two-pass. |
| 3 | `ReturnProduct.js` | `checkValidation` requires `sum(batchReturnQtys) == returnquantity` + live allocated/remaining indicator. |
| 5 | outward/transfer/reject/L&D | Added `batch.product == productId` ownership check everywhere; warehouse check to L&D + inward reject. |
| 9 | `LostDamagedInventoryService.handleBatchOnCreate` | Null-batchId entries counted in sum but skipped in loop → divergence. Filter valid first, dedup, ownership, two-pass. Both LOST_DAMAGED + EXCESS_FOUND. |
| — | `InventoryTransferService.deductSourceBatchesForTransfer` | Added product + warehouse ownership checks in pass 1. |

---

# FIFO Override Report

Global cross-tenant report showing all outward transactions where batch consumption deviated from FIFO/FEFO (`fifo_overridden = true`). Admin-only. Reports → FIFO Override Report.

Incremental sync (same pattern as `ActivityLogGlobalSyncService`) — tracks `MAX(syncedAt)` per tenant. First run loads all; later runs only `lastModifiedDate > lastSyncTime`; deleted source rows removed from master. Hourly cron (`FifoReportSyncJob`) + manual POST `/fifo-report/sync`.

Master table `global_fifo_report`, unique `(tenantSchema, outwardId, batchId, productId)`. Denormalized fields: tenantSchema, outwardId, outwardDate, product{Id,Name,Code}, measurementUnit, warehouse{Id,Name}, usageLocationName, usageAreaName, contractorName, purpose, batch{Id,LotNumber,Brand,ReceivedDate,ExpiryDate}, qtyConsumed, overrideComment, performedBy (from `outward_inventory.createdBy`), syncedAt.

**Sync Step 6 (metadata refresh):** incremental sync only touches modified rows, so product renames go stale. After sync, `FifoReportSyncService` refreshes product name+unit for ALL synced products of that tenant via `findDistinctProductIdsByTenantSchema` + `updateProductMetadata`. (General pattern — apply to any synced report with mutable product metadata.)

Backend: `model/GlobalFifoReport.java`, `repository/GlobalFifoReportRepository.java`, `Filters/GlobalFifoReportSpecification.java`, `service/FifoReportSyncService.java`, `service/FifoReportSyncOrchestrator.java` (AtomicBoolean guard, `JobExecutionLog` JOB_NAME=`FIFO_REPORT_SYNC`), `scheduled/FifoReportSyncJob.java`, `service/FifoReportService.java` (list + Excel 19 cols), `controller/FifoReportController.java` (`/list` + `/export/excel` are `@UseDefaultTenant`). Also added `OutwardBatchConsumptionRepository.findAllOverrides()`/`findOverridesModifiedAfter`, `OutwardInventoryRepo.findByOutwardidIn`.

Frontend: `Modules/Reports/FifoReport/{index,filter,table}.js`; route `/fifoReport` (in `isProjectSelectionPage`), sidebar under Reports (admin only).

---

# Reports Module (Session 4)

## Low Stock Report

Global report of products below reorder level. Sync every 30 min (`LowStockSyncJob`, `0 0/30 * * * *`). Master table `global_low_stock_report`.

**Reorder level source (critical):** `LowStockSyncService` reads reorder from **`ProductTenantConfig`** (tenant schema), NOT `StockSummary.reorderLevel`. Steps: load StockSummary → load ProductTenantConfig overrides → effective = `tenantOverride ?? stockSummary.reorderLevel` → filter `totalQty < effectiveReorder` → fresh product meta from ProductRepo → upsert preserving `lowStockSince`.

**Tile date window (calendar-day aligned):** `dateMinusDays(days)` returns start-of-calendar-day, not rolling 24h. `days=1` → today 00:00; `days=3` → 2 days ago 00:00. Frontend `handleTileClick` mirrors this. Bug to avoid: `start.setDate(today - tile.days)` gives a rolling window (a 22:30 record shows next morning under "New Today").

Backend: `model/GlobalLowStockReport.java`, `repository/GlobalLowStockReportRepository.java` (tile counts: HQL `COUNT(r)`, not positional `ORDER BY 2`), `service/LowStockSyncService.java`, `service/LowStockOrchestrator.java` (`getNonMasterSchemaList()`), `service/LowStockReportService.java`, `scheduled/LowStockSyncJob.java`, `controller/LowStockController.java` (all `@UseDefaultTenant` except `/sync`). Tiles: 1d/3d/7d/30d/total, clickable, per-project breakdown on hover. Filter: Project + Category only. Frontend `Modules/Reports/LowStock/{index,filter,table}.js`, route `/lowStockReport`.

## PO vs Inward Reconciliation Report

Global, compares ordered vs received per PO line. **Live native SQL** (no sync — PO in master schema). `purchase_order` + `purchase_order_line` + `indent_inventory_entries` + `indent_inventory`. Manual `COUNT(*) FROM (subquery)` pagination. Project = `COALESCE(ii.tenant, po.project_name, 'Unknown')`. `reconciliationStatus` filter via `HAVING (CASE...) = :status`. `WhereClause` inner class has separate `where` and `having` fields. Card-based UX (one expandable card per PO).

```java
BASE_FROM = " FROM purchase_order po JOIN purchase_order_line pol ... LEFT JOIN Firm f ... LEFT JOIN indent_inventory_entries iie ... LEFT JOIN indent_inventory ii ..."
GROUP_BY  = " GROUP BY po.purchase_order_id, pol.product_id, COALESCE(ii.tenant, po.project_name, 'Unknown')"
RECON_STATUS_CASE = " CASE WHEN COALESCE(SUM(iie.quantity_received),0) <= 0 THEN 'NOT_STARTED' WHEN ... >= MAX(pol.quantity) THEN 'COMPLETE' ELSE 'PARTIAL' END"
```

Backend: `data/PoInwardReconciliationRow.java` (`@JsonFormat(dd-MM-yyyy)` on poDate), `service/PoInwardReconciliationService.java`, `controller/PoInwardReconciliationController.java` (all `@UseDefaultTenant`, default sort `poDate DESC`). Frontend `Modules/Reports/PoReconciliation/{index,filter,cards,table}.js`, route `/poReconReport`. **Session 6:** `enrichWithBOQ()` removed (called `getCachedBOQStatusRows()` → 5s+ latency); export now 13 cols, cards grid 8→7.

## Indent Fulfillment Report

Global, indent line items with requested/received/pending. Live query. Default sort `ORDER BY ii.indent_date DESC, ii.indent_id ASC, p.product_name ASC` (rows for one indent must be contiguous for card grouping). `indentStatus` filter supports multiple values via IN.

```sql
FROM indent_inventory ii
JOIN indent_inventory_entries iie ON iie.indent_id = ii.indent_id AND iie.is_deleted = 0
JOIN product p ON p.productId = iie.productId AND p.is_deleted = 0
LEFT JOIN purchase_order po ON po.purchase_order_id = iie.purchaseOrderId AND po.is_deleted = 0
LEFT JOIN purchase_order_line pol ON pol.po_id = iie.purchaseOrderId AND pol.product_id = iie.productId AND pol.is_deleted = 0
WHERE ii.is_deleted = 0
```

`IndentFulfillmentRow`: indentId, project, indentDate, indentStatus, productName, productCode, unit, requestedQty, poQty (nullable), receivedQty, pendingQty, percentFulfilled, poNumber, poStatus, lineItemStatus, needByDate, categoryName (r[15], Session 6). Frontend `Modules/Reports/IndentFulfillment/{index,filter,cards,table}.js`, route `/indentFulfillmentReport`.

Status groups (tile filter → array sent as `attrValue`, backend `IN (:s0,:s1)`):

| Group | Statuses |
|---|---|
| PENDING | NEW, APPROVED |
| IN_PROGRESS | PO CREATED, PO PARTIAL, INWARD PARTIAL |
| COMPLETED | CLOSED, PO COMPLETED |
| CANCELLED | CANCELLED, REJECTED |

**Card status-summary pill logic** (reflects actual fulfillment, independent of `indentStatus`):

| Pill | Condition |
|---|---|
| ✕ Cancelled/Rejected | indentStatus CANCELLED/REJECTED |
| ⊘ Short Closed | indentStatus SHORT CLOSED |
| ✓ Fully Received | `indentStatus==='CLOSED'` OR `receivedQty >= requestedQty > 0` |
| 📦 Fully PO'd | `indentStatus==='PO COMPLETED'` OR `poQty >= requestedQty` — but received < requested |
| ◑ Partially Received | some received, not all, no full PO coverage |
| 🔄 PO in Progress | pendingQty in PO but 0 received |
| ○ Not Started | no PO, no receipt |

**Invariant:** `PO COMPLETED` ≠ goods received. `isFullyReceived` must require `receivedQty >= requestedQty > 0` OR `CLOSED` (previous bug: it wrongly included `PO COMPLETED`). `isFullyPOd` is separate (`sum(line.poQty) >= totalRequested`).

## Session 6 — Report page defaults

All report list pages open with `pageSize = 100` (override `ListCommon` default 12): FifoReport, StockAging, LowStock, GlobalStockReports/{DeadStock,ExpiredStock}, PoReconciliation, IndentFulfillment.

---

# BOQ — Planned-Feature Write-ups (Sessions 6–7, now implemented)

## Unified BOQ Report / BOQ End-to-End Tracker (design notes)

Single page: BOQ Planned vs Indent vs Outward, drillable product → building type → structure → final location. Indent only aggregates at product level (no location breakdown — `indent_inventory_entries` has no location FKs).

Entity relationships (confirmed from code):

| BOQUpload field | FK → | Table |
|---|---|---|
| `buildingTypeId` | `BuildingType.id` | `building_type` |
| `usageLocationId` | `UsageLocation.id` | `Usage_Location` |
| `locationId` | `UsageArea.id` | `usage_area` |
| `productId` | `Product.productId` | `product` |

`OutwardInventory`: `locationId` → `UsageLocation.id` (structure, matches BOQ `usageLocationId`), `usageAreaId` → `UsageArea.id` (final location, matches BOQ `locationId`). Join outward → building type: `outward_inventory.locationId → Usage_Location.id → Usage_Location.typeId → building_type.id`.

Backend: `GET /api/inventory/boqupload/boq-tracker`, `@Transactional(TxType.NOT_SUPPORTED)`, EntityManager native queries. Frontend `Modules/Reports/BOQTracker/index.js` accordion, route `/boqTracker`. Gap filters: BOQ-no-indent, BOQ-no-outward, indent-no-BOQ, no-plan-has-activity.

---

# Quote Comparison Feature

Buyer collects multiple vendor quotes for demand lines (from indents), compares side-by-side, awards each line to a winner, turns award into a PO — indent reflects an RFQ is in flight. Master-schema (`@UseDefaultTenant` on `QuoteComparisonService`).

## Entities

| Entity | Table | Notes |
|---|---|---|
| `QuoteComparison` | `quote_comparison` | Header. `qcId` PK (custom, `QC-1`). status enum. |
| `QuoteComparisonLine` | `quote_comparison_line` | One per demand line. `indentId`/`indentLineId` → source indent line (`lineItemCode`). `lineStatus`: OPEN → FINALIZED → PO_LINKED. |
| `ComparisonCriteria` | `comparison_criteria` | Custom columns. `criteriaScope`: LINE or HEADER. |
| `SupplierQuote` | `supplier_quote` | One per vendor *round*. `revisionLabel` (R-0, R-1…), auto-numbered via `nextRevisionLabel`. |
| `SupplierQuoteLine` | `supplier_quote_line` | Per-line: rate, qty, discount, GST, freight, computed `landedCost`. |
| `SupplierQuoteCriteriaValue` | `supplier_quote_criteria_value` | Either `supplierQuoteLine` (LINE) or `supplierQuote` (HEADER) set, never both. |
| `QuoteToPoRef` | — | Audit: which PO/PO-line a finalized line became. |

## Status state machine (`recalcStatus`, re-run after every quote/finalize/link/delete)

| Status | Set when |
|---|---|
| `DRAFT` | No SupplierQuote yet |
| `OPEN` | ≥1 quote, every line still OPEN |
| `PARTIALLY_FINALIZED` | Mix of open + decided lines |
| `FINALIZED` | Zero OPEN, none PO_LINKED |
| `PARTIALLY_ORDERED` | Zero OPEN, some (not all) PO_LINKED |
| `PO_COMPLETED` | All lines PO_LINKED |
| `CLOSED` | Manual, sticky, reversible via `reopen()` |
| `CANCELLED` | Manual, sticky, permanent (blocked if any line PO_LINKED) |

`reopen()` only from CLOSED (not CANCELLED): flips to OPEN then `recalcStatus` recomputes the real status.

## Indent linkage — "Quote Requested" marker

`IndentInventoryList.quoteRequestedQcId` (col `quote_requested_qc_id`) — **independent of `lineItemStatus`** (which is owned by PO/inward pipeline and recomputed on every inward sync via `IndentInventoryAsyncUpdater.recalculateIndentLine`). Separate additive column so it isn't wiped.

- **Set**: `create()` → `markIndentLinesQuoteRequested()` by `indentLineId` (=indent `lineItemCode`), best-effort.
- **Cleared** (best-effort, only "if marker still points at *this* QC" via `clearIndentLineMarker(code, qcId)`):
  - `cancel()` → clears all lines.
  - `close()` → clears all lines (Session 8: reversed earlier "close doesn't clear").
  - `linkToPo()` → clears just that one line.
- **Re-set on `reopen()`**: `remarkIndentLinesIfFree()` — only where marker null, not for PO_LINKED lines.
- **Terminal statuses non-blocking**: `validateIndentLinesNotAlreadyQuoted()` + `isTerminalStatus()` exempt CANCELLED/CLOSED/PO_COMPLETED.
- **`deleteSupplierQuote()`** has same orphan guard as `updateSupplierQuote()` — blocks removing a quote that's the winning ref for a FINALIZED/PO_LINKED line.
- **Propagated on indent split**: `IndentInventoryService.splitLineItem()` copies marker to both children. Residual edge case: quoted line split *then* QC cancelled — cancel's lookup-by-original-`lineItemCode` misses the re-coded children.
- **One indent line, one active (non-cancelled) comparison** — enforced proactively (`Step1SelectIndents` grays already-quoted rows when `disableAlreadyQuoted` prop passed — QC creation only) + reactively (`validateIndentLinesNotAlreadyQuoted()`).
- **Indent list filter**: "Quote Requested" Yes/No → `hasQuoteRequested` → `IndentInventorySpecification.lineItemHasQuoteRequested()` (EXISTS/NOT EXISTS). "No" = zero lines quoted.
- **Badges**: aggregate badge in `Indent/table.js` status cell (stacked vertically under main status); per-line "Quote Req." chip in list expansion + `Indent/details.js`.

## Create-PO integration (bidirectional)

- **From QC**: Overview "Ready for Purchase Order" card per winning supplier-quote group (`getFinalizedPoGroups()` groups FINALIZED rows by `supplierQuoteId`). "Create PO" → `buildQuotePrefill` (`Shared/quoteToPo.js`) → hands off via **sessionStorage** → PO step 2, pre-filled with `_linkedQcLineId`/`_linkedSupplierQuoteLineId`/`_linkedQcId` markers.
- **From PO**: "Load from Quote" in `PurchaseOrder/list.js` → `add/LoadFromQuoteDialog.js` (comparison → vendor group picker), sets state directly. Only lists comparisons in FINALIZED/PARTIALLY_FINALIZED/PARTIALLY_ORDERED — **keep this list in sync when adding a new header status** (missed for PARTIALLY_ORDERED once).
- **On PO save**: `add.js` create-success loops `state.items` for `_linked*` markers, calls `quoteComparisonLinkToPo` each (sets PO_LINKED + `recalcStatus`; guarded against double-link).
- **Editing a finalized SupplierQuote is blocked**: `updateSupplierQuote()` deletes+recreates all lines with new IDs — would orphan a winning ref. Blocked with product names, points at "Reopen".
- **Gotcha — sessionStorage not router state**: `Home/index.js` does `key={new Date()}` on routed components → remounts on every parent re-render, wiping `history.push(url, state)`. Use sessionStorage (set in `details.js`, read in `PurchaseOrder/index.js` componentDidMount, cleared on wizard `back()`).
- **Gotcha — multi-project indent strings**: QC `project` can be comma-joined across projects. `buildQuotePrefill` only carries it into PO prefill when single-valued, else leaves Project blank.

## Comparison Matrix UX (`comparisonMatrix.js`)

Vendor summary cards (rank, total landed cost, payment terms, lead time, header-criteria chips), cheapest-first. Auto-narrows past 5 vendors (top 3 in table, checkboxes to add/remove); computed once via `useState(() => ...)` at mount (doesn't recompute mid-tab). Sticky left columns use hardcoded pixel `left` offsets (`STICKY_WIDTHS`) — keep in sync with content widths. `getFinalizedPoGroups`/`buildPoItemsFromGroup`/`buildQuotePrefill` in `Shared/quoteToPo.js`, shared by `details.js` + `LoadFromQuoteDialog.js`.

## RFQ PDF

`QuoteComparisonPdfService` (iText, same as `PurchaseOrderPdfService`) — `GET /quote-comparison/{qcId}/rfq-pdf`. Logo `sc-login-logo.png`. Demand lines only (no rate data at RFQ stage).

## Files

Backend: `service/QuoteComparisonService.java`, `service/QuoteComparisonPdfService.java`, `controller/QuoteComparisonController.java` (`/quote-comparison`), `Filters/IndentInventorySpecification.java`, `data/ConsolidatedIndentLineDTO.java` (+`quoteRequestedQcId`), `model/IndentInventoryList.java` (+`quoteRequestedQcId`).
Frontend: `Modules/QuoteComparison/{list,details,create,filter,comparisonMatrix,supplierQuoteForm}.js`, `Shared/quoteToPo.js`, `PurchaseOrder/add/LoadFromQuoteDialog.js`, `PurchaseOrder/add/step1SelectIndents.js` (`disableAlreadyQuoted`), `Indent/{table,details,filter,list}.js`.

## Migrations (additive, masterschema)

`supplier_quote.revision_label`, `comparison_criteria.criteria_scope`, `supplier_quote_criteria_value.supplier_quote_id` (new) + `supplier_quote_line_id` nullable, `indent_inventory_entries.quote_requested_qc_id`.

## Known limitations

Split-then-cancel marker edge case; `LoadFromQuoteDialog` no pagination (first 100); no automated tests (manual UI + direct API against local dev DB).

---

# Service Order — Line-Level Status (Session 9)

`ServiceOrderLine.status` (col `status`, nullable, defaults `NEW` in Java) — per-line `NEW → COMPLETED | CANCELLED`. `ServiceOrderLine.cancelReason` added.

Header status **derived** via `ServiceOrderService.recomputeHeaderStatus()` (never set directly):
- all NEW (or none) → NEW
- all CANCELLED → CANCELLED
- all terminal with ≥1 COMPLETED → COMPLETED
- any mix with NEW remaining → `PARTIALLY_COMPLETED` (header-only status in `ServiceOrderStatusConstants`; not a line status)

Runs after create/update/complete/cancel (whole-order + per-line). `isLineTerminal()` guards decided lines. Mixed completed+cancelled (no NEW) → COMPLETED by design.

Endpoints: `POST /service-order/{id}/line/{lineId}/{complete|cancel|reopen}` (complete: warrantyTill+nextServiceDate; cancel: reason; reopen: back to NEW, clears cancelReason + completion warranty/nextServiceDate). Whole-order `/{id}/complete` + `/{id}/cancel` now act on all still-open lines, then derive. `refreshNextServiceDate()` = earliest next-service across non-cancelled lines, clears to null when none.

Frontend `ServiceOrder/details.js`: per-line Status col + Complete/Cancel/Reopen buttons; action col shows when `canManage`; whole-order "Complete/Cancel All Open" gated `hasOpenLines`; Edit gated to pristine NEW; `lineStatusOf(line)` treats null as NEW. In-place refresh via local `state.data` + `getServiceOrderDetail` after each action. `list.js`/`filter.js` add `PARTIALLY_COMPLETED`.

Migration: `service_order_line.status` additive (nullable so NOT NULL DDL can't fail); existing rows backfilled from header status via `PendingDeployMigrations.sql` (masterschema).

---

# BOQ "Indented" Definition — Corrected (Session 10)

"Indented" in BOQ reports now = every indent line really raised. Old logic wrongly dropped SHORT CLOSED lines (Cement drgtrdcntr showed 10640 vs real 18840).

New filter (all 3 sites in `service/BOQService.java`): exclude header `indent_status IN (CANCELLED, REJECTED)` + line `line_item_status = CANCELLED` only. SHORT CLOSED counted.
- `getBOQIndentSummary()` — BOQ vs Indent report (`/boqIndentReport`)
- `getBOQTrackerSummary()` — BOQ Tracker (`/boqTracker`) + Excel export
- `getProductBOQSummary()` — "BOQ Remaining" chip on Indent create form + details

Status breakup (BOQ vs Indent report only): `BOQIndentSummaryItem` gained `totalReceived`, `totalPending`, `totalShortClosed`. Identity **Requested = Received + Pending + ShortClosed**; `ShortClosed = SUM(GREATEST(quantity - quantity_received, 0))` over SHORT CLOSED lines; `Pending` derived in Java. Frontend `Modules/Reports/BOQIndent/index.js` — expandable rows + "Expand all", 4-chip strip. BOQ Tracker has no breakup (by design). No DB migration.

Tracker "Inward Received" source changed: `getBOQTrackerSummary()` previously summed physical `inward_inventory`/`inward_outward_entries` (all stock, incl. direct inward + non-indent POs → 9838). Now sums indent-line `quantity_received` (master, same non-cancelled filter) → only indent→PO→inward flow (7540), uniform with the report. Old physical-inward query removed; `inwardMap` populated from indent query r[2]. Global BOQ Tracker inherits the fix.

---

# Billing Unit Conversion — Direction-Aware Config + In-Place Edits

Product "billing unit conversions" let a PO line be priced/shown in an alternate unit (e.g. bill a
NOS product in kg). `ProductUnitConversion` (master, `product_unit_conversions`) stores
`conversionFactor` **canonically = base units per 1 billing unit**; the PO billing qty is always
`baseQty / conversionFactor` (`SC UI/.../PurchaseOrder/add/step2FillDetails.js`). The backend only
persists what the UI computes (`PurchaseOrderBuilder` / `updatePurchaseOrder` trust frontend
netRate/total/grandTotal).

### The bug that triggered this (PO-1287)
GI Recessed Cover (base NOS) had a kg conversion entered as `85` meaning "85 kg per pc". But the field
means base-per-billing (pcs per kg), so the UI computed `50 / 85 = 0.59 kg` and the line collapsed to
₹4,231 instead of ₹3.58L. Root cause = **data-entry direction inversion, not a code bug** — the divide
formula is correct for the field's definition. PO-1287 line 3353 was patched directly in prod DB
(rate 143/kg, billing_qty 4250, netRate 303875, total 358572.50, grandTotal 915414.50).

Prod data showed the field can't have one fixed direction: when the billing unit is **bigger** than
base (Tmt Bar KG→BUNDLE 75, Paint LTR→BUCKET 20) "1 bundle = 75 kg" is natural (divide works); when
**smaller** (Cover NOS→kg, Sheet Roll→kg) users fought the direction with garbage factors
(0.9166, 1000, 0.0105…). So the fix is **direction-aware entry**, not a blind invert.

### Design
Storage stays canonical (base-per-billing, divide) → **PO calc and all consumers untouched**. A new
additive column `display_direction` (`BASE_PER_BILLING` | `BILLING_PER_BASE`, null = legacy
`BILLING_PER_BASE`) records only how the user enters/views it. UI converts on save:
`BASE_PER_BILLING` ("1 base = N billing", e.g. 1 pc = 85 kg) → stores `1/N`; `BILLING_PER_BASE`
("1 billing = N base") → stores `N`.

### Backend
| File | Change |
|---|---|
| `model/ProductUnitConversion.java` | + `display_direction`, `reference_unit`, `reference_value` columns (additive → auto-apply) |
| `data/ProductUnitConversionDTO.java` | + `displayDirection` |
| `service/ProductUnitConversionService.java` | direction on add; new `updateConversion` (factor/direction always editable; unit rename blocked while used in a PO) |
| `controller/ProductController.java` | + `PUT /product/{id}/unit-conversions/{conversionId}` |
| `data/UpdateLineBillingRequest.java` | new (rate, billingUnit, billingQuantity, billingConversionFactor) |
| `service/PurchaseOrderService.java` | new `updateLineBilling` — guarded by `validateAddLineToPO` (non-terminal only), **recomputes netRate = rate·effQty·(1−disc); total = netRate·(1+gst); recalculateGrandTotal** so a bad frontend value can't slip through |
| `controller/PurchaseOrderController.java` | + `PUT /purchase-order/{id}/line/{lineId}/billing` |

### Frontend
| File | Change |
|---|---|
| `endpoints.js` | + `updateUnitConversion`, `updatePOLineBilling` |
| `Modules/Product/UnitConversionConfig.js` | direction radio (`1 base = N billing` ↔ `1 billing = N base`) + per-row **edit**; stores canonical, displays natural phrasing |
| `Modules/PurchaseOrder/POLineBillingDialog.js` | new dialog — pick billing unit, edit rate, live net/total preview; `billingQty = baseQty / factor` |
| `Modules/PurchaseOrder/details.js` | ⇄ button per line (shown in NEW/PARTIAL = non-terminal), opens dialog, refreshes PO via returned `localData` |

### Chained conversions — define a unit against ANY unit (not just base)
A conversion can be entered relative to another unit, e.g. base = NOS, "1 box = 600 NOS", then
"1 carton = 10 box" (instead of "6000 NOS"). Resolved **at entry time** in the config UI:
`refFactor` = the reference unit's canonical factor (1 for base); `conversionFactor` (this unit) =
`BASE_PER_BILLING ? refFactor/value : value*refFactor`. So carton stores canonical `6000` — the PO
calc still does one `baseQty / 6000`, no runtime chain walk. `reference_unit` (null = base) +
`reference_value` are stored **display/edit only**. UI adds a "Defined against" dropdown (base + other
units, self excluded). **Ceiling (snapshot):** editing box does NOT recompute carton — re-edit carton.
Chained units simply appear as normal billing-unit options in `POLineBillingDialog` (canonical factor).

### Invariants / gotchas
- **Non-terminal = NEW/PARTIAL** for the PO-line billing edit (backend blocks CANCELLED/COMPLETED/
  SHORT CLOSED via `validateAddLineToPO`; details.js action column already gated NEW/PARTIAL).
- Editing a product's conversion does **not** retroactively change existing PO lines — lines snapshot
  their own `billingQuantity`/`billingConversionFactor`.
- No PendingDeployMigrations entry — only the additive `display_direction` column. Legacy rows read as
  `BILLING_PER_BASE`; historically-wrong rows (cover=85, roll=91.66) are now self-fixable via the edit UI.
- Verified via `mvn compile` + eslint; not click-tested (no auto server start).

### Product merge interaction (added same effort)
`ProductMergeService` now **blocks merge when base `measurementUnit` differs**
(`validateMeasurementUnitCompatibility`, case/space-insensitive, in both `preview` + `execute` —
mirrors `validateBatchModeCompatibility`). `ProductMergeTenantExecutor` no longer **reassigns**
`product_unit_conversions` source→target (that produced duplicate/conflicting billing units, e.g. two
"kg" rows on the survivor); it now **deactivates the source's** rows (`is_active=false`), keeping the
survivor's config. Existing PO lines snapshot their own billing so they're unaffected.

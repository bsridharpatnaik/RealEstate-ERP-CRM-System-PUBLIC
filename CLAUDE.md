# ERP/CRM Project — Claude Code Context

## Project Overview

Real Estate ERP/CRM system for managing:

- inventory
- indents
- purchase orders
- inward / outward movements
- stock operations
- real-estate project material workflows

Primary goal: fast, safe, minimal-impact changes aligned with existing architecture.

---

## Repository Root

`/Users/bsridharpatnaik/GitHub/RealEstate-ERP-CRM-System`

---

## Active Scope (Default)

Only analyze and modify these paths unless explicitly asked otherwise:

- `sc-inventory-service/src`
- `SC UI/src`

Ignore by default:

- `sc-crm-service`
- `sc-common-service`
- build folders
- generated files
- logs
- unrelated directories

Do not scan outside active scope unless required.

---

# Project Structure

## Backend

### Inventory Service

Path: `sc-inventory-service/src`

Stack:

- Java
- Spring Boot
- JPA / Hibernate
- MySQL
- Multi-tenant architecture

Domains include:

- Inventory
- Indents
- Purchase Orders
- Inward
- Outward
- Stock

---

## Multi-Tenant Schema Rules

**Critical — understand before debugging any data issue:**

| Entity | Schema |
|---|---|
| `IndentInventory`, `IndentInventoryList` | **Master schema** (`@UseDefaultTenant` on `IndentInventoryService`) |
| `PurchaseOrder`, `PurchaseOrderLine` | **Master schema** (`@UseDefaultTenant` on `PurchaseOrderService`, `PurchaseOrderLifecycleManager`) |
| `IndentStatusUpdater` | **Master schema** (`@UseDefaultTenant` at class level) |
| Inward, Outward, Stock, LostDamaged, MOR, InventoryTransfer | **Project (tenant) schema** |
| Activity logs | **Project schema** (per-tenant) + synced to master `global_activity_log` |

**Do NOT assume all entities are in project schema.** Indents and POs are global (master schema) entities accessible across all projects. Inward/Outward/Stock are project-specific.

When a tenant context issue is suspected — check which schema the entity lives in first.

## Frontend

Path: `SC UI/src`

Stack:

- React
- JavaScript / TypeScript

Domains include:

- Inventory UI
- Indent management
- Purchase Order screens
- Stock dashboards
- Forms / tables / reports

---

# Core Working Rules

## General

- Prefer understanding existing implementation before proposing new code.
- Reuse existing patterns, naming style, and architecture.
- Prefer minimal diffs over rewrites.
- Modify existing files before creating new files.
- Keep solutions production-safe and maintainable.
- Avoid unnecessary complexity.

## Never Do These Unless Asked

- Introduce new frameworks
- Add libraries
- Refactor unrelated code
- Rename working modules
- Change API contracts unnecessarily
- Change DB schema by assumption
- Modify out-of-scope modules
- **Commit or push code** — always show changes and wait for explicit approval before running any `git commit` or `git push`

---

# Code Search Strategy

For every task:

1. Start with Graphify outputs when useful.
2. Search only in allowed paths.
3. Find existing similar feature first.
4. Reuse existing patterns.
5. Expand search only if blocked.
6. Avoid broad repo scans.

---

# Graphify

Knowledge graph exists at:

`graphify-out/`

## Rules

Before architecture or codebase questions:

1. Read `graphify-out/GRAPH_REPORT.md`
2. If available, prefer `graphify-out/wiki/index.md`

After code changes in session, refresh graph:

```bash
python3 -c "from graphify.watch import _rebuild_code; from pathlib import Path; _rebuild_code(Path('.'))"
```

---

# Activity Log Feature — Completed Work

## What Was Built

Full activity log system — per-project logs synced to a global cross-tenant log.

---

## Architecture

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

### Critical Bug — Tenant Context

**Root cause**: `ProductService` is `@UseDefaultTenant` at class level. Any call to it (e.g. `findSingleProduct()` inside `populateData()`) permanently sets `ThreadLocalStorage` to master schema for the rest of the request thread. Any subsequent `activityLogService.record()` call reads master from ThreadLocal → log saved to master.

**Fix — `TenantAspect.java`**: Changed from `@Before` to `@Around`. Now saves the original tenant before switching to master, and restores it in `finally` after the annotated method returns. This means `@UseDefaultTenant` only affects the duration of the annotated method — not the rest of the calling thread.

```java
@Around("@annotation(UseDefaultTenant) || @within(UseDefaultTenant)")
public Object applyDefaultTenant(ProceedingJoinPoint pjp) throws Throwable {
    String originalTenant = ThreadLocalStorage.getTenantName();
    tenantService.setDefaultTenant();
    try {
        return pjp.proceed();
    } finally {
        ThreadLocalStorage.setTenantName(originalTenant);
    }
}
```

**Do NOT** revert this to `@Before`. The save/restore is essential.

`ActivityLogService.record()` is synchronous (no `@Async`). It reads tenant from ThreadLocal (now correct after the aspect fix) and delegates the actual async DB write to `ActivityLogWriter.saveAsync(entry, tenant)` with tenant passed explicitly as a safety net.

---

## Entity Types Logged

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

Entity type for lost/damaged uses the actual `entryType` field value (`LOST_DAMAGED` or `EXCESS_FOUND`).

---

## Description Format (JSON)

All descriptions stored as JSON. Frontend parses and renders as mini-table.

```json
// CREATE / REJECT / RETURN — single qty
{"summary": "Inward 7096 created by sridhar", "items": [{"product": "Steel", "qty": "10"}]}

// UPDATE — old and new qty
{"summary": "Inward 7096 updated by sridhar", "items": [{"product": "Steel", "oldQty": "5", "newQty": "10"}]}

// No items (header-only update / delete)
{"summary": "Inward 7096 deleted by sridhar"}
```

UPDATE logs for inward/outward are consolidated — ONE log per update operation with all changed lines, not one log per line.

---

## Frontend — key files

| File | Role |
|---|---|
| `Modules/Activity/list.js` | Global activity log page — fetches from `/global/list`, has Sync Now + Export |
| `Modules/Activity/projectList.js` | Per-project activity log page — fetches from `/list` |
| `Modules/Activity/filter.js` | Global filter — all entity types + Project dropdown (tenantCode sent to backend) |
| `Modules/Activity/projectFilter.js` | Project filter — excludes INDENT, PURCHASE_ORDER, no tenant field |
| `Modules/Activity/table.js` | Shared table — renders description via `renderActivityDescription` |
| `Modules/Activity/renderActivityDescription.js` | Parses JSON description, renders mini-table (Product/Qty or Product/OldQty/NewQty) |
| `Modules/InwardInventory/details.js` | History tab shows activity logs via `activityLogByEntity('INWARD', id)` |
| `Modules/OutwardInventory/details.js` | History tab shows activity logs via `activityLogByEntity('OUTWARD', id)` |

### Routes

- `/activityLog` — global log (admin only, global menu)
- `/projectActivityLog` — per-project log (admin only, project menu)

`/activityLog` must be in `isProjectSelectionPage` list in `SideMenu/index.js` — otherwise Redux tenant state causes it to render in project context.

### Project Dropdown in Global Filter

`list.js` fetches tenants via `API.GET(apiEndpoints.getTenants)`, maps to `{ name: tenantName, id: tenantCode }`. `prepareRequestBody` extracts `.id` (tenantCode) for `tenantSchema` filter. Backend matches against `global_activity_log.tenantSchema` which stores the schema code.

---

## resolveCurrentUser Pattern

Every service that logs must have:

```java
private String resolveCurrentUser() {
    try { return userDetailsService.getCurrentUser().getUsername(); }
    catch (Exception e) { return "System"; }
}
```

Call it BEFORE `activityLogService.record()`. Username must be resolved on the calling thread.

---

## Local Database Access

- **Host:** 127.0.0.1
- **User:** root
- **Password:** REDACTED
- **Connect:** `mysql --user=root --password=REDACTED --host=127.0.0.1`
- Tenant schemas named after tenant code (e.g. `drgtrdcntr`). Master schema: `masterschema`.

---

# Batch Tracking Feature — Completed Work

## What It Does

Tracks physical batches of stock per warehouse. Enables FIFO/FEFO-based outward consumption, write-offs, split of untracked stock into batches, and editing of batch metadata. Lives in project (tenant) schema.

---

## Core Concepts

### BatchMode enum
```
NONE            — product is not batch-tracked
BATCH_ONLY      — tracked; FIFO ordering by receivedDate
BATCH_WITH_EXPIRY — tracked; FEFO ordering by expiryDate
```
`isBatchTracked()` = mode is not NONE. UI shows Batches tab only when batchMode ≠ NONE.

### InventoryBatch entity (`model/InventoryBatch.java`, table: `inventory_batch`)

| Field | Notes |
|---|---|
| `batchId` | PK |
| `product` | ManyToOne |
| `warehouse` | ManyToOne |
| `inwardId` | FK to inward; **-1 means created via stock split** (not linked to an inward) |
| `brand` | Editable metadata — supplier brand / grade |
| `lotNumber` | Editable metadata — supplier lot / batch reference |
| `expiryDate` | Editable; `dd-MM-yyyy`; null OK for BATCH_ONLY |
| `receivedDate` | Editable; `dd-MM-yyyy`; NOT NULL in DB |
| `qtyReceived` | Set at creation; never changed |
| `qtyRemaining` | Reduced by outward consumption and write-offs |
| `isDeleted` (inherited) | Soft delete via `ReusableFields` |
| `daysUntilExpiry`, `isExpired` | `@Transient` — computed at query time |

### OutwardBatchConsumption entity (`model/OutwardBatchConsumption.java`, table: `outward_batch_consumption`)

| Field | Notes |
|---|---|
| `outwardId` | FK to outward inventory header |
| `productId` | Denormalised product ID |
| `batch` | ManyToOne → InventoryBatch |
| `qtyConsumed` | Qty taken from this batch for this outward line |
| `fifoOverridden` | true if user manually picked batches instead of auto-FIFO/FEFO |
| `overrideComment` | Reason for override (nullable) |

### hasFifoOverride flag (`OutwardInventory.hasFifoOverride`)
Boolean on the outward header. Set to `true` during save/update if ANY consumption row has `fifoOverridden = true`. Must be explicitly reset to `false` at start of update path — otherwise stale `true` persists.

---

## FIFO / FEFO Ordering Logic

- `BATCH_ONLY` → order batches by `receivedDate` ASC (oldest first)
- `BATCH_WITH_EXPIRY` → order batches by `expiryDate` ASC (nearest expiry first)
- Override detection checks **both batch ID sequence AND qty per batch**. If user selects correct order but wrong quantities → still an override.

### How `isFifoOverride` is determined (in both `OutwardInventoryService` and `BatchTrackingService`):
1. Build `fifoBatchIds` list — sorted batch IDs in FIFO/FEFO order for that product+warehouse
2. Build `fifoExpectedQty` map — how much each batch *should* contribute under pure FIFO (fills from oldest until qty met)
3. For each override entry from user: check `!isFifoOrder || !isFifoQty`

---

## Untracked Stock

Stock where `totalQuantityInHand > sum(batch.qtyRemaining)` for batch-tracked products. This happens when inward is done but batch details weren't entered, or qty was adjusted outside batch tracking.

- **Blocking**: outward is blocked if any untracked stock exists in the target warehouse
- **Resolution**: user must Split untracked stock into named batches via the Split panel
- **Split batches** have `inwardId = -1` and show a "split" badge with tooltip in UI

### Stock Tiles (`StockTilesDTO`)
Returned by `BatchTrackingService.getStockTiles()`. Fields:
- `expiredCount`, `nearExpiryCount` (30-day window), `untrackedCount`
- All are clickable filter tiles on the Stock list page
- `untrackedCount` uses `expiryFilter = "untracked"` in stock request body

---

## Key Backend Files

| File | Role |
|---|---|
| `model/InventoryBatch.java` | Batch entity |
| `model/OutwardBatchConsumption.java` | Per-outward batch consumption record |
| `model/BatchWriteOff.java` | Write-off event record |
| `service/BatchTrackingService.java` | Core logic: FIFO consumption, split, write-off, batch edit, tiles |
| `service/OutwardInventoryService.java` | Calls batch tracking during create/update/return/delete of outwards |
| `repository/InventoryBatchRepository.java` | Includes `sumQtyRemainingGroupByProduct`, `findAllByInwardId` |
| `repository/OutwardBatchConsumptionRepository.java` | `deleteByOutwardId` (bulk JPQL `@Modifying`), `findByOutwardIdAndBatch_BatchId` |
| `repository/ProductRepo.java` | `findBatchTrackedProductIds()` — returns IDs where batchMode ≠ NONE |
| `repository/StockInformationRepo.java` | `findByProductIdInAndTotalQuantityInHandGreaterThan` |
| `controller/BatchTrackingController.java` | All batch API endpoints |
| `data/BatchUpdateRequestDTO.java` | `brand`, `lotNumber`, `expiryDate`, `receivedDate` for PUT /batch/{id} |
| `data/WriteOffRequestDTO.java` | `quantity`, `reason`, `writeOffDate` |
| `data/StockSplitRequest.java` | `warehouseId`, `batches[]` |
| `data/StockTilesDTO.java` | `expiredCount`, `nearExpiryCount`, `untrackedCount` |

---

## API Endpoints (all under `/api/inventory`)

| Method | Path | Description |
|---|---|---|
| `PUT` | `/batch/{batchId}` | Edit batch metadata (brand, lotNumber, expiryDate, receivedDate) |
| `POST` | `/batch/{batchId}/write-off` | Write off qty from a batch |
| `GET` | `/batch/{batchId}/write-off/history` | Write-off history for a batch |
| `GET` | `/stock/{productId}/batches?warehouseId=` | Get batches; warehouseId optional (null = all warehouses) |
| `POST` | `/stock/{productId}/split-existing` | Split untracked stock into batches |
| `POST` | `/stock/tiles/expiry` | Get stock tile counts (expired / near-expiry / untracked) |
| `GET` | `/outward/{outwardId}/batch-consumptions` | Get consumption records for an outward |
| `GET` | `/inward/{inwardId}/batches` | Get batches created during an inward |
| `POST` | `/outward/preview-batches` | Preview which batches FIFO would consume |

---

## Key Frontend Files

| File | Role |
|---|---|
| `Modules/Stock/details.js` | Batches tab: per-warehouse sections, split form, write-off form, edit batch form |
| `Modules/Stock/list.js` | Stock list with tile chips (expired / near-expiry / untracked) |
| `Modules/OutwardInventory/add.js` | Batch override UI during outward creation |
| `Modules/OutwardInventory/edit.js` | Preserves override batches when qty unchanged on edit |
| `Modules/OutwardInventory/details.js` | Shows `⚡ FIFO Override` badge + batch consumption table |

---

## Frontend State in `Stock/details.js`

| State key | Type | Purpose |
|---|---|---|
| `batches` | Array | All loaded batches for current product (all warehouses) |
| `batchesLoading` | bool | Loading spinner |
| `splitFormOpenWarehouseId` | null / warehouseId | Which warehouse's split form is open (null = none) |
| `splitEntries` | Array | Rows in the split form `{ qty, expiryDate, brand, lotNumber }` |
| `splitSubmitting` | bool | Split form submit in progress |
| `splitError` | string / null | Validation error in split form |
| `writeOffForm` | null / `{ batchId, warehouseId, quantity, reason }` | Active write-off row |
| `writeOffSubmitting` | bool | Write-off in progress |
| `writeOffHistories` | `{ [batchId]: [...] }` | Loaded write-off history per batch |
| `editBatchForm` | null / `{ batchId, brand, lotNumber, expiryDate, receivedDate, submitting }` | Active edit row |
| `stockAdjustments` | `{ [warehouseId]: number }` | Running write-off totals to keep untracked banner accurate without full reload |

---

## Date Format Convention

Backend `InventoryBatch` serialises/deserialises dates as `dd-MM-yyyy`.
Frontend `<input type="date">` requires `yyyy-MM-dd`.
**Conversion helpers in `Stock/details.js`:**
- `ddmmyyyyToInputDate(s)` — converts `dd-MM-yyyy` → `yyyy-MM-dd` (pre-populate form)
- `toBackendDate = (s) => s.split('-').reverse().join('-')` — converts `yyyy-MM-dd` → `dd-MM-yyyy` (send to API)

Same reverse-join pattern used in split form (`splitEntries` map before POST).

---

## Important Invariants / Known Bugs Fixed

1. **`hasFifoOverride` must be reset to `false` before update** in `OutwardInventoryService` — otherwise a previously-overridden outward that is later edited to pure-FIFO will still show the override badge.

2. **`deleteByOutwardId` must use bulk JPQL `@Modifying`** — entity-by-entity deletion caused `LazyInitializationException` on large outwards.

3. **Return path must update `OutwardBatchConsumption` records** — `restoreBatchesForReturn` restores `qtyRemaining` on batches but must also reduce/delete the matching consumption records; otherwise Batch Usage display shows wrong figures.

4. **FIFO override = order AND qty** — selecting correct batch order but wrong quantities is still an override. Both conditions checked: `!isFifoOrder || !isFifoQty`.

5. **Edit page must reload batch consumptions** — `edit.js` loads `batchConsumptionData` from `/outward/{id}/batch-consumptions` on mount. If qty unchanged and override was present, re-sends `overrideBatches` in PUT payload to preserve it.

6. **Untracked count circular dep** — `BatchTrackingService` already autowires `StockService`. Untracked filter logic in `StockService.expiryFilter` block calls repos directly (not BatchTrackingService) to avoid circular dependency.

---

## Local Database

- Host: `localhost`
- Username: `root`
- Password: `REDACTED`
- Connect: `mysql -h localhost -u root -pREDACTED`

---

## Batch Tracking — Additional Learnings (Session 2)

### `inwardId` Sentinel Values on `InventoryBatch`

Three distinct values — do not confuse:

| Value | Meaning |
|---|---|
| real inward ID (> 0) | Normal inward — batch created via inward save |
| `-1L` | Stock split — batch created via "Split Existing Stock" on Stock/Batches tab |
| `0L` | Inventory transfer — batch created at destination during transfer |

Transfer batches (`inwardId = 0`) have no FK link to a transferId. No way to query "which batches came from transfer X" without a schema change. Transfer details page shows header + items table only; batch metadata is visible in Stock → Batches tab.

---

### Activity Log — Batch-Specific Entity Types Added

Two new entity types now logged (in `BatchTrackingService`):

| entityType | action | trigger |
|---|---|---|
| `WRITE_OFF` | `CREATED` | `writeOffBatch()` — after `batchWriteOffRepository.save()` |
| `STOCK_SPLIT` | `CREATED` | `splitExistingStock()` — after all split batches saved |

Both wrapped in try-catch so a log failure never aborts the operation.

`resolveCurrentUser()` pattern used in `writeOffBatch()` — username captured inside the method before the log call.

For `STOCK_SPLIT`, user is resolved inline (try/catch) since no `@Autowired UserDetailsService` existed in `BatchTrackingService` at the time.

---

### Inward Edit — Batch Metadata Update (delta = 0 path)

`InwardInventoryService.reconcileBatchesForEditedInward()` — modified `if (Math.abs(delta) < 0.001)` block:

**Before:** always `continue` (skip) when qty unchanged.

**After:** if `paq.getBatchSplits()` is non-null and non-empty, iterate splits by position index (split[0] → batch[0], etc.) and update `brand`, `lotNumber`, `expiryDate`. Only updates fields that are non-null in the split. Then `continue`.

Matching is positional — no attempt to match by batch ID or metadata. Simple and safe for typical use (user edits the same batch list they received).

---

### Expiry Alert Notifications — Fix

`AllNotificationService.getInventoryNotification()` previously had no handlers for `EXPIRY_ALERT_30`, `EXPIRY_ALERT_60`, `EXPIRY_EXPIRED`. Notifications were created by scheduled job but rendered as blank messages.

Fixed by adding three `if` blocks after the `lostDamagedStockAdded` block. Also added guard: only add to `inventoryNormalizedNotifications` when `message` is non-empty — prevents unknown types from appearing as blank entries.

Message formats:
- `EXPIRY_ALERT_30` / `EXPIRY_ALERT_60`: `"Product X in Warehouse Y is expiring within N days. Qty remaining: Z."`
- `EXPIRY_EXPIRED`: `"Product X in Warehouse Y has expired. Qty remaining: Z. Please write off expired stock."`

---

### Daily Stock Email — Expiry Sections

Nightly stock report (`sendDailyStockEmailReport`, cron `0 0 21 * * *`) now includes two conditional expiry sections.

**New files/methods:**

| File | Change |
|---|---|
| `data/ExpiryAlertRow.java` | New DTO: `projectName`, `productName`, `warehouseName`, `brand`, `lotNumber`, `expiryDate` (String, formatted `dd-MM-yyyy`), `qtyRemaining`, `unit` |
| `service/StockEmailReportService.java` | `collectExpiryRows(tenantDisplayName, from, to)` — calls `inventoryBatchRepository.findBatchesExpiringBetween(from, to)`, maps to `ExpiryAlertRow`, sorts by expiryDate then productName |
| `scheduled/ScheduledTasks.java` | Builds two date windows (today→+30, +31→+60), collects expiry rows per tenant in same loop as stock data |
| `ReusableClasses/EmailHelper.java` | `sendDailyStockReport` now takes two extra `List<ExpiryAlertRow>` params; adds to Freemarker model as `expiring30` and `expiring60` |
| `controller/AutomaticEmailController.java` | Manual `/email/dailystockreport` trigger updated to match new signature |
| `templates/email-daily-stock.ftl` | Two new `<#if expiring30?has_content>` / `<#if expiring60?has_content>` sections — red header for ≤30d, amber for 31–60d |

**Key points:**
- `InventoryBatch.expiryDate` is Java `Date` type — format with `SimpleDateFormat("dd-MM-yyyy")` before setting on DTO
- Both sections only render when non-empty — no empty tables
- Both sections are cross-project (Project column present)
- Both the cron job and the manual trigger endpoint produce identical output

---

### Inventory Transfer — Details Page (new)

`InventoryTransfer` had no details view. Added:

**Backend:** `apiEndpoints.getInventoryTransferById(id)` → `GET /api/inventory/inventory-transfer/{id}` (endpoint already existed in backend).

**Frontend:**

| File | Change |
|---|---|
| `Modules/InventoryTransfer/details.js` | New component — fetches transfer by ID, shows header info + items table (product, code, unit, qty, source/dest closing stock) |
| `Modules/InventoryTransfer/list.js` | Added `showDetails` / `selectedRow` state, `showDetail(row)` handler, `<Slide>` panel wrapping `<Details>` |
| `SC UI/src/endpoints.js` | Added `getInventoryTransferById: (id) => \`/api/inventory/inventory-transfer/${id}\`` |

Transfer list wrapping div class: `"split"` when details open, `"inventory-transfer-list-wrapper"` otherwise (same pattern as other list/detail pages).

---

### Inward Edit — Batch Modal Now Visible in Edit Mode

Previously `add.js` hid batch split button in edit mode (`{!isEditMode && ...}`). Now shown in both create and edit mode for batch-tracked products.

Button label: `"Set Batches *"` (create) / `"Edit Batches"` (edit, no batches set) / `"View / Edit Batches"` (edit, batches already set).

`loadExistingData()` pre-populates `batchSplits` from `GET /inward/{inwardId}/batches` response — maps `qtyReceived` → `qty`, converts `expiryDate` from `dd-MM-yyyy` → `yyyy-MM-dd` for the date input.

Old brand/expiry inline fields in PO section replaced with `{false && ...}` dead-code block (kept for reference, never renders).

---

# Batch Tracking — Conflict Scenario Analysis (Session 3)

## Business Rules Confirmed

- **Inventory Transfer**: no edit or delete. Read-only after creation. No batch rollback needed.
- **Lost/Damaged (LOST_DAMAGED) and Excess Found (EXCESS_FOUND)**: edit and delete exist in current code but will be **removed** from both UI and backend. Business reason: user corrects a mistake by adding the opposite entry (excess corrects a lost, lost corrects an excess). No need for edit/delete flows.
- **Write-off**: intentionally irreversible. No delete/edit, history only. Correct.

## Stock-Modifying Operations & Batch Impact

| Operation | Stock Effect | Batch Effect |
|---|---|---|
| Inward Create | +qty | Creates `InventoryBatch` records (inwardId = real inward ID) |
| Excess Found | +qty | Increases `qtyRemaining` on existing batch OR creates new batch |
| Inventory Transfer In | +qty at target | Creates batch at target (`inwardId = 0`) |
| Outward Return | +qty | Restores `qtyRemaining` on consumed batches; reduces `OutwardBatchConsumption` |
| Outward Create | −qty | Drains via FIFO/FEFO; creates `OutwardBatchConsumption` records |
| Inventory Transfer Out | −qty at source | Drains batches at source via FIFO |
| Lost/Damaged | −qty | Drains specified batches directly |
| Write Off | −qty | Drains batch directly; `BatchWriteOff` record created |
| Inward Reject | −qty | Drains batches linked to that inward (`inwardId` match) |

## Batch Safety Guards — Current Status

### Inward Delete
`zeroBatchesForDeletedInward()` in `InwardInventoryService` checks every batch from that inward:
- If `qtyReceived - qtyRemaining > 0.001` → throws. Blocks delete.
- Correctly prevents delete when any downstream consumption exists.

### Inward Edit — Qty Reduce
`reconcileBatchesForEditedInward()` reduces batch `qtyRemaining` by delta from last batch backward.
- If reduction exceeds available `qtyRemaining` → throws with clear message.
- Correctly prevents reducing qty below what's already consumed.

### Inward Edit — Qty Increase
Safe. Adds to existing batch or creates new batch. No conflict possible.

### Inward Reject
`addReturnForInward()` drains batches linked to inward.
- Checks `qty > rb.getQtyRemaining()` before draining → throws if batch already consumed via outward.
- Auto-drain path (no override): drains last-to-first; throws if total remaining insufficient.

### Outward Delete
`reverseBatchConsumptions()` restores all `qtyRemaining` on consumed batches and deletes `OutwardBatchConsumption` records. Safe.

### Outward Edit
Calls `reverseBatchConsumptions()` first (full reverse), then `consumeBatchesForOutward()` fresh. `hasFifoOverride` reset to `false` before re-consume. Safe.

### Outward Return
`restoreBatchesForReturn()` restores batches and reduces/deletes matching `OutwardBatchConsumption` records. Both multi-batch and auto-restore paths implemented.

### Untracked Stock Guard
Present on: outward create/edit, lost/damaged create.
NOT verified on: inventory transfer out (may be missing — check `InventoryTransferService` if transfer-out issues arise).

## FIFO Override — Frontend Behavior

**Current (over-restrictive):** `edit.js` disables quantity field for any product where `batchConsumptionData` has any `fifoOverridden === true` consumption. Tooltip says "delete and recreate."

**Correct behavior:** Quantity change should be allowed. Backend already handles:
- Qty changed → `reverseBatchConsumptions()` + fresh `consumeBatchesForOutward()` using pure FIFO. Override is dropped, FIFO applied. Correct.
- Qty unchanged → `edit.js` re-sends same `overrideBatches` in payload, preserving override. Correct.

## FIFO Override — Resolved Behavior (Session 3)

- Qty field lock (`disabled: hasOverride`) removed from outward `edit.js`.
- Single batch override + qty changed → frontend auto-sends `overrideBatches = [{batchId, qty: newQty}]`. Only safe when `consumptions.length === 1 && overriddenConsumptions.length === 1` (full single-batch, not mixed).
- Multi-batch override + qty changed → frontend blocks save with error. User must delete and recreate.
- Qty unchanged + override → re-send existing `overrideBatches` unchanged. Always include fallback `overrideComment` ("Override preserved") since backend requires it.

## Lost/Damaged — Completed (Session 3)

Edit and delete removed from:
- `Lost/index.js` — removed Edit import, edit state, edit prop passed to List
- `Lost/list.js` — removed `edit` and `delete` props from `<Table>` and `<Details>`
- `Lost/details.js` — removed EditIcon, DeleteIcon, DeleteConfirm, edit/delete buttons

## Inward Edit — Batch Reconciliation (Session 3)

### State design in `add.js`

`product.batchSplits` — always stores the original batch data (loaded from backend). Never overwritten by reduce operations. Used for:
- Modal population (add/reduce modes)
- Metadata-only update (when qty unchanged, sent to backend as-is)
- Display summary ("✓ N batches · X units")

`product._reduceSplits` — stores reduce-path delta only. Set by `confirmBatchSplits` in reduce mode. Cleared when user changes qty. Sent to backend as `batchSplits` payload for the reduce path.

`product.batchSplits` (overwritten) — when user confirms increase splits via modal, original batchSplits are overwritten with the delta splits. Display shows delta, not original total.

### Date format contract

All `expiryDate` values inside `product.batchSplits` and `product._reduceSplits` are stored as **`dd-MM-yyyy`** (backend wire format). The modal input converts to/from `yyyy-MM-dd` for `<input type="date">`.

Conversions:
- `loadExistingData`: backend `dd-MM-yyyy` → store as `dd-MM-yyyy` (no conversion)
- `openBatchSplitModal` add mode: `dd-MM-yyyy` → `.split.reverse.join` → `yyyy-MM-dd` for input
- `confirmBatchSplits` increase: input `yyyy-MM-dd` → `.split.reverse.join` → `dd-MM-yyyy` stored in batchSplits
- `confirmBatchSplits` reduce: always `expiryDate: null` (not needed by backend reduce path)
- Payload: `s.expiryDate || null` → sends `dd-MM-yyyy` or null → backend `@JsonFormat(dd-MM-yyyy)` parses correctly

### Payload build for edit PUT

`isReducePath = !!product._reduceSplits`

`rawSplits = _reduceSplits || batchSplits`

When building payload, `batchId` is only included when `isReducePath`. This prevents original splits (which have batchIds from `loadExistingData`) from triggering the `hasSplitsWithBatchId` reduce-detection check on the backend when the user only changed qty without specifying which batches to reduce from.

The backend then correctly throws: "Product X has N batches. Please specify which batch(es) the reduction comes from using 'Edit Batches'."

### Backend guards

`addReturnForInward` — validates `sum(overrideBatches.qty) == quantity` before draining. Prevents batch/stock divergence when user sends partial override entries.

`reconcileBatchesForEditedInward`:
- delta = 0: metadata update (brand/lot/expiry) by position. batchId ignored.
- delta < 0, single batch: auto-reduce. No user input needed.
- delta < 0, multi-batch, no splits with batchId: throws clear error.
- delta < 0, multi-batch, splits with batchId: validates sum == reduction, then drains per batch.
- delta > 0, no splits: throws "specify batch for new qty."
- delta > 0, splits: validates sum == delta, creates/merges batches.

## External Review Findings — Fixed (Session 3)

| # | File | Issue | Fix |
|---|---|---|---|
| 1+7 | `InventoryTransferService` | Override: total check was AFTER individual batch saves → partial deductions survived total validation failure. Catch block compensated stock but NOT batch changes. | Two-pass: validate+sum before any saves; catch block now reverses source batch deductions and soft-deletes partially-created target batches. `createTargetBatchesForTransfer` returns `List<Long>` of created IDs. |
| 2 | `OutwardInventoryService.restoreBatchesForReturn` | No sum check, no product ownership, no outward ownership, no upper-bound, no duplicate check on explicit returnBatches. | Added all five: filter valid entries, dedup, sum==quantity, product match, outward consumption match, qty ≤ consumed. Two-pass: validate then apply. |
| 3 | `ReturnProduct.js` | Frontend validation only required "any" batch qty > 0. Sum could be 10 with return qty 50. | `checkValidation` now requires `sum(batchReturnQtys) == returnquantity`. Added live Allocated/remaining indicator in UI. |
| 5 | Outward create, transfer override, inward reject, lost/damaged | Batch ownership only checked warehouse (or nothing). Product ownership not validated anywhere except outward create (now). | Added `batch.product == productId` check in outward override, transfer override (pass 1), inward reject override, lost/damaged single+multi-batch. Added warehouse check to L/D and inward reject. |
| 9 | `LostDamagedInventoryService.handleBatchOnCreate` | Null-batchId entries counted in sum, skipped in loop → stock adjusted by full qty but batch adjusted by less. Duplicates not checked. Same bug in EXCESS_FOUND path. | Filter to valid entries first, then sum. Duplicate check. Ownership check. Two-pass: validate then apply. Both LOST_DAMAGED and EXCESS_FOUND paths fixed. |
| — | `InventoryTransferService.deductSourceBatchesForTransfer` | No product or warehouse ownership checks on override batches. | Added in pass 1: `batch.product == productId` and `batch.warehouse == sourceWarehouseId`. |

## Key Invariant

`batch.qtyRemaining` must equal `batch.qtyReceived` minus sum of all downstream consumptions (outwards, lost/damaged, write-offs, transfer-outs, rejects). Any operation that creates or reverses consumption must update `qtyRemaining` atomically in the same transaction.

---

# FIFO Override Report — Completed Work

## What It Does

Global cross-tenant report showing all outward transactions where batch consumption deviated from FIFO/FEFO order (i.e. `fifo_overridden = true` on `OutwardBatchConsumption`). Admin-only. Accessible from global side menu under Reports → FIFO Override Report.

---

## Architecture

### Sync Strategy — Incremental

Same pattern as `ActivityLogGlobalSyncService`. Tracks `MAX(syncedAt)` per tenant in master table.

- First run: loads all override rows for tenant
- Subsequent runs: only processes rows where `lastModifiedDate > lastSyncTime`
- Deleted source rows (outward deleted/soft-deleted): removed from master
- Auto-sync: hourly cron (`FifoReportSyncJob`)
- Manual sync: POST `/api/inventory/fifo-report/sync` → "Sync Now" button in UI

### Master Table: `global_fifo_report`

Unique constraint: `(tenantSchema, outwardId, batchId, productId)`

Stored fields (denormalized from tenant schema at sync time):
- `tenantSchema`, `outwardId`, `outwardDate`
- `productId`, `productName`, `productCode`, `measurementUnit`
- `warehouseId`, `warehouseName`
- `usageLocationName` (structure), `usageAreaName` (final location)
- `contractorName`, `purpose`
- `batchId`, `batchLotNumber`, `batchBrand`, `batchReceivedDate`, `batchExpiryDate`
- `qtyConsumed`, `overrideComment`, `performedBy` (from `outward_inventory.createdBy`)
- `syncedAt`

### Backend Key Files

| File | Role |
|---|---|
| `model/GlobalFifoReport.java` | Master schema entity |
| `repository/GlobalFifoReportRepository.java` | JPA repo — upsert helper, delete by unique key, `findLastSyncTimeByTenantSchema` |
| `Filters/GlobalFifoReportSpecification.java` | Filter by date range, tenantSchema, productName, warehouseName, contractorName, performedBy |
| `service/FifoReportSyncService.java` | Per-tenant incremental sync: reads consumptions from tenant, fetches outward + products, upserts into master |
| `service/FifoReportSyncOrchestrator.java` | Loops all tenants, `AtomicBoolean` guard, `JobExecutionLog` (JOB_NAME = `FIFO_REPORT_SYNC`) |
| `scheduled/FifoReportSyncJob.java` | `@Scheduled(cron = "0 0 * * * *")` — every hour |
| `service/FifoReportService.java` | Paginated filtered list + Excel export (19 columns) |
| `controller/FifoReportController.java` | POST `/fifo-report/sync`, `/fifo-report/list`, `/fifo-report/export/excel` — `/list` and `/export/excel` annotated `@UseDefaultTenant` (master schema query) |

### Modified Backend Files

| File | Change |
|---|---|
| `repository/OutwardBatchConsumptionRepository.java` | Added `findAllOverrides()` and `findOverridesModifiedAfter(Date since)` |
| `repository/OutwardInventoryRepo.java` | Added `findByOutwardidIn(List<Long> ids)` |

### API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/inventory/fifo-report/sync` | Trigger manual sync |
| `POST` | `/api/inventory/fifo-report/list` | Paginated filtered list |
| `POST` | `/api/inventory/fifo-report/export/excel` | Export to Excel |

---

### Frontend Key Files

| File | Role |
|---|---|
| `Modules/Reports/FifoReport/index.js` | Main list page — Sync Now, Export Excel, Filter, Table, pagination |
| `Modules/Reports/FifoReport/filter.js` | Filter: date range, project, product, warehouse, contractor, performedBy |
| `Modules/Reports/FifoReport/table.js` | Table — custom cell rendering for date, overrideComment, batchExpiryDate |

### Modified Frontend Files

| File | Change |
|---|---|
| `Modules/index.js` | Added `FifoReport` export |
| `Modules/Home/index.js` | Added route `/fifoReport → FifoReport` |
| `endpoints.js` | Added `fifoReportList`, `fifoReportExport`, `fifoReportSync`, `appRoutes.fifoReport` |
| `Shared/SideMenu/index.js` | Added "Reports → FIFO Override Report" (admin only); added `/fifoReport` to `isProjectSelectionPage` |

### Table Columns (UI)

Date, Project, Outward ID, Product, Unit, Warehouse, Structure, Final Location, Contractor, Lot #, Brand, Recv. Date, Expiry Date, Qty, Override Reason, Performed By

---

## Local Database Access

- **Host:** 127.0.0.1
- **User:** root
- **Password:** REDACTED (reset June 2026 via skip-grant-tables)
- **Connect:** `/opt/homebrew/opt/mysql@8.0/bin/mysql --user=root --password=REDACTED --host=127.0.0.1`
- MySQL runs via LaunchAgent: `~/Library/LaunchAgents/homebrew.mxcl.mysql@8.0.plist`
- Stop: `launchctl unload ~/Library/LaunchAgents/homebrew.mxcl.mysql@8.0.plist`
- Start: `launchctl load ~/Library/LaunchAgents/homebrew.mxcl.mysql@8.0.plist`

---

# Reports Module — Completed Work (Session 4)

## Low Stock Report

Global cross-tenant report showing products currently below reorder level.

### Architecture

- **Sync strategy:** Every 30 min (`LowStockSyncJob`, cron `0 0/30 * * * *`)
- **Master table:** `global_low_stock_report` — stores `tenantSchema`, `productId`, `productName`, `productCode`, `category`, `warehouseName`, `currentStock`, `reorderLevel`, `deficit`, `lowStockSince`
- **Sync logic:** `LowStockSyncService` reads `StockSummary` (master schema table with `tenantSchema` column), filters `reorderLevel > 0 AND totalQtyInHand < reorderLevel`, aggregates per product across warehouses, upserts preserving `lowStockSince`
- **Orchestrator:** `LowStockOrchestrator` — loops `schemaConfig.getNonMasterSchemaList()`, same AtomicBoolean guard pattern

### Key Backend Files

| File | Role |
|---|---|
| `model/GlobalLowStockReport.java` | Master schema entity |
| `repository/GlobalLowStockReportRepository.java` | JPA repo — tile count queries (HQL; use `COUNT(r)` not positional `ORDER BY 2`) |
| `service/LowStockSyncService.java` | Per-tenant sync |
| `service/LowStockOrchestrator.java` | Multi-tenant orchestration |
| `service/LowStockReportService.java` | Paginated list, tile counts, Excel export |
| `scheduled/LowStockSyncJob.java` | Every 30 min |
| `controller/LowStockController.java` | `/list`, `/tiles`, `/dropdowns`, `/sync`, `/export/excel` — all `@UseDefaultTenant` except `/sync` |

### Tile Windows

5 time windows: new today (1d), last 3d, last 7d, last 30d, total. Each tile shows count + per-project breakdown on hover. Clickable to filter.

### Filter

**Project** and **Category** only. Product name and date range were intentionally removed.

### Frontend Files

| File | Role |
|---|---|
| `Modules/Reports/LowStock/index.js` | Main page — tiles, sync, export, filter |
| `Modules/Reports/LowStock/filter.js` | Project + Category dropdowns only |
| `Modules/Reports/LowStock/table.js` | Sortable table, color-coded deficit column |

### Route

`/lowStockReport` — in `isProjectSelectionPage` list (global, no project context required)

---

## PO vs Inward Reconciliation Report

Global report comparing ordered vs received qty per PO line. **Live query — no sync table needed** (PO data in master schema).

### Architecture

- **Live native SQL** via `EntityManager` — `purchase_order` + `purchase_order_line` + `indent_inventory_entries` + `indent_inventory` (all in master schema)
- **Pagination:** manual `COUNT(*) FROM (subquery)` + `LIMIT/OFFSET`
- **Project identification:** `COALESCE(ii.tenant, po.project_name, 'Unknown')` — indent's tenant is the authoritative project code
- **HAVING clause:** `reconciliationStatus` filter uses `HAVING (CASE WHEN ...) = :status` after `GROUP BY`
- **`WhereClause` inner class** has separate `where` (before GROUP BY) and `having` (after GROUP BY) fields
- **Card-based UX:** rows grouped by PO, each PO = expandable card with header + product lines grid

### Key Backend Files

| File | Role |
|---|---|
| `data/PoInwardReconciliationRow.java` | DTO — `@JsonFormat(dd-MM-yyyy)` on `poDate` |
| `service/PoInwardReconciliationService.java` | Native SQL, pagination, stats, export |
| `controller/PoInwardReconciliationController.java` | All `@UseDefaultTenant`; default sort `poDate DESC` |

### Key SQL Constants

```java
BASE_FROM = " FROM purchase_order po JOIN purchase_order_line pol ... LEFT JOIN Firm f ... LEFT JOIN indent_inventory_entries iie ... LEFT JOIN indent_inventory ii ..."
GROUP_BY  = " GROUP BY po.purchase_order_id, pol.product_id, COALESCE(ii.tenant, po.project_name, 'Unknown')"
RECON_STATUS_CASE = " CASE WHEN COALESCE(SUM(iie.quantity_received), 0) <= 0 THEN 'NOT_STARTED' WHEN ... >= MAX(pol.quantity) THEN 'COMPLETE' ELSE 'PARTIAL' END"
```

### Frontend Files

| File | Role |
|---|---|
| `Modules/Reports/PoReconciliation/index.js` | Main page — 4 stat tiles, Export, Filter, Cards |
| `Modules/Reports/PoReconciliation/filter.js` | Project, PO Status, Product Name, Date Range |
| `Modules/Reports/PoReconciliation/cards.js` | Card view — one card per PO, product lines in grid |
| `Modules/Reports/PoReconciliation/table.js` | Old flat table (kept, no longer used in index.js) |

### Route

`/poReconReport` — in `isProjectSelectionPage`

---

## Indent Fulfillment Report

Global report showing indent line items with requested/received/pending quantities. **Live query — no sync** (indent data in master schema). **Card-based UX with 3-stage pipeline view.**

### Architecture

- Same native SQL + EntityManager pattern as PO Recon
- Default sort: `ORDER BY ii.indent_date DESC, ii.indent_id ASC, p.product_name ASC` (critical for card grouping — rows for the same indent must be contiguous)
- `indentStatus` filter supports **multiple values** via IN clause (for group tile clicks that cover multiple statuses)
- `WhereClause` has `sql` (WHERE additions) + `statusSql` / `statusParams` (for the simpler stats query that doesn't need product joins)

### SQL Join

```sql
FROM indent_inventory ii
JOIN indent_inventory_entries iie ON iie.indent_id = ii.indent_id AND iie.is_deleted = 0
JOIN product p ON p.productId = iie.productId AND p.is_deleted = 0
LEFT JOIN purchase_order po ON po.purchase_order_id = iie.purchaseOrderId AND po.is_deleted = 0
LEFT JOIN purchase_order_line pol ON pol.po_id = iie.purchaseOrderId AND pol.product_id = iie.productId AND pol.is_deleted = 0
WHERE ii.is_deleted = 0
```

The PO joins add `poQty` (from `pol.quantity`) and `poStatus` (from `po.status`) to each line — enables end-to-end lifecycle tracking.

### IndentFulfillmentRow fields

`indentId`, `project`, `indentDate`, `indentStatus`, `productName`, `productCode`, `unit`, `requestedQty`, `poQty` (nullable), `receivedQty`, `pendingQty`, `percentFulfilled`, `poNumber`, `poStatus`, `lineItemStatus`, `needByDate`

### Frontend Files

| File | Role |
|---|---|
| `Modules/Reports/IndentFulfillment/index.js` | Main page — 5 status group tiles, Export, Filter, Cards |
| `Modules/Reports/IndentFulfillment/filter.js` | Project, Product Name, Indent Status chips, Line Status chips, Date Range |
| `Modules/Reports/IndentFulfillment/cards.js` | Card view — one card per indent, 3-stage pipeline per line |
| `Modules/Reports/IndentFulfillment/table.js` | Old flat table (kept, no longer used) |

### Status Groups (tile filter)

| Group key | Statuses included |
|---|---|
| PENDING | NEW, APPROVED |
| IN_PROGRESS | PO CREATED, PO PARTIAL, INWARD PARTIAL |
| COMPLETED | CLOSED, PO COMPLETED |
| CANCELLED | CANCELLED, REJECTED |

Multiple statuses sent as `attrValue: ["NEW", "APPROVED"]` array. Backend uses `IN (:s0, :s1)` clause.

### Card UX

- **Header:** Indent ID, status badge, project chip, date, overall progress bar (received/requested), status summary pill
- **Status summary pill:** see "IndentFulfillment Cards — Status Summary Pill Logic" section in Session 5 Learnings for full truth table. `PO COMPLETED` ≠ received — shows "📦 Fully PO'd" not "✓ Fully Received".
- **Per-line:** product name, unit, PO info chip (PO# + status), line status badge
- **3-stage pipeline bar:** `[Requested qty] → [PO'd qty] → [Received qty]` — visual end-to-end lifecycle at a glance. PO Qty is null/blank if no PO linked yet.

### Route

`/indentFulfillmentReport` — in `isProjectSelectionPage`

---

# Reports Module — Session 5 Learnings

## Shared Frontend Infrastructure (`SC UI/src/Modules/Reports/`)

### `projectColors.js`

Shared utility imported by ALL report card/table files.

```js
getProjectColor(projectCode)  // returns { bg, color, border } — deterministic hash, same code = same color
```

Color hash uses raw `tenantCode` (schema code) — NOT display name. This ensures color stability even if display name changes.

### Project Display Name Resolution (Tenant Map)

All report index pages now fetch `GET /user/allowedtenants` → build `tenantMap = { tenantCode → tenantName }`.

Pattern used in all 5 report index files:
```js
// In state:
tenantOptions: []

// In fetchTenants() (already existed in FifoReport, StockAging, LowStock):
const tenantOptions = res.data
  .filter(t => t.inventory === true)
  .map(t => ({ name: t.tenantName || t.name || '', id: t.tenantCode }))
  .filter(t => t.name && t.id);
this.setState({ tenantOptions });

// Passed to child:
tenantMap={Object.fromEntries(this.state.tenantOptions.map(t => [t.id, t.name]))}
```

Cards/tables use: `(tenantMap && tenantMap[code]) || code` — graceful fallback to raw code if map missing.

**PoReconciliation and IndentFulfillment** did NOT originally fetch tenants (they fetched project codes from their own backend endpoints). `fetchTenants()` was added to both in this session.

### Filter Dropdowns — Exact Match vs LIKE

All report filters use **exact match** (not LIKE) for autocomplete-sourced fields:
- `SpecificationsBuilder.whereDirectFieldEquals()` — use for product name, category, warehouse, contractor, performedBy
- `SpecificationsBuilder.whereDirectFieldContains()` — only for free-text search fields

Applies to: `GlobalStockAgingSpecification`, `GlobalFifoReportSpecification`, `GlobalLowStockSpecification`

### Filter Multi-Select Pattern (Native SQL reports)

For PoRecon and IndentFulfillment, multi-value filters build IN clauses dynamically:
```java
List<String> values = f.getAttrValue().stream()...collect(toList());
if (values.size() == 1) {
    where.append(" AND field = :p0"); params.put("p0", values.get(0));
} else {
    // build IN (:p0, :p1, ...)
}
```
Frontend sends `attrValue: ["A", "B"]` array. Backend always handles both single and multi-value.

### Default Filter Badge Pattern

PoReconciliation and IndentFulfillment load with a pre-populated default filter (active/pending records only).

State:
```js
usingDefaultFilter: true  // set false when user opens filter dialog or removes badge
```

Dismissible badge in UI — clicking × clears the relevant filterData key and sets `usingDefaultFilter = false`.

---

## Low Stock Report — Architecture Notes

### Reorder Level Source (Critical)

`LowStockSyncService` reads reorder level from **`ProductTenantConfig`** (tenant schema) directly — NOT from `StockSummary.reorderLevel`. This was a two-step dependency bug that was fixed.

Steps in sync:
1. Load all `StockSummary` rows for tenant
2. Load `ProductTenantConfig` overrides for those products (from tenant schema)
3. Effective reorder = `tenantOverride ?? stockSummary.reorderLevel`
4. Filter: `totalQty < effectiveReorder`
5. Load fresh product metadata from `ProductRepo`
6. Upsert with correct reorder level

### Tile Date Window Fix (Calendar Day Alignment)

`LowStockReportService.dateMinusDays(days)` returns **start of calendar day**, not rolling 24-hour window.

- `days=1` → today midnight 00:00:00 → "New Today" shows only records from today
- `days=3` → 2 days ago midnight → covers 3 full calendar days

```java
cal.add(Calendar.DAY_OF_YEAR, (int) -(days - 1));
cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
```

Same logic in frontend `handleTileClick`:
```js
if (tile.days === 1) { start.setHours(0, 0, 0, 0); }          // today midnight
else { start.setDate(start.getDate() - (tile.days - 1)); start.setHours(0,0,0,0); }
```

**Bug to avoid:** `start.setDate(today - tile.days)` gives rolling 24h window. 9th June 22:30 record shows on 10th June morning when "New Today" tile clicked. Fixed by using calendar-day start.

---

## FIFO Report — Sync Step 6 (Metadata Refresh)

Incremental sync only touches records where `lastModifiedDate > lastSyncTime`. Product renames/unit changes would leave stale names.

`FifoReportSyncService` Step 6 — after incremental sync, refresh product metadata for ALL synced products of that tenant:
```java
List<Long> allSyncedProductIds = globalFifoReportRepository.findDistinctProductIdsByTenantSchema(tenantSchema);
// update productName + measurementUnit for each
globalFifoReportRepository.updateProductMetadata(tenantSchema, pid, name, unit);
```

Same pattern should be applied if any other synced report has product metadata (name/unit) that can change.

---

## IndentFulfillment Cards — Status Summary Pill Logic

The header pill on each indent card reflects the **actual fulfillment state**, not just the `indentStatus` field. These are independent.

| Pill | Condition |
|---|---|
| ✕ Cancelled / ✕ Rejected | `indentStatus` is CANCELLED or REJECTED |
| ⊘ Short Closed | `indentStatus` is SHORT CLOSED |
| ✓ Fully Received | `indentStatus === 'CLOSED'` OR `receivedQty >= requestedQty > 0` |
| 📦 Fully PO'd | `indentStatus === 'PO COMPLETED'` OR `poQty >= requestedQty` — but received < requested |
| ◑ Partially Received | some received, not all, no full PO coverage |
| 🔄 PO in Progress | has `pendingQty` in PO but 0 received |
| ○ Not Started | no PO, no receipt |

**Critical invariant:** `PO COMPLETED` ≠ goods received. A PO COMPLETED indent with 0 inward should show "📦 Fully PO'd", NOT "✓ Fully received". Previous bug: `isFullyReceived` included `indentStatus === 'PO COMPLETED'` which was wrong.

`isFullyReceived` must require actual `receivedQty >= requestedQty > 0` OR `CLOSED` status.

`isFullyPOd` is a separate state — compute `totalPOd = sum(line.poQty)`, check `totalPOd >= totalAllRequested`.

---

## Tile Count Queries — Native SQL Pitfall

When writing `@Query` annotations with native SQL (`nativeQuery = true`) in Spring JPA:
- Column names must match Java field names (camelCase) as Hibernate stores them
- NOT snake_case DB column names
- Example: use `outwardId` not `outward_id` in HQL/JPQL
- Use `COUNT(r)` not positional `ORDER BY 2` in HQL aggregate queries

---

## Product Name / Unit Sync — Pattern

When product metadata (name, unit, category) can change in the tenant schema, any master-schema sync table needs a metadata refresh step. Pattern:

1. After the incremental upsert, query `DISTINCT productId` from master table for that tenant
2. Fetch fresh `Product` entities from tenant schema
3. Bulk `UPDATE` master table setting `productName`, `unit` by `(tenantSchema, productId)`

Applies to: `FifoReportSyncService` (done), `LowStockSyncService` (done via fresh ProductRepo load in sync loop).

---

## Contacts Table — Supplier and Contractor Pattern

**Critical for native SQL queries:** There is NO `supplier` table in any schema. Both suppliers and contractors are stored in the `contacts` table, distinguished by `contacttype`.

| Entity class | Table | PK | Filter |
|---|---|---|---|
| `Supplier` | `contacts` | `contactId` | `contacttype = 'supplier'` |
| (Contractor) | `contacts` | `contactId` | `contacttype = 'contractor'` (or similar) |

`PurchaseOrder` has:
- `supplier_id` → FK to `contacts.contactId` (supplier)
- `firm_id` → FK to `Firm.id` (billing firm)

**In native SQL, never join `supplier` table.** Use:
```sql
LEFT JOIN contacts s ON s.contactId = po.supplier_id AND s.contacttype = 'supplier' AND s.is_deleted = 0
```

**Column name pitfall:** DB columns in masterschema use camelCase (Hibernate convention), not snake_case:
- Product unit: `measurementUnit` (not `measurement_unit`)
- Category PK: `categoryId` (not `id` or `category_id`)
- Product FK to category: `categoryId` (not `category_id`)
- Supplier/contact PK: `contactId` (not `id`)

Always verify column names against `SHOW COLUMNS FROM <table>` before writing native SQL targeting masterschema.

**Mixed convention in `product` table (tenant schema):**
- `product_code` — snake_case (NOT `productCode`)
- `product_name` — snake_case (NOT `productName`)
- `measurementUnit` — camelCase
- `categoryId` — camelCase (FK to category)
- `productId` — camelCase (PK)

Same mixed convention applies to tenant schemas. Always check before writing native SQL.

---

## Native SQL — Table Name Case Rules (Linux/QA Safety)

**Critical:** Local Mac MySQL is case-insensitive (`lower_case_table_names=2`). QA/prod runs Linux MySQL with `lower_case_table_names=0` (case-sensitive). A query that works locally can fail on QA purely due to table name casing.

### Canonical table name casing (match exactly in all native SQL)

| Table | Correct case | Wrong |
|---|---|---|
| `Product` | `Product` | ~~`product`~~ |
| `Category` | `Category` | ~~`category`~~ |
| `Firm` | `Firm` | ~~`firm`~~ |
| `BOQUpload` | `BOQUpload` | ~~`boqupload`~~ |
| `Usage_Location` | `Usage_Location` | ~~`usage_location`~~ |
| `contacts` | `contacts` | ~~`Contacts`~~ |
| `purchase_order` | `purchase_order` | ~~`Purchase_Order`~~ |
| `purchase_order_line` | `purchase_order_line` | — |
| `indent_inventory` | `indent_inventory` | — |
| `indent_inventory_entries` | `indent_inventory_entries` | — |
| `inward_inventory` | `inward_inventory` | — |
| `outward_inventory` | `outward_inventory` | — |
| `inventory_batch` | `inventory_batch` | — |
| `stock_summary` | `stock_summary` | — |

**Rule:** PascalCase/camelCase table names (Hibernate-created: `Product`, `Category`, `Firm`, `BOQUpload`) must use exact class-name casing. Snake_case tables (manually created) are all lowercase.

**This rule applies to fully-qualified references too:** `masterschema.Product` NOT `masterschema.product`.

When writing any new native SQL (`createNativeQuery` or `@Query(nativeQuery=true)`), verify the table name against this list before committing. If adding a new table, confirm its actual casing by running `SHOW TABLES` against the schema.

---

# BOQ Feature — Completed Work (Session 6)

## What Was Built

BOQ (Bill of Quantities) feature for tracking planned vs actual material usage per project.

---

## Architecture

### Key Entities

- `BOQUpload` — tenant schema table. Fields: `productId`, `quantity`, `wastagePercent`, `is_deleted`
- BOQ data lives in **project (tenant) schema** (unlike Indents which are in master schema)

### BOQ Summary (per-product, on Indent form)

**Endpoint:** `GET /api/inventory/boqupload/boq-summary?productId=X`

Returns `ProductBOQSummaryDto`:
- `hasBOQ` — false if no BOQ rows for product
- `totalPlanned` — SUM(quantity * (1 + wastagePercent/100))
- `totalIndented` — SUM of valid indent quantities (master schema)
- `remaining` — totalPlanned - totalIndented

"Valid" indents exclude status: `CANCELLED`, `REJECTED`, `SHORT CLOSED`, `SHORT_CLOSED`

**Performance:** Uses 3 targeted EntityManager native queries (COUNT, SUM BOQ, SUM indent). Sub-500ms. Avoids `getCachedBOQStatusRows()` entirely.

### BOQ vs Indent Summary Report

**Endpoint:** `GET /api/inventory/boqupload/boq-vs-planned`

Returns `List<BOQIndentSummaryItem>` — one row per product with BOQ or indent activity.

Fields: `productId`, `productName`, `categoryName`, `productCode`, `unit`, `totalPlanned`, `totalIndented`, `remaining`

Sorted: category → productName.

**Performance:** 2 EntityManager GROUP BY queries (no correlated subquery). Sub-1s.

---

## Critical: TenantNameInterceptor URL Exclusion Patterns

`TenantNameInterceptor` has excluded URL patterns. Any endpoint URL matching these patterns will have its tenant set to **masterschema** (default) instead of reading the `tenant-id` header. This silently causes BOQ/stock data queries to hit wrong schema.

**Known excluded patterns include:**
- `.*/product.*` — matches any URL containing "product"
- `.*/indent.*` — matches any URL containing "indent"

**Do NOT name BOQ endpoints with these strings.** Use:
- ✅ `/boq-summary` (not `/product-boq-summary`)
- ✅ `/boq-vs-planned` (not `/boq-indent-summary`)

If a new endpoint mysteriously returns empty/wrong data despite DB having correct data, check `TenantNameInterceptor` excluded pattern list first.

---

## Cross-Schema Query Pattern (BOQService)

`BOQService` is `@Transactional` at class level (javax). This pins connection to tenant schema.

To query BOTH tenant schema (for BOQ) AND master schema (for indents) in one method:
- Annotate the method with `@Transactional(TxType.NOT_SUPPORTED)` — suspends class-level transaction
- Use `EntityManager.createNativeQuery()` for both
- For master schema tables: use fully-qualified names (`masterschema.indent_inventory_entries`)
- For tenant schema tables: use unqualified names (routing handles it via ThreadLocal)

```java
@Transactional(Transactional.TxType.NOT_SUPPORTED)
public ProductBOQSummaryDto getProductBOQSummary(Long productId) {
    String master = schemaConfig.getMasterSchema();
    String tenantCode = ThreadLocalStorage.getTenantName();
    // tenant query — unqualified:
    em.createNativeQuery("SELECT ... FROM BOQUpload WHERE ...")...
    // master query — fully qualified:
    em.createNativeQuery("SELECT ... FROM " + master + ".indent_inventory_entries ...")...
}
```

**Self-call warning:** Both `@Cacheable` and `@Transactional` on a method are bypassed when called from within the same bean (Spring AOP proxy not invoked). Always inject self or use direct EntityManager queries instead of calling cached methods internally.

---

## Key Backend Files

| File | Role |
|---|---|
| `controller/BOQController.java` | Endpoints: `/boq-summary`, `/boq-vs-planned`, upload, list |
| `service/BOQService.java` | Core logic: summary, cross-schema queries, upload processing |
| `data/ProductBOQSummaryDto.java` | DTO for per-product BOQ summary |
| `data/BOQIndentSummaryItem.java` | DTO for BOQ vs Indent report row (includes `categoryName`) |
| `repository/BOQUploadRepository.java` | JPA repo for BOQ uploads |

---

## Key Frontend Files

| File | Role |
|---|---|
| `Modules/Reports/BOQIndent/index.js` | BOQ vs Indent Report page — table with category filter |
| `Modules/Indent/add.js` | BOQ remaining info box (renderTextField style, colored border) |
| `Modules/Indent/details.js` | BOQ remaining chip on approval view (status=NEW only) |

### BOQ chip in Indent Details

- Shown only when `canShowApprove || canShowManagerReject` (i.e. approval-relevant views)
- Green chip "BOQ Rem. X unit" or red "⚠ Exceeded by X unit"
- Fetched in parallel for all products via `fetchBOQForItems()`
- State: `boqDataByProduct: { [productId]: boqData }`

---

## Report Page Defaults (Session 6)

All report list pages now open with `pageSize = 100` (overriding `ListCommon` base class default of 12).

Affected files (add `pageSize = 100;` after `url =` line in each class):
- `Reports/FifoReport/index.js`
- `Reports/StockAging/index.js`
- `Reports/LowStock/index.js`
- `GlobalStockReports/DeadStockReport.js`
- `GlobalStockReports/ExpiredStockReport.js`
- `Reports/PoReconciliation/index.js`
- `Reports/IndentFulfillment/index.js`

---

## IndentFulfillment — Category Added (Session 6)

`IndentFulfillmentRow.categoryName` added. SQL in `IndentFulfillmentService.DATA_SELECT` selects `cat.category_name` as r[15]. The `LEFT JOIN category cat` was already in `BASE_FROM`.

Frontend `IndentFulfillment/cards.js` shows `categoryName → productName` above each line item.

---

## BOQ Table Schema (tenant schema)

Table: `BOQUpload`

| Column | Type | Notes |
|---|---|---|
| `id` | bigint PK | |
| `productId` | bigint FK | → `product.productId` |
| `buildingTypeId` | bigint FK | → `usage_location.typeId` (structure type) |
| `usageLocationId` | bigint FK | → `Usage_Location.locationId` (structure / block) |
| `locationId` | bigint FK | → `location.locationId` (final location / flat) |
| `quantity` | double | base planned qty |
| `wastagePercent` | double | effective planned = quantity * (1 + wastagePercent/100) |
| `sno` | int | row order |
| `is_deleted` | bit | soft delete |

### Dimension Tables (tenant schema)

| Table | PK | Key column | Notes |
|---|---|---|---|
| `usage_location` | `locationId` | `location_name`, `typeId` | Structure (block/tower). `typeId` → building_type |
| `building_type` | `typeId` | `building_type` | Structure type (e.g. Tower A, Villa) |
| `usage_area` | `usageAreaId` | `usagearea_name` | Final location (flat/unit) |
| `location` | `locationId` | `location_name`, `typeId` | Also links to building_type via `typeId` |

### Outward → Location Hierarchy

`outward_inventory` has:
- `locationId` → `location.locationId` (structure/block level)
- `usageAreaId` → `usage_area.usageAreaId` (final location/flat level)
- No direct `buildingTypeId` — get building type via: `location.typeId → building_type.typeId`

### BOQ → Outward Join Path

BOQ and outward share `locationId` (both refer to the same `location` table).
BOQ `usageLocationId` is NOT the same FK as outward `locationId` — verify before joining.
Check actual data to confirm join key before writing the unified report SQL.

### Indent — No Location Data

`indent_inventory_entries` has no `locationId`, `buildingTypeId`, or `usageAreaId`.
Indent qty only rolls up at **product level** — cannot drill down by structure/location for indents.

---

## Unified BOQ Report — Planned Feature (Next Session)

**Goal:** Single page showing BOQ Planned vs Indented vs Outward with drill-down.

### Hierarchy
```
Product level       → BOQ Planned | Total Indented | Total Outward
  └─ Building Type  → BOQ Planned | Outward  (no indent at this level)
       └─ Location (structure) → BOQ Planned | Outward
            └─ Usage Area (final location) → BOQ Planned | Outward
```

### Filters needed
- Category, Product
- Gap filters: BOQ exists but no indent, BOQ exists but no outward, indent exists but no BOQ

### Backend approach
- New endpoint in `BOQController`
- `@Transactional(TxType.NOT_SUPPORTED)` + `EntityManager` native queries (same pattern as `getBOQIndentSummary`)
- Two queries: (1) BOQ GROUP BY product+buildingType+location+usageLocation, (2) Outward GROUP BY product+location+usageArea
- Indent totals at product level only (existing `getBOQIndentSummary` logic reused)
- Join outward → building type via `location.typeId → building_type.typeId`

### Frontend approach
- New page `Modules/Reports/BOQDashboard/index.js`
- Accordion UX: top row per product, expand to see building type rows, expand further to location rows
- New route `/boqDashboard`, add to `isProjectSelectionPage`, add to sidebar under Reports

### Pre-work for next session
- Verify `BOQUpload.usageLocationId` vs outward `locationId` join compatibility with actual data
- Run: `SELECT bu.usageLocationId, oi.locationId FROM BOQUpload bu JOIN outward_inventory oi ON bu.productId = oi.productId LIMIT 5` to confirm key overlap

---

# BOQ End-to-End Tracker — Planned Feature (Session 7)

## Goal

Single unified page: BOQ Planned vs Indent vs Outward, drillable from summary → structure type → structure → final location.

## Entity Relationships (confirmed from code)

### BOQUpload fields (tenant schema, table: `BOQUpload`)
| Field | FK | Entity class | Table |
|---|---|---|---|
| `buildingTypeId` | → `BuildingType.id` | `BuildingType` | `building_type` |
| `usageLocationId` | → `UsageLocation.id` | `UsageLocation` | `Usage_Location` |
| `locationId` | → `UsageArea.id` | `UsageArea` | `usage_area` |
| `productId` | → `Product.productId` | `Product` | `product` |
| `quantity` | — | — | — |
| `wastagePercent` | — | — | — |

Planned qty = `quantity * (1 + wastagePercent/100)`

### OutwardInventory fields (tenant schema, table: `outward_inventory`)
| Field | FK | Meaning |
|---|---|---|
| `locationId` | → `UsageLocation.id` | Structure (matches BOQ `usageLocationId`) |
| `usageAreaId` | → `UsageArea.id` | Final location (matches BOQ `locationId`) |

### UsageLocation (table: `Usage_Location`)
- `location_name` — structure name
- `typeId` → `BuildingType.id` — structure type FK

### UsageArea (table: `usage_area`)
- `usagearea_name` — final location name
- No direct FK to BuildingType (path is via UsageLocation)

### Join path: Outward → BuildingType
```
outward_inventory.locationId → Usage_Location.id → Usage_Location.typeId → building_type.id
```

### Hierarchy
```
BuildingType (structure type)
  └─ UsageLocation (structure) — linked via typeId
       └─ UsageArea (final location) — linked via outward
```

## Drilldown Design

| Level | Columns | Data source |
|---|---|---|
| Product | BOQ Planned, Indented, Outward | BOQ (tenant), indent (master schema), outward (tenant) |
| BuildingType | BOQ Planned, Outward | BOQ + outward (indent has no location) |
| UsageLocation | BOQ Planned, Outward | BOQ + outward |
| UsageArea | BOQ Planned, Outward | BOQ + outward |

Indent only aggregates at product level — no structure breakdown.

## Filter Requirements
- Category
- Product name
- BOQ exists but no indent
- BOQ exists but no outward
- Indent exists but no BOQ
- No BOQ, no indent, no outward (activity with no plan)

## Backend Strategy
- New endpoint: `GET /api/inventory/boqupload/boq-tracker`
- Returns per-product summary with nested structure breakdown
- `@Transactional(TxType.NOT_SUPPORTED)` to cross tenant+master schemas (same as `getProductBOQSummary`)
- BOQ + outward: JOIN on `productId` + `usageLocationId = outward.locationId`
- Indent: cross-schema query to `masterschema.indent_inventory_entries` (same as existing BOQ summary)
- Gap filters implemented as HAVING or post-filter in Java

## Frontend Strategy
- New page: `Modules/Reports/BOQTracker/index.js`
- Accordion UX: top-level product rows, expand → structure type rows, expand → structure/location rows
- Route: `/boqTracker` — add to `isProjectSelectionPage`
- Sidebar: under Reports

---

## PoReconciliation — BOQ Planned Removed (Session 6)

`enrichWithBOQ()` removed from `PoInwardReconciliationService`. It was calling `getCachedBOQStatusRows()` on every page load causing 5+ second latency. BOQ Planned column removed from:
- `PoInwardReconciliationService.exportExcel()` — 13 columns now (was 14)
- `Reports/PoReconciliation/cards.js` — grid changed from 8 to 7 columns

---

# Quote Comparison Feature — Completed Work (Session 7)

## What It Does

Lets a buyer collect multiple vendor quotes for a set of demand lines (sourced from indents), compare them side by side, award (finalize) each line to a winner, and turn the award into a Purchase Order — with the indent itself reflecting that an RFQ is in flight. Master-schema entity (`@UseDefaultTenant` on `QuoteComparisonService`), same as Indents/POs.

## Entities

| Entity | Table | Notes |
|---|---|---|
| `QuoteComparison` | `quote_comparison` | Header. `qcId` PK (custom ID gen, e.g. `QC-1`). `status` enum — see below. |
| `QuoteComparisonLine` | `quote_comparison_line` | One per demand line. `indentId`/`indentLineId` link back to the source indent line (`lineItemCode`). `lineStatus`: `OPEN` → `FINALIZED` → `PO_LINKED`. |
| `ComparisonCriteria` | `comparison_criteria` | Custom comparison columns. `criteriaScope`: `LINE` (varies per product) or `HEADER` (one value per vendor for the whole quote). |
| `SupplierQuote` | `supplier_quote` | One per vendor *round*. `revisionLabel` (R-0, R-1...) lets the same vendor be quoted multiple times — each round is a separate row, auto-numbered if left blank (`nextRevisionLabel`). |
| `SupplierQuoteLine` | `supplier_quote_line` | Per-line vendor response: rate, qty, discount, GST, freight, computed `landedCost`. |
| `SupplierQuoteCriteriaValue` | `supplier_quote_criteria_value` | Either `supplierQuoteLine` (LINE scope) or `supplierQuote` (HEADER scope) is set, never both. |
| `QuoteToPoRef` | — | Audit trail: which PO/PO-line a finalized quote line was turned into. |

## Status State Machine (`QuoteComparisonService.recalcStatus`, re-run after every quote/finalize/link/delete)

| Status | Set when |
|---|---|
| `DRAFT` | No `SupplierQuote` exists yet |
| `OPEN` | At least one quote exists, every line still `OPEN` |
| `PARTIALLY_FINALIZED` | Mix of open and decided lines |
| `FINALIZED` | Zero `OPEN` lines, **none** `PO_LINKED` |
| `PARTIALLY_ORDERED` | Zero `OPEN` lines, **some** (not all) `PO_LINKED` |
| `PO_COMPLETED` | **All** lines `PO_LINKED` |
| `CLOSED` | Manual — `recalcStatus` exits early (sticky), reversible via `reopen()` |
| `CANCELLED` | Manual — sticky, permanent (blocked if any line already `PO_LINKED`) |

`reopen()` only works from `CLOSED` (not `CANCELLED`): it flips status to `OPEN` then calls `recalcStatus` to let it recompute the *real* status from the lines' actual state, rather than guessing.

## Indent Linkage — "Quote Requested" Marker

`IndentInventoryList.quoteRequestedQcId` (column `quote_requested_qc_id`) — **independent of `lineItemStatus`**, which is owned by the PO/inward pipeline and gets silently recomputed on every inward sync (`IndentInventoryAsyncUpdater.recalculateIndentLine`). Overloading that field would have gotten wiped; this is a separate, additive column instead.

- **Set**: `QuoteComparisonService.create()` → `markIndentLinesQuoteRequested()`, looked up by `indentLineId` (= indent's `lineItemCode`), best-effort (logs+continues on failure, never blocks QC creation).
- **Cleared** (all best-effort, all "only if the marker still points at *this* QC" via `clearIndentLineMarker(indentLineCode, qcId)`, so a newer QC that re-grabbed the line isn't clobbered):
  - `cancel()` — whole comparison withdrawn → `clearIndentLinesQuoteRequested()` clears all lines.
  - `close()` — whole comparison decided → clears all lines. **(Reversed the earlier "close doesn't clear" decision — a closed RFQ is no longer awaiting a quote. Session 8.)**
  - `linkToPo()` — that ONE demand line is now ordered → `clearIndentLineMarker(...)` clears just that line (others may still be OPEN). Since PO save loops each `_linked` line, a fully-ordered QC ends up with every marker cleared as it reaches `PO_COMPLETED`.
- **Re-set on `reopen()`**: `remarkIndentLinesIfFree()` re-marks a reopened (CLOSED→active) comparison's lines, but ONLY where the indent line's marker is currently null (don't clobber a newer QC) and NOT for `PO_LINKED` lines (already ordered, stay cleared).
- **The two-active guard treats terminal statuses as non-blocking**: `validateIndentLinesNotAlreadyQuoted()` and `isTerminalStatus()` exempt `CANCELLED`, `CLOSED`, and `PO_COMPLETED` — belt-and-suspenders so that even if a best-effort marker-clear silently failed, a terminal QC can never permanently lock its indent line out of a new comparison.
- **`deleteSupplierQuote()` has the same orphan guard as `updateSupplierQuote()`**: blocks removing a quote that is the winning reference for any FINALIZED/PO_LINKED demand line (would dangle `finalizedSupplierQuoteLineId` at a soft-deleted line) — user must Reopen those lines first.
- **Propagated on indent split**: `IndentInventoryService.splitLineItem()` copies the marker to both split children (it copies `lineItemStatus` too — same pattern). Known residual: if a quoted line is later split *and then* its QC is cancelled, the cancel's lookup-by-original-`lineItemCode` won't find the (now differently-coded) split children, so their markers won't auto-clear — narrow compound edge case, not closed.
- **One indent line can only be in ONE active (non-cancelled) comparison at a time** — enforced two ways:
  1. **Proactive**: `Step1SelectIndents` (shared by both PO and Quote Comparison creation wizards) disables/grays already-quoted rows with a tooltip, but **only when `disableAlreadyQuoted` prop is passed** (only from `QuoteComparison/create.js` — the PO wizard is unaffected, since being quoted never blocks creating a PO directly from an indent).
  2. **Reactive safety net**: `QuoteComparisonService.create()` → `validateIndentLinesNotAlreadyQuoted()` rejects server-side regardless of UI state (handles races / direct API calls).
- **Indent list filter**: "Quote Requested" Yes/No dropdown (`Indent/filter.js`) → `hasQuoteRequested` attrName → `IndentInventorySpecification.lineItemHasQuoteRequested()` (EXISTS/NOT EXISTS subquery on `IndentInventoryList`). "No" means *zero* lines quoted, not "at least one un-quoted line".
- **Indent list/details badges**: aggregate "Quote Requested" / "Partial Quote Requested" badge (`Indent/table.js` status cell, stacked vertically under the main status to avoid overflowing into the next column — it did the first time, fixed) computed client-side from how many *active* (non-cancelled) line items carry the flag; per-line "Quote Req." chip in both the list's expansion row and `Indent/details.js`.

## Create-PO Integration (bidirectional)

- **From Quote Comparison**: Overview tab shows a "Ready for Purchase Order" card per *winning supplier-quote* group (one PO = one vendor/revision) — `getFinalizedPoGroups()` groups `FINALIZED` matrix rows by `supplierQuoteId`. "Create PO" button builds a prefill (`SC UI/src/Shared/quoteToPo.js` → `buildQuotePrefill`) and hands off via `sessionStorage` (see gotcha below), landing directly on PO step 2 (Fill Details), pre-filled with product/qty/rate/GST/discount and `_linkedQcLineId`/`_linkedSupplierQuoteLineId`/`_linkedQcId` markers on each item.
- **From Purchase Order**: "Load from Quote" button in `PurchaseOrder/list.js` toolbar opens `PurchaseOrder/add/LoadFromQuoteDialog.js` — two-step picker (comparison → vendor group within it), same `quoteToPo.js` helpers, sets state directly (no sessionStorage needed since it's the same already-mounted page). Only lists comparisons in `FINALIZED`, `PARTIALLY_FINALIZED`, or `PARTIALLY_ORDERED` status (i.e. has at least one finalized-but-unordered line) — **must keep this list in sync whenever a new header status is added**, it was missed for `PARTIALLY_ORDERED` once already.
- **On PO save**: `add.js`'s create-success handler loops `state.items` for the `_linked*` markers and calls `quoteComparisonLinkToPo` for each — which sets the QC line to `PO_LINKED` and triggers `recalcStatus`. Guarded against double-linking (`linkToPo` rejects if the line is already `PO_LINKED`).
- **Editing a finalized SupplierQuote is blocked**: `updateSupplierQuote()` deletes-and-recreates all `SupplierQuoteLine` rows with new IDs on every edit — if any of those lines is the *current* winning reference for a finalized demand line, editing would silently orphan that reference (matrix/Create PO lose track of the award). Blocked outright with a message naming the affected product(s) and pointing at "Reopen" first.
- **Gotcha — sessionStorage, not router state**: the PO module's route wrapper (`Home/index.js`) does `key={new Date()}` on every routed component, which **remounts on every parent re-render**, wiping anything passed via `history.push(url, state)` before it's read. Use `sessionStorage` (set in `details.js` before navigating, read in `PurchaseOrder/index.js` `componentDidMount`, cleared only on the wizard's `back()`) instead of router state for any future cross-route handoff in this app.
- **Gotcha — multi-project indent strings**: a QC's `project` field can be a comma-joined list when its source indents spanned multiple projects. `buildQuotePrefill` only carries it into the PO prefill when it's a single value — otherwise the PO's Project field is left blank rather than silently holding an invalid combined string (caught by testing, was a real bug).

## Comparison Matrix UX (`comparisonMatrix.js`)

- Vendor summary cards (rank, total landed cost, payment terms, lead time, header-criteria chips) above the detail table, sorted cheapest-first.
- **Auto-narrows past 5 vendors**: shows only the 3 cheapest in the detailed table by default, with checkboxes on each card to add/remove vendors — full vendor count always visible via cards even when narrowed. Computed once via `useState(() => ...)` at mount; doesn't auto-recompute if new quotes arrive while the tab stays open (would need a tab switch to re-trigger).
- Sticky left columns use **hardcoded pixel `left` offsets** (`STICKY_WIDTHS` map) — must stay in sync with actual column content widths, or sticky-column text bleeds across boundaries (happened once, fixed by constraining width/overflow on each sticky cell).
- `getFinalizedPoGroups`/`buildPoItemsFromGroup`/`buildQuotePrefill` live in `SC UI/src/Shared/quoteToPo.js`, shared by `QuoteComparison/details.js` and `LoadFromQuoteDialog.js` — change once, both stay in sync.

## RFQ PDF Export

`QuoteComparisonPdfService` (iText, same pattern as `PurchaseOrderPdfService`) — `GET /quote-comparison/{qcId}/rfq-pdf`. Branded with the same logo resource (`sc-login-logo.png`) as the PO PDF. Lists demand lines only (product/qty/unit/spec/need-by) — no rate data exists yet at RFQ stage. No firm/letterhead detail beyond the logo, since an RFQ isn't tied to a specific firm the way a PO is.

## Key Backend Files

| File | Role |
|---|---|
| `service/QuoteComparisonService.java` | Core logic: create/finalize/reopen/close/cancel/linkToPo, status machine, indent-marker sync |
| `service/QuoteComparisonPdfService.java` | RFQ PDF |
| `controller/QuoteComparisonController.java` | All endpoints under `/quote-comparison` |
| `Filters/IndentInventorySpecification.java` | `lineItemHasQuoteRequested()` — new EXISTS subquery |
| `data/ConsolidatedIndentLineDTO.java` | Added `quoteRequestedQcId` — surfaced in the indent-picker response |
| `model/IndentInventoryList.java` | Added `quoteRequestedQcId` column |

## Key Frontend Files

| File | Role |
|---|---|
| `Modules/QuoteComparison/{list,details,create,filter,comparisonMatrix,supplierQuoteForm}.js` | Module pages |
| `Shared/quoteToPo.js` | Shared QC→PO prefill builders |
| `Modules/PurchaseOrder/add/LoadFromQuoteDialog.js` | "Load from Quote" picker |
| `Modules/PurchaseOrder/add/step1SelectIndents.js` | Shared indent picker — `disableAlreadyQuoted` prop (QC creation only) |
| `Modules/Indent/{table,details,filter,list}.js` | Quote-requested badges + filter |

## DB Migrations (no Flyway/Liquibase in this project — `ddl-auto=none`)

All additive, all on `masterschema`. Captured in `sc-inventory-service/quote-comparison-schema-migrations.sql` — **run this by hand against every other environment before deploying**:
- `supplier_quote.revision_label`
- `comparison_criteria.criteria_scope`
- `supplier_quote_criteria_value.supplier_quote_id` (new) + `supplier_quote_line_id` made nullable
- `indent_inventory_entries.quote_requested_qc_id`

## Known Limitations (not fixed, by design or lower priority)

- Split-then-cancel compound edge case on the quote-requested marker (see above).
- `LoadFromQuoteDialog` has no pagination (fetches first 100 quote comparisons).
- No automated test coverage — this feature was built and verified via manual UI testing + direct API calls against local dev DB only.

---

# Pending Deploy Migrations — Standing Convention

No Flyway/Liquibase. `spring.jpa.hibernate.ddl-auto=none` in `application.properties` is **not** the
effective setting at runtime, though — `sc-inventory-service/src/main/java/com/ec/application/multitenant/AutoDDLConfig.java`
builds its own per-tenant routing `DataSource`/`EntityManagerFactory` for every schema in
`schemas.map`, and hardcodes `hibernate.hbm2ddl.auto=update` on each one. That means:

- **Additive changes (new column, new table) are auto-applied on backend startup** — Hibernate's
  `update` mode creates what's missing on every tenant schema (and master) the moment the app boots
  against it. No manual SQL needed for these.
- **Hibernate `update` never drops or renames anything** — dropped columns/tables, renamed
  columns, FK changes, and any data backfill/correction still need to be applied by hand. This is
  what `PendingDeployMigrations.sql` is actually for.

**Single canonical file:** `sc-inventory-service/src/main/resources/SQLs/PendingDeployMigrations.sql`

Rule going forward: any code change that requires a **non-additive** schema change (dropped column,
renamed column, FK change, etc.) or a one-off data correction gets its SQL **appended** to this file
(with a comment explaining what/why and which schema it targets — tenant schema vs `masterschema`),
instead of creating a new one-off `.sql` file. Do not create per-feature migration files anymore.
Purely additive changes (new column/table) don't need an entry — they self-apply on next backend
startup per environment.

User's workflow: after deploying the code, they run everything currently in this file by hand against the target environment(s), then empty the file back out (keeping the header comment) once confirmed applied everywhere. So at any point in time, the file's contents = what is still outstanding / not yet run in prod.

Do NOT delete or stop populating this file just because it looks empty — empty means "nothing pending," not "convention abandoned."

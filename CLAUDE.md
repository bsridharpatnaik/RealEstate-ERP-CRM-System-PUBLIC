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

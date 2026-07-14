# ERP/CRM Project — Claude Code Context

Real Estate ERP/CRM: inventory, indents, purchase orders, inward/outward, stock, service orders,
real-estate project material workflows. Goal: fast, safe, minimal-impact changes aligned with
existing architecture.

Repo root: `/Users/bsridharpatnaik/GitHub/RealEstate-ERP-CRM-System`

> **Narrative "what was built" history for each shipped feature lives in [`docs/feature-log.md`](docs/feature-log.md)**
> (not auto-loaded). Read the relevant section there when working on a specific feature. This file
> holds only the durable rules, gotchas, and invariants that change how *any* task is approached.

---

## Active Scope (default)

Only analyze/modify these unless explicitly asked:
- `sc-inventory-service/src` — Java, Spring Boot, JPA/Hibernate, MySQL, multi-tenant. Domains: Inventory, Indents, POs, Inward, Outward, Stock, Service Orders, Quote Comparison, BOQ, Reports.
- `SC UI/src` — React (JS/TS).

Ignore by default: `sc-crm-service`, `sc-common-service`, build folders, generated files, logs, `graphify-out` (except when reading the graph).

---

## Core Working Rules

- Understand existing implementation before proposing new code; reuse existing patterns/naming/architecture.
- Prefer minimal diffs over rewrites. Modify existing files before creating new ones.
- **Show changes first; wait for explicit approval before applying** (and always before any `git commit`/`git push`).
- Never (unless asked): introduce frameworks/libraries, refactor unrelated code, rename working modules, change API contracts, change DB schema by assumption, modify out-of-scope modules.
- Search only allowed paths; find an existing similar feature first; avoid broad repo scans. Prefer Graphify (`graphify-out/GRAPH_REPORT.md`, `graphify-out/wiki/index.md`) for architecture questions.

---

## Multi-Tenant Schema Rules — understand before debugging ANY data issue

| Entity | Schema |
|---|---|
| `IndentInventory`, `IndentInventoryList` | **Master** (`@UseDefaultTenant` on `IndentInventoryService`) |
| `PurchaseOrder`, `PurchaseOrderLine` | **Master** (`@UseDefaultTenant` on `PurchaseOrderService`, `PurchaseOrderLifecycleManager`) |
| `IndentStatusUpdater` | **Master** (class-level) |
| `QuoteComparison*`, `ServiceOrder*` | **Master** (`@UseDefaultTenant` on their services) |
| Inward, Outward, Stock, LostDamaged, MOR, InventoryTransfer, InventoryBatch, BOQUpload | **Project (tenant)** |
| Activity logs | Project (per-tenant) + synced to master `global_activity_log` |

Indents/POs/QuoteComparison/ServiceOrders are **global (master schema)** entities accessible across
all projects. Inward/Outward/Stock/Batch/BOQ are **project-specific**. Do NOT assume all entities are
in project schema. When a tenant-context issue is suspected, check which schema the entity lives in first.

Tenant schemas are named after the tenant code (e.g. `drgtrdcntr`). Master schema: `masterschema`.

---

## Critical Gotchas & Invariants

### Tenant context — `TenantAspect` (do NOT revert)
`@UseDefaultTenant` (class or method) is handled by `TenantAspect.applyDefaultTenant` as an **`@Around`**
advice that saves the caller's tenant, switches to master, and **restores it in `finally`**. This scopes
the master switch to just the annotated method. Root cause it fixes: a call into a `@UseDefaultTenant`
service (e.g. `ProductService.findSingleProduct()`) used to leave `ThreadLocalStorage` pinned to master
for the rest of the request thread, so a later `activityLogService.record()` saved to the wrong schema.
**Do NOT revert to `@Before`** — the save/restore is essential.

```java
@Around("@annotation(UseDefaultTenant) || @within(UseDefaultTenant)")
public Object applyDefaultTenant(ProceedingJoinPoint pjp) throws Throwable {
    String originalTenant = ThreadLocalStorage.getTenantName();
    tenantService.setDefaultTenant();
    try { return pjp.proceed(); }
    finally { ThreadLocalStorage.setTenantName(originalTenant); }
}
```

### `resolveCurrentUser` pattern (every service that logs)
```java
private String resolveCurrentUser() {
    try { return userDetailsService.getCurrentUser().getUsername(); }
    catch (Exception e) { return "System"; }
}
```
Call it **BEFORE** `activityLogService.record()` — username must be resolved on the calling thread.
`record()` is synchronous, captures tenant from ThreadLocal, and delegates the async DB write to
`ActivityLogWriter.saveAsync(entry, tenant)` with tenant passed explicitly.

### `TenantNameInterceptor` — excluded URL patterns
Any endpoint URL matching an excluded pattern gets tenant forced to **masterschema** (ignores the
`tenant-id` header) → silently queries the wrong schema. Known patterns include `.*/product.*` and
`.*/indent.*`. **Do NOT put `product`/`indent` in a new tenant-scoped endpoint URL** (e.g. use
`/boq-summary`, not `/product-boq-summary`). If a new endpoint returns empty/wrong data despite correct
DB rows, check the interceptor's excluded-pattern list first.

### Cross-schema queries in a tenant-pinned service
A class-level `@Transactional` (javax) service pins the connection to the tenant schema. To query BOTH
tenant and master in one method: annotate the method `@Transactional(TxType.NOT_SUPPORTED)` to suspend
the class transaction, then use `EntityManager.createNativeQuery()` — **fully-qualified**
(`masterschema.indent_inventory_entries`) for master tables, **unqualified** for tenant tables (routed
by ThreadLocal). Example: `BOQService.getProductBOQSummary()`.
**Self-call warning:** `@Cacheable`/`@Transactional` are bypassed when a method is called from within
the same bean (no AOP proxy). Inject self or use direct EntityManager queries.

### Global Activity Log visibility for master-logged entities
Master-schema services (`PurchaseOrderService`, `IndentInventoryService`, `QuoteComparisonService`,
`ServiceOrderService`, …) log with `tenantSchema = masterschema`. `ActivityLogGlobalSyncService`
filters `tenantSchema IN allowedSchemas`, and `getCurrentUserAllowedSchemas()` returns only project
tenants → master-logged entities (SERVICE_ORDER, PURCHASE_ORDER, INDENT, QUOTE_COMPARISON, PRODUCT)
were filtered out. Fix: `allowedSchemasForGlobalView()` appends master schema — **but only for admins**
(`hasRole(ADMIN)`), because `/global/list` + `/global/export` are not role-guarded at the controller.

### Native SQL — table-name casing (Linux/QA safety)
Local Mac MySQL is case-insensitive (`lower_case_table_names=2`); QA/prod Linux MySQL is case-sensitive
(`=0`). A query that works locally can fail on QA on casing alone. **Hibernate-created tables use exact
class-name PascalCase; manually-created tables are all-lowercase snake_case.** Applies to fully-qualified
refs too (`masterschema.Product`, NOT `masterschema.product`).

| Correct | Correct | Correct |
|---|---|---|
| `Product` | `Category` | `Firm` |
| `BOQUpload` | `Usage_Location` | `contacts` |
| `purchase_order` | `purchase_order_line` | `indent_inventory` |
| `indent_inventory_entries` | `inward_inventory` | `outward_inventory` |
| `inventory_batch` | `stock_summary` | `building_type`, `usage_area`, `location` |

Verify any new table name with `SHOW TABLES` before committing.

### Native SQL — column-name convention (mixed!)
masterschema columns are camelCase (Hibernate): `measurementUnit`, `categoryId`, `contactId`, `productId`.
The `product` table (tenant + master) is **mixed**: `product_code`, `product_name` (snake) but
`measurementUnit`, `categoryId`, `productId` (camel). Always `SHOW COLUMNS FROM <table>` before writing
native SQL. In `@Query(nativeQuery=true)`/HQL, use Java field names (camelCase, e.g. `outwardId`) not
snake_case; use `COUNT(r)` not positional `ORDER BY 2`.

### `contacts` table — no `supplier` table exists
Suppliers AND contractors both live in `contacts`, distinguished by `contacttype`. `Supplier` entity →
table `contacts`, PK `contactId`, filter `contacttype='supplier'`. `PurchaseOrder.supplier_id` → FK to
`contacts.contactId`; `firm_id` → `Firm.id`. In native SQL never join a `supplier` table:
```sql
LEFT JOIN contacts s ON s.contactId = po.supplier_id AND s.contacttype = 'supplier' AND s.is_deleted = 0
```

### Frontend routing gotchas
- Global/cross-tenant pages (all Reports, `/activityLog`) must be in the `isProjectSelectionPage` list in `Shared/SideMenu/index.js`, else Redux tenant state renders them in project context.
- `Home/index.js` uses `key={new Date()}` on routed components → they **remount on every parent re-render**, wiping `history.push(url, state)`. For cross-route handoff use **sessionStorage** (set before navigate, read in `componentDidMount`, clear on back).

### Product-metadata sync (for any master sync table)
Incremental syncs only touch modified rows, so product renames/unit changes go stale. After the upsert,
query `DISTINCT productId` from the master table for that tenant, fetch fresh `Product`s, bulk-`UPDATE`
`productName`/`unit`. Done in `FifoReportSyncService` (Step 6) and `LowStockSyncService`.

### Product Merge — must cover EVERY table that references a product
Product merge (`ProductMergeService` + `ProductMergeTenantExecutor`) reassigns all source-product FKs
to the survivor, then soft-deletes the source `Product`. It hand-writes native SQL per table — there is
no framework sweep — so it silently goes stale whenever a new product-referencing table is added.

**Whenever you add or discover a table/entity with a `productId`/`product_id` column (or a `@ManyToOne`
to `Product`), you MUST also update `ProductMergeTenantExecutor`:**
1. Decide its schema: tenant table → `executeTenantMerge` + `countUsages`; master table → `executeMasterMerge`
   + `countMasterUsages` (see the Multi-Tenant Schema table — Indents/POs/QuoteComparison/ServiceOrders/Product
   are master; Inward/Outward/Stock/Batch/BOQ are tenant).
2. Add the `UPDATE … SET productId=<target> WHERE productId=<source>` (also refresh denormalized
   `productName`/`productCode`/`measurementUnit`/`category` columns if the table has them).
3. Add a matching `COUNT(*)` to the preview so the impact table shows it, and surface it in the UI
   (`SC UI/src/Modules/ProductMerge/index.js` — a column + `totalUsage()`).
4. **Exceptions that do NOT need a reassign:** `@Subselect`/`@Immutable` views (derived), and
   full-rebuild sync tables that `deleteByTenantSchema` then rebuild (aging/dead/expired/low-stock reports
   self-heal). **Incremental** sync tables (`global_fifo_report`) DO need reassign — they never self-heal.
   The `all_inventory` rollup (proc `update_all_inventory()`) is refreshed per tenant after merge.

**Table-name casing is load-bearing here** (see native-SQL casing gotcha above). The merge broke on QA
because it queried `stock` but the real Hibernate table is `Stock` (the `Stock` entity has no `@Table`,
so the table = class name). **Before writing/merging any native SQL, verify the exact table name** with
`SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=… AND LOWER(TABLE_NAME)=LOWER('…')`.
Entities without `@Table` → PascalCase class-name table (`Stock`, `Product`, `BOQUpload`,
`InventoryMonthPriceMapping`); manually-created tables are lowercase snake_case.

---

## Batch Tracking — core model & invariants

`BatchMode` enum: `NONE` (not tracked), `BATCH_ONLY` (FIFO by `receivedDate`), `BATCH_WITH_EXPIRY`
(FEFO by `expiryDate`). `isBatchTracked()` = mode ≠ NONE; UI shows Batches tab only then.

`InventoryBatch` (table `inventory_batch`): `qtyReceived` (set at creation, never changes),
`qtyRemaining` (reduced by consumption/write-offs), editable `brand`/`lotNumber`/`expiryDate`/
`receivedDate` (dates `dd-MM-yyyy`; `receivedDate` NOT NULL). `daysUntilExpiry`/`isExpired` are `@Transient`.

**`inwardId` sentinel values — do not confuse:** `>0` normal inward · `-1L` stock split · `0L` inventory-transfer destination.

**Key invariant:** `batch.qtyRemaining == batch.qtyReceived − sum(all downstream consumptions)` (outwards,
lost/damaged, write-offs, transfer-outs, rejects). Any op creating/reversing consumption must update
`qtyRemaining` atomically in the same transaction.

**FIFO override detection = order AND qty** — correct batch order with wrong quantities is still an
override (`!isFifoOrder || !isFifoQty`, in both `OutwardInventoryService` and `BatchTrackingService`).

Bug-fixes to preserve:
1. `hasFifoOverride` must be reset to `false` before the outward update path (else stale badge).
2. `deleteByOutwardId` must be bulk JPQL `@Modifying` (entity-by-entity → `LazyInitializationException`).
3. Return path must reduce/delete matching `OutwardBatchConsumption` rows, not just restore `qtyRemaining`.
4. `edit.js` reloads `batchConsumptionData` on mount; re-sends `overrideBatches` when qty unchanged.
5. Untracked-count logic in `StockService.expiryFilter` calls repos directly (not `BatchTrackingService`) to avoid a circular dep.

**Untracked stock**: `totalQuantityInHand > sum(qtyRemaining)` for a tracked product → outward blocked
until user splits into named batches (split batches have `inwardId = -1`).

Date convention (`Stock/details.js`): backend `dd-MM-yyyy` ↔ `<input type="date">` `yyyy-MM-dd` via
`ddmmyyyyToInputDate` and `toBackendDate = s => s.split('-').reverse().join('-')`.

---

## Reports Module — shared infrastructure & conventions

- **`Modules/Reports/projectColors.js`** — `getProjectColor(tenantCode)` → `{bg,color,border}`, deterministic hash on the **raw tenantCode** (stable across display-name changes). Imported by all report card/table files.
- **Tenant map**: report index pages fetch `GET /user/allowedtenants`, build `tenantMap = {tenantCode → tenantName}` (`filter(t => t.inventory === true)`), pass to children; cards use `(tenantMap && tenantMap[code]) || code`.
- **Filters use exact match** for autocomplete-sourced fields: `SpecificationsBuilder.whereDirectFieldEquals()` (product/category/warehouse/contractor/performedBy); `whereDirectFieldContains()` only for free-text.
- **Multi-select filters (native-SQL reports)**: frontend sends `attrValue: ["A","B"]`; backend builds `= :p0` for one value or `IN (:p0,:p1,…)` for many. Handle both.
- **Default filter badge**: PoReconciliation + IndentFulfillment load a pre-populated default filter (`usingDefaultFilter: true`); dismissible × clears it.
- **All report list pages** set `pageSize = 100` (override `ListCommon` default of 12).
- Sync-based reports use an `AtomicBoolean` guard + `JobExecutionLog`; live reports (PoRecon, IndentFulfillment) use native SQL via EntityManager with manual `COUNT(*) FROM (subquery)` pagination.

---

## BOQ — definitions & schema reference

BOQ (Bill of Quantities) tracks planned vs actual material usage per project. `BOQUpload` is a **tenant
schema** table. `BOQService` is class-level `@Transactional`; cross-schema methods use
`@Transactional(TxType.NOT_SUPPORTED)` + native queries (see cross-schema gotcha above).

**"Indented" = every indent line really raised** (Session 10). Filter at all 3 sites in `BOQService`:
exclude header `indent_status IN (CANCELLED, REJECTED)` + line `line_item_status = CANCELLED` only —
**SHORT CLOSED is counted** (it's real demand). Sites: `getBOQIndentSummary()` (`/boqIndentReport`),
`getBOQTrackerSummary()` (`/boqTracker` + export), `getProductBOQSummary()` ("BOQ Remaining" chip on
Indent create form + details).

BOQ vs Indent report status breakup: identity **Requested = Received + Pending + ShortClosed**;
`ShortClosed = SUM(GREATEST(quantity − quantity_received, 0))` over SHORT CLOSED lines; `Pending`
derived in Java so it always reconciles. Tracker's "Inward Received" = indent-line `quantity_received`
(indent→PO→inward flow), **not** physical `inward_inventory` — uniform with the report.

`BOQUpload` (tenant schema): `id` PK, `productId` → `Product.productId`, `buildingTypeId` →
`BuildingType.id` (`building_type`), `usageLocationId` → `UsageLocation.id` (`Usage_Location`),
`locationId` → `UsageArea.id` (`usage_area`), `quantity`, `wastagePercent` (planned =
`quantity*(1+wastagePercent/100)`), `sno`, `is_deleted`.

Location joins: `outward_inventory.locationId` = BOQ `usageLocationId` (structure);
`outward_inventory.usageAreaId` = BOQ `locationId` (final location). Outward → building type:
`outward_inventory.locationId → Usage_Location.id → Usage_Location.typeId → building_type.id`.
`indent_inventory_entries` has NO location FKs — indent rolls up at product level only.

---

## Pending Deploy Migrations — Standing Convention

No Flyway/Liquibase. `spring.jpa.hibernate.ddl-auto=none` in `application.properties` is **not** the
effective runtime setting — `multitenant/AutoDDLConfig.java` builds its own per-tenant routing
`DataSource`/`EntityManagerFactory` for every schema in `schemas.map` and hardcodes
`hibernate.hbm2ddl.auto=update`. So:

- **Additive changes (new column/table) auto-apply on backend startup** — no manual SQL needed.
- **Hibernate `update` never drops/renames** — dropped/renamed columns, FK changes, and data
  backfills/corrections must be applied by hand.

**Single canonical file:** `sc-inventory-service/src/main/resources/SQLs/PendingDeployMigrations.sql`.
Append any **non-additive** schema change or one-off data correction here (with a comment: what/why +
target schema). Do not create per-feature migration files. Purely additive changes need no entry.

User's workflow: after deploying code, they run everything in this file by hand against the target
env(s), then empty it (keeping the header) once confirmed. File contents at any time = what's still
outstanding. **Do NOT stop populating it just because it looks empty** — empty means "nothing pending."

---

## Local Database Access

- Host `127.0.0.1`, user `root`, password `REDACTED` (reset June 2026 via skip-grant-tables).
- Connect: `/opt/homebrew/opt/mysql@8.0/bin/mysql --user=root --password=REDACTED --host=127.0.0.1`
- MySQL runs via LaunchAgent `~/Library/LaunchAgents/homebrew.mxcl.mysql@8.0.plist`
  (`launchctl load`/`unload` to start/stop).
- Local UI login: `sridhar` / `Test@123` at `localhost:3000`.
- **Never auto-start dev servers** (8090/8091/3000) — user runs them manually; verify via compile + DB.

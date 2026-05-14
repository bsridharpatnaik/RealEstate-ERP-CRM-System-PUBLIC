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

**Root cause**: `ProductService` is `@UseDefaultTenant` at class level. Any call to it (e.g. `findSingleProduct()` inside `populateData()`) sets `ThreadLocalStorage` to master schema. By the time `activityLogService.record()` was submitted to the async executor, `TenantAwareTaskDecorator` captured master schema → log saved to wrong place.

**Fix**: `ActivityLogService.record()` is now synchronous — captures tenant at call site before any `@UseDefaultTenant` fires. Delegates actual async DB write to `ActivityLogWriter.saveAsync(entry, tenant)` with tenant passed explicitly. No reliance on `TenantAwareTaskDecorator` for this flow.

**Do NOT** put `@Async` back on `ActivityLogService.record()`. Keep it synchronous.

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
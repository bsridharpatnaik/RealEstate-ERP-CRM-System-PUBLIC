# ERP/CRM Project — Codex Context

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

## Local Database

- Host: `localhost`
- Username: `root`
- Password: `REDACTED`
- Connect: `mysql -h localhost -u root -pREDACTED`
# ERP Inventory — UI Permissions Documentation

**Roles:** `admin` · `purchase-manager` · `management` · `project-manager` · `store-incharge`

**Reading this doc:**
- ✅ = action visible and available
- ❌ = action hidden (entry point not rendered)
- `—` = not applicable for that role
- ⚠️ = gap: action visible to all roles, no role check implemented in UI
- Date-gated = button exists but is **disabled** (not hidden) after the role-specific edit window expires

---

## Part 1 — Side Menu

The menu renders in two modes: **Global** (no project selected) and **Project-level** (after selecting a project).

### 1A. Global Menu (no project selected)

| Menu Item | Path | admin | purchase-manager | management | project-manager | store-incharge |
|---|---|:---:|:---:|:---:|:---:|:---:|
| Global Dashboard | `/globalDashboard` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Projects | `/project` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Stock Summary | `/deadStock` | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Indent & PO** *(parent)* | — | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Global Indent | `/globalIndent` | ✅ | ✅ | ✅ | ✅ | ❌ |
| → Purchase Order | `/purchaseOrder` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Inventory Transfer | `/inventoryTransfer` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Historical Pricing | `/historicalPricing` | ✅ | ✅ | ✅ | ❌ | ❌ |
| **Admin** *(parent)* | — | ✅ | ❌ | ❌ | ❌ | ❌ |
| → User Management | `/user` | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Global Config** *(parent)* | — | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Firm | `/firm` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Category | `/category` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Product (Inventory) | `/inventory` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Machinery | `/machinery` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Contact | `/contact` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Logout | — | ✅ | ✅ | ✅ | ✅ | ✅ |

> **Note:** `isInventory` flag (tenant has inventory enabled) gates the entire project-level inventory section. All 5 inventory roles satisfy `isInventory`. The global menu has no `isInventory` check — items are always shown.

---

### 1B. Project-Level Menu (after selecting a project)

`isInventory` = tenant has inventory enabled AND role is one of the 5 inventory roles.

| Menu Item | Path | admin | purchase-manager | management | project-manager | store-incharge |
|---|---|:---:|:---:|:---:|:---:|:---:|
| Dashboard | `/dashboard` | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Inventory Management** *(parent)* | — | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Stock | `/stock` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → All Inventory | `/allInventory` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Indent | `/indent` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Inward Inventory | `/inwardInventory` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Outward Inventory | `/outwardInventory` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Machinery on Rent | `/machineryOnRent` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Lost / Damaged | `/lost` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Pricing *(parent)* | — | ✅ | ✅ | ✅ | ✅ | ✅ |
| → → Pricing Inventory | `/pricingInventory` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → → Pricing Report | `/pricingReport` | ✅ | ✅ | ✅ | ✅ | ✅ |
| **BOQ** *(parent)* | — | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Input | `/boq` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Status | `/boqStatus` | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Settings** *(parent)* | — | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Change Password | *(modal)* | ✅ | ✅ | ✅ | ✅ | ✅ |
| → Admin → User | `/user` | ✅ | ❌ | ❌ | ❌ | ❌ |
| → Inventory Settings *(parent)* | — | ✅ | ✅ | ✅ | ✅ | ✅ |
| → → Warehouse | `/warehouse` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → → Building Type | `/buildingType` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → → Usage Location | `/usageLocation` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → → Final Location (Usage Area) | `/usageArea` | ✅ | ✅ | ✅ | ✅ | ✅ |
| → → Machinery | `/machinery` | ✅ | ✅ | ✅ | ✅ | ✅ |
| Logout | — | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## Part 2 — Module Actions

> **Scope:** Only entry-point actions are listed (Add, Edit, Delete, Approve, Reject, Cancel, Export, Print, Import, Short Close). Save, Cancel form, and Close/Back buttons are excluded per requirement.

---

### 2.1 Purchase Order

**Location:** Global menu → Indent & PO → Purchase Order (`/purchaseOrder`)

#### List View

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Add Purchase Order | ✅ | ✅ | ❌ | ❌ | ❌ |
| Edit (table row icon) | ✅ | ✅ | ❌ | ❌ | ❌ |
| Delete (table row icon) | ✅ | ✅ | ❌ | ❌ | ❌ |
| Drafts button | ✅ | ✅ | ❌ | ❌ | ❌ |
| Filter | ✅ | ✅ | ✅ | ✅ | ✅ |
| Export to Excel | ✅ | ✅ | ✅ | ✅ | ✅ |
| Open Detail | ✅ | ✅ | ✅ | ✅ | ✅ |

> `canEditInventoryModules()` = admin, purchase-manager gates Add / Edit / Delete / Drafts.

#### Detail Panel (More ⋮ menu)

| Action | Condition | admin | purchase-manager | management | project-manager | store-incharge |
|---|---|:---:|:---:|:---:|:---:|:---:|
| Print with Rates | admin / purchase-manager / management only | ✅ | ✅ | ✅ | ❌ | ❌ |
| Print without Rates | All roles | ✅ | ✅ | ✅ | ✅ | ✅ |
| Edit | PO status = **NEW** | ✅ | ✅ | ❌ | ❌ | ❌ |
| Short Close | PO status = **PARTIAL** | ✅ | ✅ | ❌ | ❌ | ❌ |
| Delete | PO status ≠ CANCELLED | ✅ | ✅ | ❌ | ❌ | ❌ |

> Money columns (Rate, GST, Net Rate, Total, Grand Total) in the detail view are hidden for `project-manager` and `store-incharge` via `canViewMoneyFields()`.

---

### 2.2 Indent (Global View)

**Location:** Global menu → Indent & PO → Global Indent (`/globalIndent`)

> The Global Indent page is a **read-only view** for all roles — no Add button is shown because `isGlobal=true` suppresses it. All roles that can see the menu item can view the list and details.

#### List View

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Add Indent | ❌ | ❌ | ❌ | ❌ | ❌ |
| Filter | ✅ | ✅ | ✅ | ✅ | — |
| Export to Excel | ✅ | ✅ | ✅ | ✅ | — |
| Open Detail | ✅ | ✅ | ✅ | ✅ | — |
| Edit (row) | ✅* | ✅* | ❌ | ❌ | — |
| Cancel (row) | ✅* | ✅* | ❌ | ✅* | — |

*Status-dependent — see detail panel below.

---

### 2.3 Indent (Project View)

**Location:** Project menu → Inventory Management → Indent (`/indent`)

#### List View

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Add Indent | ✅ | ✅ | ❌ | ❌ | ✅ |
| Filter | ✅ | ✅ | ✅ | ✅ | ✅ |
| Export to Excel | ✅ | ✅ | ✅ | ✅ | ✅ |
| Open Detail | ✅ | ✅ | ✅ | ✅ | ✅ |
| Edit (row icon) | status-dep. | status-dep. | ❌ | ❌ | status-dep. |
| Cancel/Delete (row icon) | status-dep. | status-dep. | ❌ | status-dep. | status-dep. |

**Edit row icon** — `canEditIndentRecord(status)`:

| Status | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| NEW | ✅ | ✅ | ❌ | ✅ | ✅ |
| APPROVED | ✅ | ✅ | ❌ | ✅ | ❌ |
| PO Partial / PO Completed / Closed / any other | ❌ | ❌ | ❌ | ❌ | ❌ |

**Cancel row icon** — `canCancelIndentRecord(status)`:

| Status | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| NEW | ✅ | ✅ | ❌ | ✅ | ✅ |
| APPROVED | ✅ | ✅ | ❌ | ✅ | ❌ |
| Any other | ❌ | ❌ | ❌ | ❌ | ❌ |

#### Detail Panel

| Action | Condition | admin | purchase-manager | management | project-manager | store-incharge |
|---|---|:---:|:---:|:---:|:---:|:---:|
| Print | Always | ✅ | ✅ | ✅ | ✅ | ✅ |
| Approve | Status = **NEW** | ✅ | ✅ | ❌ | ✅ | ❌ |
| Reject | Status = **NEW** | ✅ | ✅ | ❌ | ✅ | ❌ |
| Cancel Indent | Status = NEW or APPROVED | ✅ | ✅ | ❌ | ✅* | ✅** |
| Re-submit as New | Status = **REJECTED** | ✅ | ✅ | ❌ | ❌ | ✅ |

*project-manager: NEW and APPROVED
**store-incharge: NEW only

---

### 2.4 Inward Inventory

**Location:** Project menu → Inventory Management → Inward Inventory (`/inwardInventory`)

#### List View — Add Button

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Add Inward *(dropdown)* | ✅ | ❌ | ❌ | ❌ | ✅ |
| → Inward from PO | ✅ | ❌ | ❌ | ❌ | ✅ |
| → Direct Inward | ✅ | ❌ | ❌ | ❌ | ✅ |
| → Sample Inward | ✅ | ❌ | ❌ | ❌ | ✅ |
| Filter | ✅ | ✅ | ✅ | ✅ | ✅ |
| Export | ✅ | ✅ | ✅ | ✅ | ✅ |
| Open Detail | ✅ | ✅ | ✅ | ✅ | ✅ |

#### Detail Panel

| Action | Condition | admin | purchase-manager | management | project-manager | store-incharge |
|---|---|:---:|:---:|:---:|:---:|:---:|
| Edit | Within edit window | ✅ | ❌ | ❌ | ❌ | ✅ |
| Delete | Within edit window | ✅ | ❌ | ❌ | ❌ | ✅ |
| Add Reject | Within edit window | ✅ | ❌ | ❌ | ❌ | ✅ |
| Print Inward | Always | ✅ | ✅ | ✅ | ✅ | ✅ |
| Print Reject Note | Only if reject exists | ✅ | ✅ | ✅ | ✅ | ✅ |

> Edit / Delete / Add Reject are hidden for non-authorized roles via `canCreateInward()`. Additionally, even for authorized roles, buttons are **disabled** (not hidden) if the record is older than the role's edit window (`getRoleEditConstraintDays()`).

---

### 2.5 Outward Inventory

**Location:** Project menu → Inventory Management → Outward Inventory (`/outwardInventory`)

#### List View — Add Button

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Add Outward | ✅ | ❌ | ❌ | ❌ | ✅ |
| Filter | ✅ | ✅ | ✅ | ✅ | ✅ |
| Export | ✅ | ✅ | ✅ | ✅ | ✅ |
| Open Detail | ✅ | ✅ | ✅ | ✅ | ✅ |

#### Detail Panel

| Action | Condition | admin | purchase-manager | management | project-manager | store-incharge |
|---|---|:---:|:---:|:---:|:---:|:---:|
| Edit | Within edit window | ✅ | ❌ | ❌ | ❌ | ✅ |
| Delete | Within edit window | ✅ | ❌ | ❌ | ❌ | ✅ |
| Add Reject | Within edit window | ✅ | ❌ | ❌ | ❌ | ✅ |
| Add Return | Within edit window | ✅ | ❌ | ❌ | ❌ | ✅ |
| Print Outward | Always | ✅ | ✅ | ✅ | ✅ | ✅ |
| Print Debit Note | Only if reject exists | ✅ | ✅ | ✅ | ✅ | ✅ |
| Print Return Note | Only if return exists | ✅ | ✅ | ✅ | ✅ | ✅ |

---

### 2.6 Contact

**Location:** Global menu → Global Config → Contact (`/contact`)

#### List View

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Add Contact | ✅ | ✅ | ❌ | ❌ | ✅ |
| Filter | ✅ | ✅ | ✅ | ✅ | ✅ |
| Export to Excel | ✅ | ✅ | ✅ | ✅ | ✅ |
| Open Detail | ✅ | ✅ | ✅ | ✅ | ✅ |

#### Detail Panel

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Edit | ✅ | ✅ | ❌ | ❌ | ✅ |
| Delete | ✅ | ✅ | ❌ | ❌ | ✅ |

---

### 2.7 Product (Inventory)

**Location:** Global menu → Global Config → Inventory (`/inventory`)

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Add Product | ✅ | ✅ | ❌ | ❌ | ❌ |
| Edit (list) | ✅ | ✅ | ❌ | ❌ | ❌ |
| Delete (list) | ✅ | ✅ | ❌ | ❌ | ❌ |
| Filter | ✅ | ✅ | ✅ | ✅ | ✅ |

---

### 2.8 Category

**Location:** Global menu → Global Config → Category (`/category`)

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Add Category | ✅ | ✅ | ❌ | ❌ | ❌ |
| Edit (list) | ✅ | ✅ | ❌ | ❌ | ❌ |
| Delete (list) | ✅ | ✅ | ❌ | ❌ | ❌ |

---

### 2.9 Machinery (Global Config)

**Location:** Global menu → Global Config → Machinery (`/machinery`)

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Add Machinery | ✅ | ✅ | ❌ | ❌ | ❌ |
| Edit (list) | ✅ | ✅ | ❌ | ❌ | ❌ |
| Delete (list) | ✅ | ✅ | ❌ | ❌ | ❌ |

---

### 2.10 Firm

**Location:** Global menu → Global Config → Firm (`/firm`)

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Add Firm | ✅ | ✅ | ❌ | ❌ | ❌ |
| Edit Firm | ✅ | ✅ | ❌ | ❌ | ❌ |

---

### 2.11 Stock

**Location:** Project menu → Inventory Management → Stock (`/stock`)

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Filter | ✅ | ✅ | ✅ | ✅ | ✅ |
| Export | ✅ | ✅ | ✅ | ✅ | ✅ |
| Open Detail | ✅ | ✅ | ✅ | ✅ | ✅ |

> Stock is **read-only** for all roles — no Add / Edit / Delete buttons exist.

---

### 2.12 Stock Summary (Global)

**Location:** Global menu → Stock Summary (`/deadStock`)

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Filter | ✅ | ✅ | ✅ | ✅ | ✅ |
| Export to Excel | ✅ | ✅ | ✅ | ✅ | ✅ |
| Import Reorder Levels | ⚠️ | ⚠️ | ⚠️ | ⚠️ | ⚠️ |
| Sync Stock | ⚠️ | ⚠️ | ⚠️ | ⚠️ | ⚠️ |

> ⚠️ **Gap:** Import and Sync have no role check in the UI. These should be restricted to admin and purchase-manager.

---

### 2.13 Inventory Transfer

**Location:** Global menu → Indent & PO → Inventory Transfer (`/inventoryTransfer`)

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Add Transfer | ⚠️ | ⚠️ | ⚠️ | ⚠️ | ⚠️ |

| Filter | ✅ | ✅ | ✅ | ✅ | ✅ |
| Export | ✅ | ✅ | ✅ | ✅ | ✅ |
| Open Detail | ✅ | ✅ | ✅ | ✅ | ✅ |

> ⚠️ **Gap:** No role check on Add Transfer in UI or backend. Needs role restriction (likely admin + store-incharge).

---

### 2.14 Machinery on Rent

**Location:** Project menu → Inventory Management → MOR (`/machineryOnRent`)

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Add MOR | ✅ | ❌ | ❌ | ❌ | ✅ |
| Edit (detail) | ✅ | ❌ | ❌ | ❌ | ✅ |
| Delete (detail) | ✅ | ❌ | ❌ | ❌ | ✅ |
| Filter / Export | ✅ | ✅ | ✅ | ✅ | ✅ |

---

### 2.15 Lost / Damaged Inventory

**Location:** Project menu → Inventory Management → Lost (`/lost`)

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Add Lost Entry | ✅ | ❌ | ❌ | ❌ | ✅ |
| Edit (detail) | ✅ | ❌ | ❌ | ❌ | ✅ |
| Delete (detail) | ✅ | ❌ | ❌ | ❌ | ✅ |
| Filter / Export | ✅ | ✅ | ✅ | ✅ | ✅ |

---

### 2.16 Settings — Warehouse

**Location:** Project menu → Settings → Inventory Management → Warehouse (`/warehouse`)

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Add Warehouse | ⚠️ | ⚠️ | ⚠️ | ⚠️ | ⚠️ |
| Edit Warehouse | ⚠️ | ⚠️ | ⚠️ | ⚠️ | ⚠️ |

---

### 2.17 Settings — Usage Location / Final Location / Building Type

**Location:** Project menu → Settings → Inventory Management

| Action | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Add / Edit / Delete | ⚠️ | ⚠️ | ⚠️ | ⚠️ | ⚠️ |

> These are configuration screens (Warehouse, Location, Final Location, Building Type). Backend has `@CheckAuthority` only — any authenticated user can write. UI has no role check either. Typically these should be admin / purchase-manager only.

---

## Part 3 — Data Visibility (Not Buttons)

Some data is hidden rather than a button being hidden. These are enforced in the UI via `canViewMoneyFields()`:

| Data Field | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Rate, GST %, Net Rate, Total Amount (PO detail) | ✅ | ✅ | ✅ | ❌ | ❌ |
| Grand Total (PO detail) | ✅ | ✅ | ✅ | ❌ | ❌ |
| Price columns (PO print) | ✅ | ✅ | ✅ | ❌ | ❌ |

---

## Part 4 — Edit Window (Date-Based Disable)

For Inward and Outward, even authorized users cannot edit/delete records older than their role-specific window. Buttons are **shown but disabled** (not hidden) once the window expires.

| Role | Edit Window |
|---|---|
| admin | `adminDays` (configured in Project Constants) |
| purchase-manager | `managerDays` (configured in Project Constants) |
| management, project-manager, store-incharge | `generalDays` (configured in Project Constants) |

---

## Part 5 — UI Gaps Summary

The following modules have **no role-based visibility control** on write actions in the UI. Backend may enforce, but the user sees the button, clicks it, and only gets a 403 error at the API level — bad UX.

| Module | Missing UI Check | Suggested Roles |
|---|---|---|
| ~~Inward Inventory — Add button~~ | ✅ Fixed | admin, store-incharge |
| ~~Outward Inventory — Add button~~ | ✅ Fixed | admin, store-incharge |
| ~~Firm — Add / Edit~~ | ✅ Fixed | admin, purchase-manager |
| ~~Machinery on Rent — Add / Edit / Delete~~ | ✅ Fixed | admin, store-incharge |
| ~~Lost / Damaged — Add / Edit / Delete~~ | ✅ Fixed | admin, store-incharge |
| Inventory Transfer — Add | ⚠️ Pending | admin, store-incharge (TBD) |
| Stock Summary — Import / Sync | ⚠️ Pending | admin, purchase-manager |
| Warehouse / Location / Final Location / Building Type — writes | ⚠️ Pending | admin, purchase-manager |

---

## Part 6 — Quick Reference: Helper Function → Roles

| `helper.js` Function | Roles that return `true` |
|---|---|
| `isAdmin()` | admin |
| `canEditInventoryModules()` | admin, purchase-manager |
| `canEditContactModules()` | admin, purchase-manager, store-incharge |
| `canCreateIndent()` | admin, purchase-manager, store-incharge |
| `canCreateInward()` | admin, store-incharge |
| `canApproveIndentRecord(status)` | admin, purchase-manager, project-manager *(NEW only)* |
| `canManagerRejectIndentRecord(status)` | admin, purchase-manager, project-manager *(NEW only)* |
| `canCancelIndentRecord(status)` | See section 2.3 table |
| `canEditIndentRecord(status)` | NEW → admin, purchase-manager, project-manager, store-incharge; APPROVED → admin, purchase-manager, project-manager; PO created or later → nobody |
| `canResubmitIndentRecord(status)` | admin, purchase-manager, store-incharge *(REJECTED only)* |
| `canViewMoneyFields()` | admin, purchase-manager, management |

---

*Document generated: 2026-04-03*
*Covers: SC-Web-UI frontend — src/Modules and src/Shared/SideMenu*

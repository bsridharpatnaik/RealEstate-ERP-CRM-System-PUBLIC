# Product Merge — Business Specification

**Version:** 1.0  
**Date:** 2026-05-11  
**Status:** Pending Business Confirmation  

---

## 1. Purpose

Over time, the same physical material may get registered in the system under two different names by different users — for example, "6 inch tape" and "6" tape". Both refer to the same product, but the system treats them as separate items, causing split stock, split purchase history, and inconsistent reporting.

The Product Merge feature allows an administrator to consolidate two duplicate products into one, reassigning all historical and current data to the surviving product, and removing the duplicate.

---

## 2. Who Can Use This Feature

Only **Admin users** can perform a product merge. This action is irreversible and affects data across the entire system.

---

## 3. How It Works — Overview

The administrator:
1. Selects the **duplicate product** (the one to be removed)
2. Selects the **surviving product** (the one to be kept)
3. Reviews a summary of all data that will be affected
4. Confirms the merge

The system then reassigns all records from the duplicate product to the surviving product and removes the duplicate.

---

## 4. What the Surviving Product Retains

After the merge, the surviving product keeps:
- Its own **name**
- Its own **unit of measurement**
- Its own **product code**
- Its own **category**
- Its own **reorder level settings**

The duplicate product's name, unit, and attributes are **not carried over** in any way.

---

## 5. Scenarios — Stock

Stock is tracked per product per warehouse (storage location). The following scenarios can occur:

### Scenario 5.1 — Both products have stock in the same warehouse

> **Example:** Duplicate product has 10 units in Warehouse A. Surviving product has 20 units in Warehouse A.

**Outcome:** The surviving product's stock in Warehouse A becomes **30 units** (10 + 20). The duplicate product's stock entry for Warehouse A is removed.

---

### Scenario 5.2 — Duplicate product has stock in a warehouse where the surviving product has no stock

> **Example:** Duplicate product has 15 units in Warehouse B. Surviving product has no stock in Warehouse B.

**Outcome:** The 15 units are **transferred to the surviving product** in Warehouse B. The surviving product now has 15 units in Warehouse B.

---

### Scenario 5.3 — Surviving product has stock in a warehouse where the duplicate has none

> **Example:** Surviving product has 25 units in Warehouse C. Duplicate product has no stock in Warehouse C.

**Outcome:** No change. The surviving product retains its 25 units in Warehouse C.

---

### Scenario 5.4 — Neither product has stock in a particular warehouse

**Outcome:** No stock-related change for that warehouse.

---

### Scenario 5.5 — Duplicate product has zero stock

**Outcome:** The zero stock record is removed. No quantities are affected.

---

## 6. Scenarios — Purchase Orders

### Scenario 6.1 — Open / pending purchase orders referencing the duplicate product

> **Example:** A PO was raised for "6 inch tape" (duplicate) and is still pending delivery.

**Outcome:** The line item is updated to reference "6" tape" (surviving product). The PO continues normally. No quantities or values change.

---

### Scenario 6.2 — Completed purchase orders referencing the duplicate product

**Outcome:** Historical records are updated to reference the surviving product. This ensures that purchase history, pricing history, and reports show a unified view under the surviving product.

---

## 7. Scenarios — Inward Entries (Material Received)

### Scenario 7.1 — Past inward entries recorded under the duplicate product

**Outcome:** All inward entries are reassigned to the surviving product. The quantity received, date, supplier, and all other details remain unchanged. Only the product reference changes.

---

## 8. Scenarios — Outward Entries (Material Issued)

### Scenario 8.1 — Past outward entries recorded under the duplicate product

**Outcome:** All outward entries are reassigned to the surviving product. Issue quantity, date, location, contractor, and all other details remain unchanged.

---

## 9. Scenarios — Indent (Material Requests)

### Scenario 9.1 — Open indents raised for the duplicate product

> **Example:** A site has raised an indent requesting "6 inch tape" (duplicate) which is not yet fulfilled.

**Outcome:** The indent line is updated to reference the surviving product. Fulfillment continues normally against the surviving product.

---

### Scenario 9.2 — Fulfilled or closed indents referencing the duplicate product

**Outcome:** Historical indent records are updated to reference the surviving product for reporting consistency.

---

## 10. Scenarios — BOQ (Bill of Quantities)

### Scenario 10.1 — Duplicate product appears in BOQ for a building type and location where the surviving product does NOT appear

**Outcome:** The BOQ entry is updated to reference the surviving product. Quantity and location remain unchanged.

---

### Scenario 10.2 — Both duplicate and surviving product appear in BOQ for the same building type and location

> **Example:** BOQ for "Block A – Flooring" has 100 units of "6 inch tape" and 50 units of "6" tape".

**Outcome:** The duplicate product's BOQ entry (100 units) is **removed**. The surviving product retains its own entry (50 units). The quantities are **not summed** — the surviving product's BOQ quantity is kept as-is.

**Reason:** BOQ quantities represent planned requirements. Summing them could inflate the planned quantity beyond what was actually planned for the surviving product.

---

### Scenario 10.3 — BOQ history entries referencing the duplicate product

**Outcome:** Historical BOQ change records are updated to reference the surviving product.

---

## 11. Scenarios — Material Transfer (Between Warehouses)

### Scenario 11.1 — Past transfer records referencing the duplicate product

**Outcome:** All transfer records are updated to reference the surviving product. Transfer quantities, dates, source and destination warehouses remain unchanged.

---

## 12. Scenarios — Lost / Damaged Inventory

### Scenario 12.1 — Lost or damaged entries recorded against the duplicate product

**Outcome:** These records are updated to reference the surviving product. Quantities and dates remain unchanged.

---

## 13. Scenarios — Pricing / Rate History

### Scenario 13.1 — Duplicate product has a price recorded for a month where the surviving product does NOT have a price

**Outcome:** The price entry is moved to the surviving product for that month.

---

### Scenario 13.2 — Both products have a price recorded for the same month

**Outcome:** The surviving product's price is kept. The duplicate product's price for that month is discarded.

---

## 14. Scenarios — Rejected Material (Inward / Outward Rejections)

### Scenario 14.1 — Rejection records referencing the duplicate product

**Outcome:** All rejection records are updated to reference the surviving product. Quantities, dates, and reasons remain unchanged.

---

## 15. What Happens to the Duplicate Product

After all data has been reassigned:
- The duplicate product is **marked as deleted** in the system
- It will no longer appear in any product lists, search results, or dropdowns
- Historical records will show the surviving product's name in place of the duplicate

The duplicate product is **not permanently erased** from the database — it is deactivated. This preserves audit integrity.

---

## 16. Stock Summary Recalculation

After the merge completes, the system automatically recalculates the global stock summary. This ensures that dashboards, reports, and stock levels reflect the merged state immediately.

---

## 17. Multi-Site / Multi-Project Scope

The merge is applied **across all projects and sites** in the system. If the duplicate product existed in 5 different project sites, all 5 are updated in a single merge operation.

---

## 18. Failure Handling

The merge is processed site by site. If the merge succeeds for some sites but fails for others:
- Successfully merged sites are saved
- Failed sites are reported to the administrator
- The administrator can retry the merge — it is safe to re-run (already-merged sites will be skipped)

---

## 19. Pre-Merge Validation

Before the merge is executed, the system will:
- Confirm both products exist and are active
- Prevent merging a product with itself
- Show a **preview** of all affected records (how many entries per category, per site) so the administrator can review before confirming

---

## 20. What Cannot Be Undone

This action **cannot be reversed** once confirmed. The administrator must review the preview carefully before proceeding. If there is any doubt, the action should not be performed.

---

## 21. Audit Trail

All changes made during a merge are recorded in the system's audit log with:
- Who performed the merge
- When it was performed
- Which product was merged into which

---

*Please review and confirm all scenarios above before development begins.*

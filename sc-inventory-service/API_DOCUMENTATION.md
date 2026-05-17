# ERP/CRM Inventory Service — API Documentation

**Base URL:** `/` (tenant-scoped via header/filter except where noted as `@UseDefaultTenant`)
**Auth:** All endpoints rely on a global auth filter. Method-level annotations add extra enforcement:
- `@CheckAuthority` — verifies the user is authenticated (token check)
- `@AllowOnly(roles={...})` — restricts to specific roles; returns 403 for others
- *No annotation* — no extra method-level gate (global filter still applies)

**Roles in system:** `admin` · `purchase-manager` · `management` · `project-manager` · `store-incharge`

---

## Legend

| Access Label | Meaning |
|---|---|
| **All roles** | No method-level restriction; any authenticated user can call |
| **Authenticated** | `@CheckAuthority` only; any authenticated user, no role filter |
| **admin, purchase-manager** | Restricted by `@AllowOnly` |
| **admin, store-incharge** | Restricted by `@AllowOnly` |
| *(specific list)* | Restricted by `@AllowOnly` with those exact roles |
| ⚠️ **No auth gate** | Neither annotation present — relies solely on global filter |

---

## 1. Purchase Order

**Controller:** `PurchaseOrderController`
**Base Path:** `/purchase-order`
**Tenant:** Master schema (`@UseDefaultTenant`)

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/purchase-order/create` | Create a new purchase order | admin, purchase-manager |
| POST | `/purchase-order` | Fetch paginated PO list with filters | All roles |
| GET | `/purchase-order/{id}` | Get a single PO by ID | All roles |
| PUT | `/purchase-order/{id}` | Update an existing PO | admin, purchase-manager |
| DELETE | `/purchase-order/{id}` | Cancel / delete a PO | admin, purchase-manager |
| POST | `/purchase-order/short-close` | Short-close a PO (mark remaining qty as closed) | ⚠️ All roles (no `@AllowOnly`) |
| GET | `/purchase-order/{id}/status-history` | Get status change history for a PO | All roles |
| POST | `/purchase-order/export/excel` | Export filtered PO list as Excel | All roles |
| GET | `/purchase-order/print-po/{id}` | Generate and download PO as PDF | All roles |
| POST | `/purchase-order/po-prioritize` | Manually trigger PO priority recomputation | admin, purchase-manager |
| GET | `/purchase-order/project-list` | Get list of all tenant/project schemas | All roles |

**Notes:**
- `print-po` accepts `?hideMoneyFields=true` query param; backend uses role to decide whether to mask prices in PDF
- `short-close` has no role gate — consider adding `@AllowOnly(ADMIN, PURCHASE_MANAGER)` if this should be restricted

---

## 2. Purchase Order — Historical Rates

**Controller:** `PurchaseOrderHistoryController`
**Base Path:** `/purchase-orders`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/purchase-orders/previous-rates` | Get previous purchase rates for a product | admin, purchase-manager, management |
| GET | `/purchase-orders/price-trend/scatter` | Get scatter chart data for price trend | All roles |
| GET | `/purchase-orders/po-rates` | Get all rates attached to a specific PO | All roles |

**Notes:**
- `/previous-rates` is the "Historical Pricing" feature — intentionally blocked from project-manager and store-incharge

---

## 3. Indent

**Controller:** `IndentInventoryController`
**Base Path:** `/indent`
**Tenant:** Master schema (`@UseDefaultTenant`)

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/indent/create` | Create a new indent | admin, purchase-manager, store-incharge |
| POST | `/indent` | Fetch paginated indent list with filters | All roles |
| GET | `/indent/{id}` | Get a single indent by ID | All roles |
| PUT | `/indent/{id}` | Update indent (role + status rules apply in service) | admin, purchase-manager, store-incharge |
| DELETE | `/indent/{id}` | Cancel indent (role + status rules apply in service) | admin, purchase-manager, project-manager, store-incharge |
| PATCH | `/indent/{indentId}/approve` | Approve / reject / cancel indent (outcome decided by service) | admin, purchase-manager, project-manager |
| PATCH | `/indent/{indentId}/split` | Split a line item in an indent | ⚠️ All roles (no `@AllowOnly`) |
| GET | `/indent/{indentId}/status-history` | Get status change history for an indent | All roles |
| GET | `/indent/open-indents/by-category` | Get consolidated open indents grouped by category | All roles |
| POST | `/indent/export/excel` | Export filtered indent list as Excel | All roles |

**Notes:**
- `DELETE` and `PATCH /approve` outcomes are decided by `IndentValidationService` based on role + current status, not just the annotation
- `split` has no role gate — currently open to all authenticated users

---

## 4. Inward Inventory

**Controller:** `InwardInventoryController`
**Base Path:** `/inward`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/inward/po/dropdown` | Get PO dropdown for inward creation | All roles |
| GET | `/inward/po/{poNumber}` | Fetch PO details for inward pre-fill | All roles |
| POST | `/inward/create/from-po` | Create inward from an approved PO | admin, store-incharge |
| POST | `/inward/create` | Create a direct inward entry | admin, store-incharge |
| POST | `/inward` | Fetch paginated inward list with filters | All roles |
| POST | `/inward/totals` | Get inward quantity/value totals for filters | All roles |
| POST | `/inward/export` | Export filtered inward list | All roles |
| GET | `/inward/{id}` | Get a single inward entry by ID | All roles |
| PUT | `/inward/{id}` | Update inward entry | admin, store-incharge |
| DELETE | `/inward/{id}` | Delete inward entry | admin, store-incharge |
| PATCH | `/inward/{id}` | Add a rejection entry to an inward | admin, store-incharge |
| POST | `/inward/opening-stock` | Create opening stock (backdoor/migration) | ⚠️ All roles (no auth gate) |

**Notes:**
- `opening-stock` endpoint has no auth gate — should be secured or removed if no longer needed for migration

---

## 5. Outward Inventory

**Controller:** `OutwardInventoryController`
**Base Path:** `/outward`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/outward/create` | Create a new outward entry | admin, store-incharge |
| POST | `/outward` | Fetch paginated outward list with filters | All roles |
| POST | `/outward/totals` | Get outward quantity totals for filters | All roles |
| POST | `/outward/export` | Export filtered outward list | All roles |
| GET | `/outward/{id}` | Get a single outward entry by ID | All roles |
| PUT | `/outward/{id}` | Update outward entry | admin, store-incharge |
| DELETE | `/outward/{id}` | Delete outward entry | admin, store-incharge |
| PATCH | `/outward/{id}` | Add return or rejection entry to an outward | admin, store-incharge |

---

## 6. Product

**Controller:** `ProductController`
**Base Path:** `/product`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/product` | Fetch filtered products with pagination | All roles |
| GET | `/product` | Get products list with optional filters | All roles |
| GET | `/product/{id}` | Get a single product by ID | All roles |
| POST | `/product/create` | Create a new product | admin, purchase-manager |
| PUT | `/product/{id}` | Update product details | admin, purchase-manager |
| DELETE | `/product/{id}` | Delete a product | admin, purchase-manager |
| GET | `/product/idandnames` | Get product ID + name pairs for dropdowns | All roles |
| GET | `/product/measurementunits/all` | Get all measurement units | All roles |
| GET | `/product/typeahead/{name}` | Typeahead search for products by name | All roles |
| GET | `/product/categorynames` | Get category names for dropdown | All roles |
| GET | `/product/{id}/all-tenant-reorder-configs` | Get reorder level configs across all tenants | All roles |
| PUT | `/product/{id}/tenant-reorder-config` | Save/update reorder level config for a tenant | admin, purchase-manager |
| DELETE | `/product/{id}/tenant-reorder-config/{tenantName}` | Remove reorder level config for a tenant | admin, purchase-manager |

---

## 7. Category

**Controller:** `CategoryController`
**Base Path:** `/category`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/category` | Fetch filtered categories with pagination | All roles |
| GET | `/category/{id}` | Get a single category by ID | All roles |
| POST | `/category/create` | Create a new category | admin, purchase-manager |
| PUT | `/category/{id}` | Update category | admin, purchase-manager |
| DELETE | `/category/{id}` | Delete category | admin, purchase-manager |
| GET | `/category/idandnames` | Get category ID + name pairs for dropdowns | All roles |

---

## 8. Contact (Supplier / Contractor)

**Controller:** `ContactController`
**Base Path:** `/contact`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/contact` | Fetch filtered contacts with pagination | All roles |
| GET | `/contact/{id}` | Get a single contact by ID | All roles |
| POST | `/contact/create` | Create a new contact | admin, purchase-manager, store-incharge |
| PUT | `/contact/{id}` | Update contact details | admin, purchase-manager, store-incharge |
| DELETE | `/contact/{id}` | Delete a contact | admin, purchase-manager, store-incharge |
| GET | `/contact/typeahead/globalsearch/{nameorno}` | Typeahead search by name or number | All roles |
| GET | `/contact/typeahead/namesearch/{name}` | Typeahead search by name only | All roles |
| POST | `/contact/export/excel` | Export contacts list as Excel | All roles |

---

## 9. Firm

**Controller:** `FirmController`
**Base Path:** `/firm`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/firm` | Get all firms | All roles |
| GET | `/firm/{id}` | Get a single firm by ID | All roles |
| POST | `/firm/create` | Create a new firm | admin, purchase-manager |
| PUT | `/firm/{id}` | Update firm details | admin, purchase-manager |
| GET | `/firm/idandnames` | Get firm ID + name pairs for dropdowns | All roles |

**Notes:**
- No DELETE endpoint on Firm

---

## 10. Machinery

**Controller:** `MachineryController`
**Base Path:** `/machinery`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/machinery` | Fetch filtered machineries with pagination | All roles |
| GET | `/machinery/{id}` | Get a single machinery by ID | All roles |
| POST | `/machinery/create` | Create a new machinery record | admin, purchase-manager |
| PUT | `/machinery/{id}` | Update machinery record | admin, purchase-manager |
| DELETE | `/machinery/{id}` | Delete machinery record | admin, purchase-manager |
| GET | `/machinery/idandnames` | Get machinery ID + name pairs for dropdowns | All roles |

---

## 11. Machinery On Rent

**Controller:** `MachineryOnRentController`
**Base Path:** `/mor`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/mor` | Fetch all machinery on rent with filters | All roles |
| POST | `/mor/export` | Export machinery on rent list | All roles |
| GET | `/mor/{id}` | Get single machinery on rent by ID | All roles |
| POST | `/mor/create` | Create a machinery on rent entry | Authenticated |
| PUT | `/mor/{id}` | Update machinery on rent entry | Authenticated |
| DELETE | `/mor/{id}` | Delete machinery on rent entry | Authenticated |

**Notes:**
- Write endpoints have `@CheckAuthority` only — no `@AllowOnly` role restriction. Any authenticated user can write. Consider scoping if needed.

---

## 12. Warehouse

**Controller:** `WarehouseController`
**Base Path:** `/warehouse`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/warehouse` | Get all warehouses with pagination | All roles |
| GET | `/warehouse/idandnames` | Get warehouse ID + name pairs for dropdowns | All roles |
| POST | `/warehouse/create` | Create a new warehouse | Authenticated |
| PUT | `/warehouse/{id}` | Update warehouse details | Authenticated |

**Notes:**
- Write endpoints are `@CheckAuthority` only — any authenticated user can create/update warehouses

---

## 13. Location (Usage Location)

**Controller:** `LocationController`
**Base Path:** `/location`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/location` | Fetch filtered locations with pagination | All roles |
| GET | `/location/{id}` | Get a single location by ID | All roles |
| GET | `/location/idandnames` | Get location ID + name pairs for dropdowns | All roles |
| GET | `/location/typeahead/{name}` | Typeahead search for locations | All roles |
| POST | `/location/create` | Create a new location | Authenticated |
| PUT | `/location/{id}` | Update location | Authenticated |
| DELETE | `/location/{id}` | Delete location | Authenticated |

---

## 14. Usage Area

**Controller:** `UsageAreaController`
**Base Path:** `/usagearea`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/usagearea` | Fetch filtered usage areas with pagination | All roles |
| GET | `/usagearea/{id}` | Get a single usage area by ID | All roles |
| GET | `/usagearea/idandnames` | Get usage area ID + name pairs for dropdowns | All roles |
| POST | `/usagearea/create` | Create a new usage area | Authenticated |
| PUT | `/usagearea/{id}` | Update usage area | Authenticated |
| DELETE | `/usagearea/{id}` | Delete usage area | Authenticated |

---

## 15. Stock

**Controller:** `StockController`
**Base Path:** `/stock`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/stock` | Get all stocks with filters and pagination | All roles |
| POST | `/stock/export` | Export stock data | All roles |
| POST | `/stock/current` | Get current stock for a product-warehouse combination | All roles |
| GET | `/stock/current` | Get stock quantity (GET variant) | All roles |
| GET | `/stock/current-stock` | Get current stock for indent pre-fill | All roles |
| GET | `/stock/getfilterdropdown` | Get dropdown values for stock filters | All roles |

---

## 16. Stock Summary (Global / Cross-tenant)

**Controller:** `StockSummaryController`
**Base Path:** `/stock-summary`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/stock-summary` | Get filtered stock summary across tenants | All roles |
| POST | `/stock-summary/export/excel` | Export stock summary as Excel | All roles |
| POST | `/stock-summary/import` | Import reorder levels from Excel | All roles |
| POST | `/stock-summary/sync` | Sync stock summary across all tenants | All roles |

**Notes:**
- `/import` for reorder levels has no role restriction — consider adding `@AllowOnly(ADMIN, PURCHASE_MANAGER)` if reorder level management should be restricted

---

## 17. Dead Stock

**Controller:** `DeadStockController`
**Base Path:** `/dead-stock`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/dead-stock` | Get filtered dead stock records | All roles |
| GET | `/dead-stock` | Get dead stock for a specific product | All roles |
| GET | `/dead-stock/{id}` | Get dead stock by ID | All roles |
| POST | `/dead-stock/sync` | Sync dead stock data across tenants | All roles |

---

## 18. All Inventory (Stock View)

**Controller:** `AllInventoryController`
**Base Path:** `/inventory`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/inventory` | Fetch all inventory with filters | All roles |
| POST | `/inventory/report` | Get inventory report by date range | All roles |
| POST | `/inventory/export/excel` | Export inventory to Excel | All roles |
| GET | `/inventory/refresh` | Trigger closing stock recalculation | All roles |

---

## 19. Dashboard (Inventory)

**Controller:** `DashboardController`
**Base Path:** `/dashboard`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/dashboard` | Get inventory dashboard summary | All roles |
| GET | `/dashboard/notification` | Get all system notifications | All roles |
| DELETE | `/dashboard/notification/{id}` | Delete a notification | All roles |

---

## 20. Dashboard v2 (Analytics)

**Controller:** `DashboardControllerv2`
**Base Path:** `/dashboard/v2`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/dashboard/v2/iostats` | Get historical inward/outward stats | All roles |
| GET | `/dashboard/v2/stockstats` | Get stock percentage breakdown | All roles |
| GET | `/dashboard/v2/iotrend` | Get inward-outward trend chart data | All roles |
| GET | `/dashboard/v2/inwardstats` | Get inward inventory stats (paginated) | All roles |
| GET | `/dashboard/v2/outwardstats` | Get outward inventory stats (paginated) | All roles |

---

## 21. Global Dashboard (Cross-tenant / Indent Analytics)

**Controller:** `GlobalDashboardController`
**Base Path:** `/global-dashboard`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/global-dashboard/charts` | Get indent status breakdown charts | All roles |
| GET | `/global-dashboard/products/stock` | Get stock info per product across tenants | All roles |
| GET | `/global-dashboard/indent/trend` | Get indent lifecycle trend (last 4 weeks) | All roles |
| GET | `/global-dashboard/po/trend` | Get PO lifecycle trend chart | All roles |
| GET | `/global-dashboard/stale-charts` | Get stale indent stacked chart | All roles |
| GET | `/global-dashboard/suppliers/lead-time/heatmap` | Get supplier lead time heatmap | All roles |

---

## 22. Inventory Transfer (Inter-warehouse)

**Controller:** `InventoryTransferController`
**Base Path:** `/inventory-transfer`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/inventory-transfer` | Fetch all transfers with pagination | All roles |
| GET | `/inventory-transfer/{id}` | Get transfer by ID | All roles |
| POST | `/inventory-transfer/create` | Create an inventory transfer | ⚠️ All roles (no auth gate) |
| GET | `/inventory-transfer/current-stock` | Get current stock for transfer pre-fill | All roles |
| POST | `/inventory-transfer/current-stock/bulk` | Get current stock for multiple products | All roles |

**Notes:**
- `/create` has no `@CheckAuthority` or `@AllowOnly` — consider adding role restriction (store-incharge / admin)

---

## 23. Lost / Damaged Inventory

**Controller:** `LostOrDamagedInventoryController`
**Base Path:** `/lostdamaged`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/lostdamaged` | Get all lost/damaged records with filters | All roles |
| GET | `/lostdamaged/{id}` | Get single lost/damaged entry by ID | All roles |
| POST | `/lostdamaged/create` | Create a lost/damaged inventory entry | Authenticated |
| PUT | `/lostdamaged/{id}` | Update lost/damaged entry | Authenticated |
| DELETE | `/lostdamaged/{id}` | Delete lost/damaged entry | Authenticated |

---

## 24. Inventory Month Pricing

**Controller:** `InventoryMonthPricingController`
**Base Path:** `/inventorypricing`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/inventorypricing` | Get all pricing mappings with pagination | All roles |
| GET | `/inventorypricing/dropdown` | Get dropdown values for pricing | All roles |
| POST | `/inventorypricing/create` | Create a pricing mapping | Authenticated |
| POST | `/inventorypricing/delete` | Delete a pricing mapping | Authenticated |

---

## 25. Missing Inventory Pricing

**Controller:** `MissingInventoryPricingController`
**Base Path:** `/missingpricing`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/missingpricing` | Get all items with missing pricing data | All roles |
| GET | `/missingpricing/dropdown` | Get dropdown values | All roles |

---

## 26. Inventory Month Usage

**Controller:** `InventoryMonthUsageInformationController`
**Base Path:** `/inventoryusage`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/inventoryusage` | Fetch monthly usage information | All roles |
| GET | `/inventoryusage/{locationId}` | Get available dates by location | All roles |
| GET | `/inventoryusage/dropdown` | Get location dropdown values | All roles |

---

## 27. Project Constants

**Controller:** `ProjectConstantsController`
**Base Path:** `/project-constants`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/project-constants` | Get all project constants (includes edit-day limits) | ⚠️ All roles (no auth gate) |
| GET | `/project-constants/{id}` | Get a single project constant by ID | ⚠️ All roles (no auth gate) |
| PUT | `/project-constants/{id}` | Update a project constant | ⚠️ All roles (no auth gate) |

**Notes:**
- All 3 endpoints have no auth annotation at all — the `PUT` to update constants (including edit-day limits) is completely open. Consider adding `@CheckAuthority` + `@AllowOnly(ADMIN)` on the PUT endpoint.

---

## 28. Dropdown Population

**Controller:** `PopulateDropdownController`
**Base Path:** `/dropdown`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/dropdown/{page}` | Get all dropdown values for a given page/module | All roles |

---

## 29. File Handling (Tenant-scoped)

**Controller:** `FileHandlingController`
**Base Path:** `/file`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/file/upload` | Upload a document file | Authenticated |
| GET | `/file/download/{fileId}` | Download a file by ID | All roles |

---

## 30. File Handling (Master Schema)

**Controller:** `MasterFileHandlingController`
**Base Path:** `/master-file`
**Tenant:** Master schema (`@UseDefaultTenant`)

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/master-file/upload` | Upload a document to the master schema | Authenticated |
| GET | `/master-file/download/{fileId}` | Download a file from master schema | All roles |

---

## 31. Drafts

**Controller:** `DraftController`
**Base Path:** `/drafts`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/drafts` | Save a draft (INDENT or PO type) | All roles |
| GET | `/drafts` | Get draft payload by type | All roles |
| GET | `/drafts/list` | List all drafts by type | All roles |
| GET | `/drafts/{id}` | Get draft by ID | All roles |
| DELETE | `/drafts/{id}` | Delete draft by ID | All roles |

---

## 32. BOQ (Bill of Quantities)

**Controller:** `BOQInventoryController`
**Base Path:** `/boq`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/boq/alltypes` | Get all valid building types | All roles |
| GET | `/boq/type/{id}` | Get all units under a building type | All roles |
| GET | `/boq/{id}` | Get a single BOQ entry | All roles |
| GET | `/boq/bytype/{id}` | Get BOQ entries by building type | All roles |
| GET | `/boq/byunit/{id}` | Get BOQ entries by location/unit | All roles |
| GET | `/boq/getproductlist/{id}` | Get product list for BOQ dropdown | All roles |
| POST | `/boq/create` | Create a new BOQ entry | Authenticated |
| PUT | `/boq/{id}` | Update a BOQ entry | Authenticated |
| DELETE | `/boq/{id}` | Delete a BOQ entry | Authenticated |

---

## 33. BOQ Upload

**Controller:** `BOQController`
**Base Path:** `/boqupload`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/boqupload/get_buildingunit_by_buildingtypeid/{id}` | Get building units by type ID | All roles |
| GET | `/boqupload/get_boq_report` | Get BOQ status report | All roles |
| GET | `/boqupload/getboqquantity` | Get BOQ quantity for outward pre-fill | All roles |
| POST | `/boqupload/boq_upload` | Upload and validate BOQ from Excel | Authenticated |
| POST | `/boqupload/get_boq_status_details` | Get detailed BOQ status | All roles |

---

## 34. BOQ Status

**Controller:** `BOQStatusController`
**Base Path:** `/boqstatus`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/boqstatus` | Fetch all BOQ records | All roles |
| GET | `/boqstatus/typelistwithcount` | Get type list with consumed unit count | All roles |
| POST | `/boqstatus/locationlist/{id}` | Get location-wise status for a type | All roles |
| POST | `/boqstatus/inventorylist/{id}` | Get inventory list for a location (paginated) | All roles |
| GET | `/boqstatus/inventorynames/{id}` | Get inventory names for a location | All roles |

---

## 35. Building Type

**Controller:** `BuildingTypeController`
**Base Path:** `/buildingtype`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/buildingtype` | Get filtered building types | All roles |
| GET | `/buildingtype/{id}` | Get single building type | All roles |
| GET | `/buildingtype/idandnames` | Get ID and name pairs for dropdowns | All roles |
| POST | `/buildingtype/create` | Create a new building type | Authenticated |
| PUT | `/buildingtype/{id}` | Update building type | Authenticated |
| DELETE | `/buildingtype/{id}` | Delete building type | Authenticated |

---

## 36. Contractor

**Controller:** `ContractorController`
**Base Path:** `/contractor`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/contractor` | Get all contractors with pagination | All roles |
| GET | `/contractor/names` | Get contractor names projection | All roles |
| GET | `/contractor/isused/{id}` | Check if contractor is referenced by any record | All roles |

**Notes:**
- Read-only controller — no create/update/delete endpoints (contractors likely managed elsewhere or via Contact module)

---

## 37. Supplier

**Controller:** `SupplierController`
**Base Path:** `/supplier`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/supplier` | Get all suppliers with pagination | All roles |
| GET | `/supplier/names` | Get supplier names projection | All roles |
| GET | `/supplier/isused/{id}` | Check if supplier is referenced by any record | All roles |

**Notes:**
- Read-only controller — suppliers managed via Contact module

---

## 38. Intersite Movement

**Controller:** `IntersiteMovementController`
**Base Path:** `/ism`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/ism/warehouses` | Get all warehouses available for movement | All roles |

---

## 39. Email (Automatic)

**Controller:** `AutomaticEmailController`
**Base Path:** `/email`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/email/stockupdate` | Trigger stock update notification email | ⚠️ No auth gate |
| GET | `/email/stockvalidation` | Trigger stock validation notification email | ⚠️ No auth gate |

**Notes:**
- These are likely cron/scheduler-triggered endpoints — no auth annotation. If externally accessible, should be secured or moved to an internal trigger mechanism.

---

## 40. Email (Manual)

**Controller:** `EmailController`
**Base Path:** `/email`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/email/stocknotification` | Send stock notification email | ⚠️ No auth gate |

---

## 41. SMS

**Controller:** `SMSController`
**Base Path:** `/sms`

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/sms/iostats` | Send IO stats via SMS | ⚠️ No auth gate |

---

---

## Security Gaps Summary

The following endpoints have weaker access control than expected. Review and harden if needed:

| Endpoint | Issue | Suggested Fix |
|---|---|---|
| `POST /purchase-order/short-close` | No `@AllowOnly` — any authenticated user can short-close a PO | Add `@AllowOnly(ADMIN, PURCHASE_MANAGER)` |
| `PATCH /indent/{id}/split` | No `@AllowOnly` — any authenticated user can split indent line items | Add `@AllowOnly(ADMIN, PURCHASE_MANAGER)` |
| `POST /inventory-transfer/create` | No `@CheckAuthority` or `@AllowOnly` | Add `@AllowOnly(ADMIN, STORE_INCHARGE)` |
| `POST /inward/opening-stock` | No auth gate — backdoor/migration endpoint | Secure or remove if migration is complete |
| `PUT /project-constants/{id}` | No auth gate — anyone can update edit-day limits | Add `@CheckAuthority` + `@AllowOnly(ADMIN)` |
| `GET /email/stockupdate` | No auth gate | Restrict to internal/scheduler only |
| `GET /email/stockvalidation` | No auth gate | Restrict to internal/scheduler only |
| `GET /email/stocknotification` | No auth gate | Restrict to internal/scheduler only |
| `GET /sms/iostats` | No auth gate | Restrict to internal/scheduler only |
| `POST /stock-summary/import` | No role gate — anyone can overwrite reorder levels | Add `@AllowOnly(ADMIN, PURCHASE_MANAGER)` |
| Warehouse/Location/UsageArea/BOQ/BuildingType writes | `@CheckAuthority` only, no role restriction | Consider `@AllowOnly(ADMIN, PURCHASE_MANAGER)` |
| `MachineryOnRentController` writes | `@CheckAuthority` only, no role restriction | Consider `@AllowOnly(ADMIN, PURCHASE_MANAGER)` |

---

## Role Permission Matrix — Write Operations

Quick reference: which roles can call write (create/update/delete) endpoints.

| Module | admin | purchase-manager | management | project-manager | store-incharge |
|---|:---:|:---:|:---:|:---:|:---:|
| Purchase Order (create/edit/delete) | ✅ | ✅ | ❌ | ❌ | ❌ |
| Indent (create/edit) | ✅ | ✅ | ❌ | ❌ | ✅ |
| Indent (cancel/delete) | ✅ | ✅ | ❌ | ✅ | ✅* |
| Indent (approve) | ✅ | ✅ | ❌ | ✅ | ❌ |
| Inward (create/edit/delete) | ✅ | ❌ | ❌ | ❌ | ✅ |
| Outward (create/edit/delete) | ✅ | ❌ | ❌ | ❌ | ✅ |
| Contact (create/edit/delete) | ✅ | ✅ | ❌ | ❌ | ✅ |
| Product / Category / Firm | ✅ | ✅ | ❌ | ❌ | ❌ |
| Machinery | ✅ | ✅ | ❌ | ❌ | ❌ |
| Historical Pricing (view) | ✅ | ✅ | ✅ | ❌ | ❌ |
| Price fields in PO/Print | ✅ | ✅ | ✅ | ❌ | ❌ |

*store-incharge can only cancel NEW indents; project-manager can cancel NEW or APPROVED.

---

## Frontend Permission Map

How backend roles map to frontend helper functions in `src/helper.js`:

| Helper Function | Returns true for | Used to hide/show |
|---|---|---|
| `isAdmin()` | admin | File delete in EditForm |
| `canEditInventoryModules()` | admin, purchase-manager | PO edit/delete buttons, Product/Category/Firm write buttons |
| `canEditContactModules()` | admin, purchase-manager, store-incharge | Contact add/edit/delete buttons |
| `canCreateIndent()` | admin, purchase-manager, store-incharge | Add Indent button |
| `canCreateInward()` | admin, store-incharge | Inward/Outward edit/delete/add-reject buttons |
| `canEditIndentRecord(status)` | See table below | Edit button in Indent detail |
| `canCancelIndentRecord(status)` | See table below | Cancel button in Indent detail |
| `canApproveIndentRecord(status)` | admin, purchase-manager, project-manager (NEW only) | Approve button |
| `canManagerRejectIndentRecord(status)` | admin, purchase-manager, project-manager (NEW only) | Reject button |
| `canResubmitIndentRecord(status)` | admin, purchase-manager, store-incharge (REJECTED only) | Resubmit button |
| `canViewMoneyFields()` | all except project-manager, store-incharge | Price columns in tables and prints |
| `getRoleEditConstraintDays()` | Returns adminDays / managerDays / generalDays | Date edit gate on all write forms |

**Indent edit/cancel rules (status-dependent):**

| Status | Can Edit | Can Cancel |
|---|---|---|
| NEW | admin, purchase-manager, store-incharge | admin, purchase-manager, project-manager, store-incharge |
| APPROVED | admin, purchase-manager | admin, purchase-manager, project-manager |
| REJECTED / CLOSED / CANCELLED | Nobody | Nobody |

---

*Document generated: 2026-04-03*
*Covers: sc-inventory-service backend + SC-Web-UI frontend*

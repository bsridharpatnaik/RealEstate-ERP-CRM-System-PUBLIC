# Quote Comparison Module Technical Design

## Document Purpose

This document defines the proposed technical design for a new Quote Comparison module in the inventory and procurement domain of the ERP.

The module is intended to bridge the current gap between:

`Approved Indent -> Vendor Quotation Comparison -> Finalized Vendor Selection -> Purchase Order`

This design is aligned to the current ERP structure where:

- indents are managed in the inventory procurement flow
- purchase orders already exist as a downstream procurement document
- quote comparison must support both commercial and technical evaluation
- quote comparison should remain generic enough for multiple material categories

## Design Goals

- introduce a sourcing layer between indent and purchase order
- support multiple supplier quotations for the same demand
- support evaluation beyond lowest price
- support line-level or full-request vendor finalization
- preserve auditability and traceability into purchase order
- fit the existing React + Spring Boot + JPA + multi-tenant architecture

## Functional Positioning in Current ERP

The current procurement chain already supports:

- indent creation and approval
- consolidation of open indent lines by category
- purchase order creation from selected indent lines
- purchase order update, status tracking, print, and export

The proposed Quote Comparison module should sit between approved indent sourcing demand and purchase order creation.

Recommended future procurement flow:

`Indent -> Quote Comparison -> Finalized Quote -> PO -> Inward -> Stock`

## Tenancy and Schema Design

### Recommended schema placement

Quote comparison should be stored in the **master schema**.

### Reason

In the current system:

- `IndentInventory` is in master schema
- `PurchaseOrder` is in master schema
- quote comparison logically belongs to the same procurement chain

If quote comparison is kept in project schema while indents and POs remain in master schema, cross-project sourcing and PO traceability become harder and inconsistent.

### Design decision

Recommended entities to live in master schema:

- QuoteComparisonHeader
- QuoteComparisonLine
- SupplierQuoteHeader
- SupplierQuoteLine
- QuoteAttributeDefinition
- QuoteAttributeValue
- QuoteEvaluation
- QuoteFinalization
- QuoteToPoRef

Attachments may continue to use the existing file handling pattern already used by inventory procurement flows.

## Proposed Navigation and Information Architecture

## Side Menu Placement

Add a new menu item under Inventory Management:

- Stock
- All Inventory
- Indent
- **Quote Comparison**
- Inward Inventory
- Outward Inventory
- Purchase Order

## Proposed Routes

- `/quoteComparison`
  Quote comparison list page

- `/quoteComparison/create`
  Create comparison from approved indent lines

- `/quoteComparison/:id`
  Quote comparison detail and evaluation workspace

- `/quoteComparison/:id/finalize`
  Finalization screen or modal workflow

- `/quoteComparison/:id/convert-to-po`
  PO conversion review screen

- `/quoteComparison/reports`
  Operational and management reports

## Page Inventory

| Page | Purpose | Primary users |
|---|---|---|
| Quote Comparison List | Search, filter, monitor all comparisons | Purchase Manager, Purchase Executive, Admin |
| Create Quote Comparison | Build a new comparison from approved indent lines | Purchase Executive, Purchase Manager |
| Quote Comparison Details | Core workspace for supplier quote entry and comparison | Purchase Executive, Purchase Manager |
| Supplier Quote Entry | Capture or revise one supplier response | Purchase Executive |
| Comparison Evaluation | Compare commercial and technical values side by side | Purchase Manager, Management |
| Finalization | Select supplier or split award | Purchase Manager, Management |
| Convert to PO | Review selected values before PO creation | Purchase Manager |
| Reports | Rate analysis and sourcing traceability | Management, Purchase Manager |

## Detailed Page Design

## 1. Quote Comparison List Page

### Purpose

Show all quote comparison records with status, project, indent, suppliers involved, and finalization progress.

### Layout

- Header with page title and create button
- Top filter bar
- Summary counters
- Main listing table
- Quick actions per row

### Filters

- project
- indent number
- quote comparison number
- status
- date range
- supplier
- created by
- finalized supplier

### Grid columns

- comparison number
- indent reference
- project
- created date
- status
- no. of suppliers quoted
- no. of lines
- finalized supplier or mixed award
- linked PO count
- actions

### Actions

- open
- edit
- add supplier quote
- finalize
- convert to PO
- cancel
- export

## 2. Create Quote Comparison Page

### Purpose

Create a sourcing request from approved indent demand.

### Layout

- Step 1: select approved indent or consolidated indent lines
- Step 2: review selected demand lines
- Step 3: create comparison header

### Main sections

- comparison basic information
- project selection if required
- related indent references
- sourcing type
- overall notes
- line item review

### Line item fields

- indent no
- line item code
- product
- quantity
- UOM
- required date
- specification from indent
- category
- current line status

### Key behavior

- allow full-indent selection
- allow selected line-item selection
- allow grouped/consolidated sourcing by category
- prevent duplicate open comparison for the same indent line unless explicitly overridden

## 3. Quote Comparison Detail Page

### Purpose

Serve as the main working screen for quote capture, comparison, review, and decision making.

### Layout

- Header summary card
- Comparison status and stage banner
- Left panel: demand lines
- Right panel: supplier quote tabs or supplier columns
- Bottom section: notes, attachments, approval history, linked PO references

### Header summary

- comparison number
- linked indent(s)
- project
- status
- created by
- created on
- total line count
- quoted supplier count
- finalized supplier count

### Tabs or sections

- Overview
- Supplier Quotes
- Comparison Matrix
- Evaluation
- Finalization
- Audit Trail

## 4. Supplier Quote Entry Screen

### Purpose

Capture one supplier’s quotation response.

### Recommended form sections

- supplier information
- quote commercial header
- quote line response table
- attachments and notes

### Header-level fields

- supplier
- quotation reference no
- quotation date
- validity date
- payment terms
- freight terms
- delivery lead time
- header notes

### Line-level fields

- quoted quantity
- quoted unit rate
- discount percent
- GST or tax
- landed cost
- expected delivery date
- line remarks
- compliance indicator

### Technical fields

These fields should be dynamic and driven by category template.

Examples:

- brand
- make
- grade
- density
- thickness
- diameter
- size
- specification text
- warranty
- sample approved

## 5. Comparison Matrix Screen

### Purpose

Show all suppliers side by side for each demand line.

### Recommended layout

- one row per demand line
- one supplier column block per vendor
- sticky left columns for item identity
- scrollable supplier quote columns

### Fixed columns

- indent no
- line item code
- product
- required quantity
- UOM
- indent specification

### Per-supplier comparison columns

- quoted rate
- landed cost
- lead time
- brand
- grade or density
- compliance
- remarks
- select indicator

### Highlighting rules

- highlight lowest commercial value
- visually flag missing quotes
- visually flag technical deviation
- visually flag expired quote validity
- visually flag partial quantity quote

## 6. Evaluation and Finalization Screen

### Purpose

Allow the business to select one quote per line or one supplier for all lines.

### Layout

- comparison summary
- evaluation criteria section
- line-wise decision grid
- final decision remarks section

### Evaluation modes

- direct manual selection
- weighted score selection

### Weighted scoring criteria examples

- price
- delivery lead time
- specification compliance
- brand preference
- warranty
- supplier performance

### Finalization outputs

- selected supplier
- selected line items
- final rate
- final taxes/charges
- final notes
- decision remarks
- reason for non-lowest selection

## 7. Convert to PO Screen

### Purpose

Create PO from finalized quote outcome.

### Layout

- selected supplier summary
- selected lines preview
- commercial carry-forward preview
- PO header entry section
- create PO action

### Carry-forward mapping

Recommended mapping into existing PO structure:

- supplier -> `supplierId`
- notes -> `notes`
- selected technical values -> line-level brand, grade, specification
- finalized rate -> `rate`
- discount -> `discountPercent`
- GST -> `gstPercent`
- line total -> `totalAmount`
- indent references -> existing `indentRefs`

### Multi-supplier behavior

If the comparison has split supplier selection:

- create one PO per selected supplier
- group selected lines by supplier
- preserve quote comparison linkage to all generated POs

## Low-Fidelity Screen Designs

## A. Quote Comparison List

```text
+----------------------------------------------------------------------------------+
| Quote Comparison                                               [Create New]      |
+----------------------------------------------------------------------------------+
| Filters: Project | Status | Supplier | Date Range | Indent | Finalized Supplier |
+----------------------------------------------------------------------------------+
| Summary: Open | Under Evaluation | Finalized | Converted to PO                   |
+----------------------------------------------------------------------------------+
| No | Comparison No | Project | Indent | Status | Suppliers | Finalized | Actions |
|----|---------------|---------|--------|--------|-----------|-----------|---------|
| 01 | QC-00045      | Aster   | IND-11 | Eval   | 3         | Pending   | View    |
| 02 | QC-00046      | Cedar   | IND-22 | Final  | 4         | ABC Co    | PO      |
+----------------------------------------------------------------------------------+
```

## B. Quote Comparison Detail

```text
+--------------------------------------------------------------------------------------+
| QC-00045 | Project: Aster | Indent: IND-11 | Status: Under Evaluation               |
+--------------------------------------------------------------------------------------+
| Tabs: Overview | Supplier Quotes | Comparison Matrix | Evaluation | Audit            |
+--------------------------------------------------------------------------------------+
| Demand Lines                                | Supplier Comparison                     |
|---------------------------------------------|-----------------------------------------|
| Line 1: Cement OPC 53, Qty 500 bags         | Supplier A | Supplier B | Supplier C  |
| Line 2: TMT 12mm, Qty 4 MT                  | Rate       | Rate       | Rate        |
| Line 3: Pipe 4 inch, Qty 120 nos            | Brand      | Brand      | Brand       |
|                                             | Lead Time  | Lead Time  | Lead Time   |
+--------------------------------------------------------------------------------------+
| Notes | Attachments | Approval Remarks | Linked POs                                 |
+--------------------------------------------------------------------------------------+
```

## C. Finalization Screen

```text
+----------------------------------------------------------------------------------+
| Finalization                                                                    |
+----------------------------------------------------------------------------------+
| Evaluation Mode: ( ) Manual Selection   ( ) Weighted Score                      |
+----------------------------------------------------------------------------------+
| Item               | Lowest Quote | Recommended | Selected Supplier | Reason     |
|--------------------|-------------|-------------|-------------------|------------|
| Cement OPC 53      | Supplier B  | Supplier A  | Supplier A        | Better lead|
| TMT 12mm           | Supplier C  | Supplier C  | Supplier C        | Lowest rate|
+----------------------------------------------------------------------------------+
| Final remarks:                                                                  |
| [............................................................................]  |
|                                    [Save Draft] [Finalize]                      |
+----------------------------------------------------------------------------------+
```

## Frontend Technical Design

## Recommended React Module Structure

Suggested UI module path:

- `SC UI/src/Modules/QuoteComparison/index.js`
- `SC UI/src/Modules/QuoteComparison/list.js`
- `SC UI/src/Modules/QuoteComparison/filter.js`
- `SC UI/src/Modules/QuoteComparison/details.js`
- `SC UI/src/Modules/QuoteComparison/create.js`
- `SC UI/src/Modules/QuoteComparison/ComparisonMatrix.js`
- `SC UI/src/Modules/QuoteComparison/SupplierQuoteForm.js`
- `SC UI/src/Modules/QuoteComparison/FinalizeModal.js`
- `SC UI/src/Modules/QuoteComparison/ConvertToPO.js`
- `SC UI/src/Modules/QuoteComparison/style.scss`

## Shared UI Components to Reuse

- list and table framework used by Indent and PurchaseOrder
- filter component pattern
- details popup or side panel patterns
- upload and file attachment components
- confirmation modal pattern
- existing autocomplete/select components

## Frontend State Requirements

The detail page should maintain state for:

- comparison header
- demand line list
- supplier quote list
- selected comparison view
- evaluation criteria
- selected suppliers by line
- attachments
- audit trail
- PO conversion preview

## Backend Technical Design

## Proposed Controllers

- `QuoteComparisonController`
- `SupplierQuoteController`
- `QuoteComparisonEvaluationController`
- `QuoteComparisonReportController`

## Suggested API Endpoints

### Comparison header and line APIs

- `POST /api/inventory/quote-comparison/create`
- `POST /api/inventory/quote-comparison`
- `GET /api/inventory/quote-comparison/{id}`
- `PUT /api/inventory/quote-comparison/{id}`
- `PATCH /api/inventory/quote-comparison/{id}/cancel`
- `GET /api/inventory/quote-comparison/{id}/audit`

### Supplier quote APIs

- `POST /api/inventory/quote-comparison/{id}/supplier-quote`
- `PUT /api/inventory/quote-comparison/{id}/supplier-quote/{supplierQuoteId}`
- `GET /api/inventory/quote-comparison/{id}/supplier-quotes`
- `GET /api/inventory/quote-comparison/{id}/comparison-matrix`

### Evaluation and finalization APIs

- `POST /api/inventory/quote-comparison/{id}/evaluate`
- `POST /api/inventory/quote-comparison/{id}/finalize`
- `POST /api/inventory/quote-comparison/{id}/reopen`

### PO integration APIs

- `GET /api/inventory/quote-comparison/{id}/po-preview`
- `POST /api/inventory/quote-comparison/{id}/create-po`
- `GET /api/inventory/quote-comparison/{id}/linked-pos`

### Reports

- `POST /api/inventory/quote-comparison/report`
- `POST /api/inventory/quote-comparison/export/excel`

## API Design Notes

- APIs should follow existing inventory service pagination/filter patterns
- quote comparison data should be filterable via `FilterDataList`
- file attachments should reuse current file upload/download services
- PO conversion should reuse the existing PO builder model as much as possible

## Domain Model Proposal

## 1. QuoteComparisonHeader

Suggested fields:

- id
- comparisonNumber
- indentReferenceMode
- projectName
- tenantCode or tenant summary
- status
- sourcingType
- createdBy
- createdDate
- finalizedDate
- overallNotes

## 2. QuoteComparisonLine

Suggested fields:

- id
- quoteComparisonHeaderId
- indentNo
- indentLineItemCode
- productId
- productNameSnapshot
- quantityRequired
- uomSnapshot
- indentSpecificationSnapshot
- categorySnapshot
- requiredDate
- lineStatus

## 3. SupplierQuoteHeader

Suggested fields:

- id
- quoteComparisonHeaderId
- supplierId
- quotationRefNo
- quotationDate
- validityDate
- paymentTerms
- deliveryTerms
- freightTerms
- leadTimeDays
- headerNotes
- quoteRevisionNo
- status

## 4. SupplierQuoteLine

Suggested fields:

- id
- supplierQuoteHeaderId
- quoteComparisonLineId
- quotedQuantity
- quotedRate
- discountPercent
- taxPercent
- landedAmount
- expectedDeliveryDate
- lineRemarks
- complianceStatus

## 5. QuoteAttributeDefinition

Suggested fields:

- id
- categoryId or procurementType
- attributeCode
- attributeLabel
- attributeType
- isMandatory
- displayOrder
- isCommercial
- isTechnical
- isDelivery

## 6. QuoteAttributeValue

Suggested fields:

- id
- supplierQuoteLineId
- attributeDefinitionId
- attributeValueText
- attributeValueNumber
- attributeValueBoolean

## 7. QuoteFinalization

Suggested fields:

- id
- quoteComparisonHeaderId
- quoteComparisonLineId
- selectedSupplierQuoteLineId
- selectedSupplierId
- finalRate
- finalTaxPercent
- finalLandedAmount
- finalRemarks
- selectionReason
- approvedBy
- approvedDate

## 8. QuoteToPoRef

Suggested fields:

- id
- quoteComparisonHeaderId
- quoteComparisonLineId
- supplierQuoteLineId
- purchaseOrderId
- purchaseOrderLineId

## Data Relationship Summary

- one comparison header -> many comparison lines
- one comparison header -> many supplier quote headers
- one supplier quote header -> many supplier quote lines
- one comparison line -> many supplier quote lines
- one comparison line -> one finalization record in normal case
- one comparison header -> one or many POs depending on split supplier selection

## Integration with Existing Purchase Order Model

The current PO model already supports:

- supplier header information
- line-level rate, discount, GST
- line-level brand, grade, diameter, specification
- indent line references through `PurchaseOrderIndentRef`

This is useful because quote finalization can map cleanly into the existing PO design.

### Proposed conversion mapping

From final selected supplier quote line to PO line:

- `productId` -> `CreatePoLineRequest.productId`
- `quantityRequired` or approved quantity -> `quantity`
- selected brand -> `brand`
- selected grade -> `grade`
- selected specification -> `specification`
- selected rate -> `rate`
- selected discount -> `discountPercent`
- selected tax -> `gstPercent`
- computed net rate -> `netRate`
- computed amount -> `totalAmount`
- indent references -> `indentRefs`

### PO header mapping

- selected supplier -> `supplierId`
- commercial notes -> `notes`
- project or sourcing summary -> `subject` or `projectName`

## Validation Rules

## Header validations

- comparison must reference at least one approved indent line
- duplicate active comparison for the same indent line should be blocked or warned
- comparison cannot be finalized without at least one valid supplier quote

## Supplier quote validations

- supplier must be selected
- quote must have at least one responded line
- validity date cannot be before quotation date
- commercial values must be non-negative
- quoted quantity cannot exceed configured business limits

## Line validations

- partial quantity should be explicitly marked
- mandatory technical attributes must be filled before evaluation
- expired quotes should not be finalized without override

## Finalization validations

- one line must have at most one active selected supplier outcome
- non-lowest selection requires reason
- technically non-compliant line should require explicit override
- finalized lines cannot be edited unless reopened

## Status Model

Suggested status lifecycle:

- Draft
- Open for Quotes
- Quotes Received
- Under Evaluation
- Partially Finalized
- Finalized
- Converted to PO
- Cancelled
- Closed

## Permission Model

Recommended access by role:

| Role | List/View | Create/Edit | Add Supplier Quotes | Finalize | Convert to PO |
|---|---|---|---|---|---|
| admin | Yes | Yes | Yes | Yes | Yes |
| purchase-manager | Yes | Yes | Yes | Yes | Yes |
| project-manager | Yes | Limited | No | Recommend only | No |
| store-incharge | View only | No | No | No | No |
| management | View only | No | No | Optional approval | No |

## Reporting Design

Recommended reports:

- quote comparison register
- supplier participation report
- finalization turnaround report
- vendor win/loss report
- lowest vs selected quote variance report
- quote-to-PO conversion report
- category-wise quote comparison analytics

## Notifications

Recommended notifications:

- comparison created
- supplier quote pending
- all supplier quotes received
- validity expiry approaching
- finalization completed
- PO created from comparison

## Non-Functional Considerations

### Performance

- comparison matrix should be optimized for many suppliers and many lines
- header list should use pagination like existing list pages
- heavy comparison datasets should be server-prepared rather than fully computed in the browser

### Auditability

- all supplier revisions should be preserved
- finalization action should capture actor and timestamp
- PO creation should preserve source comparison traceability

### Attachment handling

- supplier quote attachments should reuse current inventory file storage flow
- downloaded attachments should preserve source supplier quote association

### Export

- list view and comparison matrix export should be supported
- exported sheet should show selected supplier vs all quoted suppliers

## MVP Recommendation

## Phase 1

- create comparison from indent
- capture multiple supplier quotes
- compare commercial and basic technical values
- finalize line-wise or full comparison
- create PO from finalized quote

## Phase 2

- configurable attribute templates by category
- weighted scoring model
- split-award automation
- quote revision comparison history
- reports and management dashboards

## Phase 3

- approval workflow
- supplier portal
- automated RFQ communication
- vendor performance score integration

## Open Design Questions

- should one indent line be allowed in multiple active comparisons
- should finalization support split quantity across multiple suppliers for the same line
- should comparison operate only at product level or also at grouped commodity level
- should category attribute templates be managed by admin or by purchase master users
- should technical approval be separate from commercial approval for certain categories

## Recommended Final Technical Direction

The module should be implemented as a **generic procurement comparison engine** rather than a simple quote-entry form.

That means the final design should:

- remain tightly linked to indent and PO
- support multi-supplier line-by-line comparison
- allow category-driven technical fields
- preserve full sourcing history
- convert cleanly into the existing PO structure

This direction fits both the current ERP architecture and the real procurement behavior where vendor selection often depends on brand, quality, delivery, and compliance in addition to rate.

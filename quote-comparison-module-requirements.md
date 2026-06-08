# Quote Comparison Module Requirements

## Purpose

Define a generic quote comparison module for procurement teams to collect, compare, evaluate, and finalize supplier quotations against an approved indent.

This module should fit into the current procurement flow of the ERP:

`Indent -> Quote Comparison -> Finalized Quote -> Purchase Order -> Inward`

The document is intentionally generic so it can work across multiple categories of materials, vendors, and projects without assuming one specific procurement style.

## Why This Module Is Needed

The current ERP already supports indents, purchase orders, inward, outward, stock, and supplier-related workflows. A quote comparison module fills the gap between approved demand and purchase order creation.

The module should help the business:

- collect multiple vendor quotations against the same indent
- compare supplier offers in one place
- capture both price and non-price differentiators
- record the reason for vendor selection
- finalize a quote and carry the selected values into the purchase order
- maintain procurement transparency and auditability

## Benchmark Against Common ERP Patterns

Across common ERP procurement implementations, quote comparison usually sits between approved demand and purchase order creation.

The recurring patterns seen across other ERP-style systems are:

- one sourcing request linked to one demand or requisition
- multiple supplier quotations against the same requirement
- side-by-side line-level comparison
- commercial comparison beyond base price alone
- supplier selection with justification
- conversion of selected supplier response into purchase order

This document follows those common patterns, but keeps the model flexible enough for construction and materials procurement where technical factors may materially affect quote selection.

## High-Level Business Scope

The module should support:

- creation of a quote comparison request from one indent or selected indent line items
- collection of quotations from multiple suppliers for the same requirement
- side-by-side comparison of supplier responses
- evaluation based on both commercial and technical attributes
- selection of one supplier quote per line item or for the full request
- conversion of selected quote values into purchase order data

## Target Users

- Purchase Manager
- Purchase Executive
- Admin
- Project Manager or recommending authority
- Management or approver, where commercial approval is required

## Recommended Business Flow

### 1. Start from indent

Once an indent is approved, the user should be able to create a quote comparison record from:

- the full indent
- selected indent line items
- one or more consolidated indent lines for the same item/category

The quote comparison record becomes the sourcing container for that demand.

### 2. Invite or register multiple supplier quotes

For the same quote comparison record, the user should be able to add multiple supplier quotations.

Each supplier quote should allow:

- supplier selection
- supplier quotation reference number
- quotation date
- quotation validity date
- delivery lead time
- payment terms
- tax and charge details
- freight or logistics details
- warranty or replacement commitments
- commercial notes
- attachments such as quotation PDFs, scans, or emails

### 3. Enter quote line details

For each supplier quote, the user should be able to respond line by line against the indent items.

Each quote line should support:

- quoted unit rate
- quoted quantity
- UOM
- discount
- tax applicability
- landed amount or other additional charges where relevant
- expected delivery date
- remarks

### 4. Capture technical and quality attributes

The comparison should not be limited to price alone.

The system should support configurable quote attributes at line level such as:

- brand
- make or manufacturer
- model
- grade
- density
- thickness
- size or dimension
- specification compliance
- country of origin
- warranty period
- sample approval status

These attributes should be configurable because they vary by material category. For example, cement, steel, pipes, electrical items, finishing materials, and chemicals may all need different comparison criteria.

### 5. Compare supplier quotes

The module should provide a side-by-side comparison view showing all supplier responses for the same indent item.

Comparison should support:

- line-wise supplier comparison
- full-quote comparison
- sorting by price, delivery, validity, or supplier
- highlighting lowest price
- identifying missing or partial supplier responses
- visibility of technical/commercial deviations

### 6. Evaluate and finalize

The business should be able to finalize:

- one supplier for the full quote comparison
- different suppliers for different line items

The system should support both approaches because some procurements are awarded to a single vendor, while others are split line by line based on best fit.

The finalization action should capture:

- selected supplier
- selected line items
- final approved rate
- final commercial notes
- justification for selection
- approval remarks if approval workflow exists

### 7. Convert finalized quote into purchase order

Once finalized, the selected commercial values should flow into purchase order creation.

The purchase order should inherit, at minimum:

- supplier
- indent reference
- selected line items
- approved quantities
- approved rate
- taxes and charges
- delivery terms
- payment terms
- notes and remarks
- any selected technical specification references

The quote comparison record should remain linked to the purchase order for audit and traceability.

## Functional Requirements

## 1. Quote Comparison Header

The module should maintain a quote comparison header with:

- quote comparison number
- project or tenant
- related indent number
- comparison date
- status
- created by
- sourcing type
- overall notes

Possible statuses:

- Draft
- Open for Quotes
- Quotes Received
- Under Evaluation
- Partially Finalized
- Finalized
- Cancelled
- Closed

## 2. Quote Comparison Lines

Each header should contain one or more comparison lines derived from indent items.

Each line should support:

- item reference
- item description
- indent quantity
- required date
- current stock reference if needed
- specification from indent
- technical attribute template

## 3. Supplier Quote Management

The system should support multiple supplier quotes per comparison record.

Each supplier quote should allow:

- full quote capture
- partial quote capture
- line-level response
- attachments
- notes
- revisions or re-submission

The system should preserve quote history when suppliers revise their offers.

## 4. Comparison Attributes

The module should support three kinds of comparison values:

- **Commercial values**
  Price, discount, taxes, freight, payment terms, credit period, minimum order conditions.

- **Delivery values**
  Lead time, delivery schedule, delivery location commitment, partial supply capability.

- **Technical values**
  Brand, make, density, grade, compliance, sample approval, warranty, or any project-specific quality factor.

These should be configurable by item category or procurement type.

## 5. Evaluation Model

The system should support two evaluation methods:

- **Direct comparison**
  Simple side-by-side comparison where users manually select the best vendor.

- **Weighted evaluation**
  Configurable scoring model where criteria such as price, delivery, brand, compliance, and warranty are assigned weights.

This is important because the lowest price is not always the best quote.

## 6. Supplier Selection Rules

The module should support:

- single supplier selection for all lines
- split award across suppliers
- no-award or re-quote decision
- manual override of lowest price with mandatory justification

## 7. Purchase Order Integration

The purchase order module should be able to create a PO from a finalized quote comparison.

The integration should support:

- one PO from one finalized supplier
- multiple POs when different suppliers are selected line by line
- quote-to-PO traceability
- prevention of duplicate PO creation for the same finalized lines unless explicitly allowed

## 8. Audit and Traceability

The module should preserve:

- all received supplier quotes
- quote revisions
- comparison snapshots
- selection remarks
- approval remarks
- linked purchase orders

This is necessary for internal control, transparency, and later vendor performance review.

## 9. Search, Filters, and Reporting

Users should be able to filter quote comparisons by:

- project
- indent
- supplier
- status
- date range
- item category
- finalized supplier

Reports should support:

- quote comparison summary
- vendor participation report
- vendor win/loss report
- rate comparison history
- lowest vs selected vendor variance
- indent-to-quote-to-PO traceability report

## 10. Notifications and Workflow

The module should support optional notifications for:

- quote request created
- quote pending from supplier
- quote validity nearing expiry
- comparison ready for evaluation
- comparison finalized
- PO created from quote

If approval workflow is introduced, approval should be configurable by role, amount, or project.

## Key Business Rules

- Quote comparison must start only from approved indent demand or other approved sourcing demand.
- A supplier may quote for all lines or only selected lines.
- Different suppliers may be selected for different items in the same comparison.
- Finalized quote values should become the source for PO commercial values.
- If a user selects a non-lowest quote, justification should be captured.
- Technical non-compliance should be visible before finalization.
- Quote revisions should not overwrite historical supplier submissions.
- Finalized lines should be locked from accidental modification unless reopened by authorized users.

## Suggested Data Structure

At a high level, the module will likely need the following business entities:

- Quote Comparison Header
- Quote Comparison Line
- Supplier Quote Header
- Supplier Quote Line
- Quote Attribute Definition
- Quote Attribute Value
- Evaluation Criteria Set
- Evaluation Score
- Final Selection Record
- Quote to Purchase Order Link

This structure is suggested so that the system can support both simple and advanced comparison scenarios.

## Comparison Criteria Recommendations

At minimum, comparison should support the following fields:

| Category | Typical fields |
|---|---|
| Commercial | Unit rate, discount, tax, freight, total landed cost, payment terms |
| Delivery | Lead time, delivery schedule, partial delivery support, destination terms |
| Technical | Brand, make, grade, density, dimensions, compliance notes, warranty |
| Supplier context | Past performance, preferred supplier flag, supplier rating, approval status |
| Decision support | Lowest quote indicator, selected quote indicator, deviation remarks |

## Generic Screen Requirements

The module should eventually provide these screens:

- Quote comparison list
- Quote comparison details
- Supplier quote entry screen
- Side-by-side comparison screen
- Evaluation and scoring screen
- Finalization screen
- Quote-to-PO conversion screen
- Reports and analytics screen

## Out of Scope for Initial Version

The first version does not have to include:

- supplier portal submission
- automated email RFQ workflow
- automated vendor scoring from historical performance
- reverse auction
- AI-based quote recommendation
- complex commercial bid opening workflow

These can be added in later phases.

## Phase-Wise Recommendation

### Phase 1

- create quote comparison from indent
- record multiple supplier quotes
- compare quotes line by line
- capture price, delivery, and notes
- finalize supplier selection
- create PO from finalized quote

### Phase 2

- configurable technical attributes by category
- weighted scoring model
- split award across suppliers
- quote revision handling
- analytics and reports

### Phase 3

- approval workflow
- supplier portal or email-based quote intake
- vendor performance integration
- advanced commercial analysis and negotiation support

## Recommended Design Principle

The module should be designed as a **generic sourcing layer** between indent and purchase order, not as a one-off price entry screen.

That means:

- the module must support multiple suppliers
- the module must support multiple decision criteria
- the module must support category-specific technical attributes
- the module must maintain traceability into purchase order

This will allow the same module to work for both simple purchases and technically sensitive materials where brand, density, grade, or specification matter as much as price.

## Summary

The quote comparison module should enable the business to:

- source multiple quotations against an indent
- compare supplier responses fairly and transparently
- evaluate both commercial and technical factors
- finalize the best supplier or mix of suppliers
- create purchase orders from the approved quote outcome

In short, it should become the procurement decision layer between demand generation and purchase execution.

# Real Estate ERP/CRM System

Multi-tenant ERP/CRM platform for real-estate and construction operations, covering inventory, procurement, BOQ-driven planning, and customer lifecycle management.

## Demo

📺 **4-minute product walkthrough:** https://drive.google.com/file/d/1HTJA1ND96BwAfcAOyxjQgoYH3P8bWRL8/view?usp=drive_link

## What This Platform Covers

- **Inventory operations** for material planning, stock movement, inward/outward handling, and project supply execution
- **Procurement workflows** for indents, purchase orders, supplier coordination, and material receipt
- **BOQ-driven control** for planning, tracking, and cross-project visibility
- **CRM and post-sales workflows** for leads, pipeline execution, customer engagement, and payment tracking
- **Shared platform services** for authentication, tenants, users, and role-based access
- **AI assistant and smart suggestions** that answer questions from live data and flag what needs attention while approving indents and raising POs

## Core Modules

### Inventory Service
The inventory module is the operational core for construction and project material management.

**Major capabilities**
- Inventory masters and reference management
- BOQ management and BOQ reporting
- Indent and purchase order workflows
- Inward, outward, and stock movement operations
- Live stock visibility and stock control
- Stock summary, dead stock, and reorder monitoring
- Inventory transfers across projects or tenants
- Pricing, usage, and operational reporting
- Global dashboards and cross-project inventory visibility

### Common Service
The common service provides the shared platform layer used by the rest of the system.

**Major capabilities**
- Authentication and token-based access
- User and role management
- Tenant and access mapping
- Shared notification support
- API logging and common operational services

### CRM Service
The CRM module manages the lead-to-customer lifecycle and post-sales engagement.

**Major capabilities**
- Lead and prospect management
- Pipeline and planner workflows
- Lead activity and follow-up management
- Broker, source, and sentiment management
- Customer conversion and post-sales workflows
- Deal structure, payment schedule, and payment tracking
- CRM dashboards, funnel views, and sales analytics

## Notable Features Delivered

The platform has grown well beyond its initial inventory scope. Major capabilities shipped over time:

**Inventory & stock**
- Batch tracking with **FIFO / FEFO** (expiry-aware) consumption, per-batch quantity ledgers, and expiry monitoring
- FIFO override detection (order **and** quantity) with a dedicated override report
- Untracked-stock enforcement — tracked products must be split into named batches before outward
- Inventory transfers across projects / tenants
- Lost/damaged handling, write-offs, and stock reconciliation
- Live stock, stock summary, dead-stock and reorder-level monitoring
- **MinIO** object storage for document/file attachments (migrated off MySQL BLOBs)

**Procurement**
- Indent → Purchase Order → Inward workflow, end to end
- Purchase Order lifecycle management with **iText PDF** generation
- PO spend analytics (configurable high-value thresholds and color bands)
- PO-vs-Inward reconciliation reporting
- **Quote Comparison / RFQ** — comparison matrix, RFQ PDF, and bidirectional PO integration
- Service Orders with line-level status tracking

**BOQ (Bill of Quantities)**
- BOQ upload with planned-vs-actual material tracking
- Unified BOQ report and end-to-end BOQ tracker
- BOQ indent report and "BOQ remaining" indicators on indent creation

**Reporting & operations**
- Global **activity log** (per-tenant, synced to a master global view)
- Reports module: Low Stock, Indent Fulfillment, PO Reconciliation, aging / dead / expired stock
- **Product merge** with cross-table foreign-key reassignment
- Global and cross-project dashboards

**Platform**
- **Phone-friendly layouts** across list, detail, report and form pages, including the multi-step PO wizard
- **Multi-tenant** schema routing (per-tenant schemas plus a shared master schema)
- JWT authentication via an API gateway, with role-based access
- Firebase-based push notifications
- Timezone-aware operations (Asia/Kolkata)

## AI Assistant & Smart Suggestions

**AI assistant (chat)**
- Admins and purchase managers ask questions in plain English ("Which POs are pending delivery?", "Cement stock across all sites?", "PO value by month for the last 6 months") and get answers, tables and charts from live ERP data
- Runs sandboxed with read-only data access; conversations can continue with follow-up questions
- **Per-user daily limits** (a default plus per-user overrides) managed from an admin **AI Usage** page, which also shows usage and cost per user and a full history of who asked what

**Smart suggestions (rule-based, instant, no AI cost)**
- **Indent approval** — every item is checked before approval: stock already at the project (with how long it has been sitting), unused stock at other projects that could be transferred, material already on order and still to arrive, similar indents raised recently, and BOQ overruns. Ends with the top suggested actions. No prices are shown to approvers.
- **Purchase order creation** — as rates are entered: last price paid (supplier, date), the 6-month price range and lowest price, how the entered rate compares with both, a note when the last price is stale, and the same item already on order for other projects
- **PO details** — received vs pending per line, short-close / cancellation, and the last real status change in plain English
- **Dashboard "Needs attention" banner** — indents waiting for approval, approved indents without a PO, POs with nothing received, and POs with no progress, each with the oldest example; role- and project-aware

## Product Highlights

- **Multi-tenant by design**  
  Supports tenant-aware and project-aware workflows across major modules.

- **Built for construction and real-estate operations**  
  Strong focus on BOQ, inventory control, procurement execution, and project material visibility.

- **Global and project-level views**  
  Supports both local operational execution and higher-level cross-project visibility.

- **ERP + CRM in one platform**  
  Combines backend operational control with customer-facing sales and post-sales workflows.

## Best Fit

This platform is best suited for organizations that need:

- project-wise inventory and procurement control
- BOQ-linked material planning
- multi-project or multi-tenant operational visibility
- integrated CRM and post-sales tracking alongside ERP workflows

## High-Level Positioning

This repository represents a **construction-focused ERP/CRM platform** with strong capabilities in:

- inventory and stock operations
- procurement and material flow
- BOQ management
- tenant-aware operational control
- CRM, sales pipeline, and post-sales processes

## Tech Stack

- **Backend:** Java, Spring Boot (microservices), Spring Security, JPA/Hibernate, Zuul API gateway
- **Database:** MySQL 8 (multi-tenant, schema-per-tenant + shared master schema)
- **Object storage:** MinIO
- **PDF:** iText
- **Notifications:** Firebase
- **Frontend:** React 16 (Create React App, `env-cmd`)
- **AI:** Anthropic Claude, via the Claude Code CLI, for the chat assistant

### Services & ports

| Service | Path | Default port |
|---|---|---|
| Common (gateway, auth, users, tenants) | `sc-common-service` | 8090 |
| Inventory (ERP core) | `sc-inventory-service` | 8091 |
| CRM | `sc-crm-service` | 8092 |
| Frontend | `SC UI` | 3000 |

### Implementation highlights

For developers exploring the code, some of the notable patterns and libraries used:

- **Netflix Zuul** API gateway — routing plus a custom logging filter (`LoggingZuulFilter`)
- **Spring Security + JWT** (`io.jsonwebtoken`) — token issuance and a request filter for stateless auth
- **JPA Criteria API + Specifications** — dynamic, type-safe query building (`SpecificationsBuilder`, `UserSpecifications`) powering the filterable reports
- **Hibernate Envers** — entity auditing / revision history
- **Multi-tenant routing** — per-tenant schema resolution with a shared master schema
- **iText** — server-side PDF generation for purchase orders and service orders
- **MinIO** SDK — object storage for file/document attachments
- **Firebase Cloud Messaging** — push notifications
- **Spring `@Async` + `@Scheduled`** — asynchronous API/activity logging and scheduled cleanup jobs
- **Deterministic smart-suggestion writer** (`SmartSuggestionWriter`) — turns SQL facts into natural sentences with stable wording per record and Indian number formatting; unit-tested with JUnit and Jest

## Configuration Before Running

> **Note:** For security, environment-specific `application-*.properties` files and hardcoded
> secrets are **not committed** to this repository. You must create/populate the files below with
> your own values before the project will run.

### 1. Backend — per service (`<service>/src/main/resources/`)

**`application.properties` (base, each service)**
- `spring.profiles.active` — selects which `application-<profile>.properties` to load
- `jwt.secret` — **must be identical across all three services** (gateway signs, services validate)

**`application-<profile>.properties` (the active profile — DB & environment)**
- `spring.datasource.username`, `spring.datasource.password`
- `db.host` — MySQL host/IP
- `schemas.list` — comma-separated tenant schema names for multi-tenant routing (plus the master schema)
- `common.serverurl` — URL of the common/gateway service (used by inventory)
- `stock.notification.emailids`, `sendsms` — notification settings

**MinIO (inventory service properties)**
- `minio.url`, `minio.accessKey`, `minio.secretKey`

**Email — `EmailConstants.java` (hardcoded, one per service)**
- `sc-common-service/.../common/Configuration/EmailConstants.java`
- `sc-inventory-service/.../application/config/EmailConstants.java`
- `sc-crm-service/.../crm/Config/EmailConstants.java`
- Set `mailHost`, `mailPort`, `mailUsername`, `mailPassword`

**Firebase (common service)**
- Place your Firebase Admin SDK JSON in `sc-common-service/src/main/resources/`
- Point `app.firebase-config=<your-file>.json` at it (in `application.properties`)

**Gateway basic-auth (common service `application.properties`)**
- `spring.security.user.name`, `spring.security.user.password`

### 2. Frontend — `SC UI/.env`

- `REACT_APP_BASE_URL` — API gateway base path
- `REACT_APP_ISCRM`, `REACT_APP_ISPOSTSALES` — feature flags
- Branding: `REACT_APP_TITLE`, `REACT_APP_DESC`, `REACT_APP_NAME`, `REACT_APP_FAVICON`, `REACT_APP_LOGIN_LOGO`, `REACT_APP_COOKIES_MAIN_KEY`

### 3. Database

- MySQL 8 with a `masterschema` plus one schema per tenant (names must match `schemas.list`).
- Hibernate runs with per-tenant `hbm2ddl.auto=update`, so **additive** schema changes apply on
  startup; non-additive changes (drops/renames, backfills) are applied manually.
- **Database views are required.** After the schemas exist, run the `CreateViews.sql` script for
  each service against the relevant schema(s) — the application depends on these views at runtime:
  - `sc-inventory-service/src/main/resources/SQLs/CreateViews.sql`
  - `sc-crm-service/src/main/resources/DB Scripts/CreateViews.sql`
  - `sc-common-service/src/main/resources/CreateViews.sql`

### 4. AI assistant (optional)

- `ai.assistant.command` (inventory service) — external command that answers a question: receives the question on stdin and a session id (or `-`) as its argument, and prints the Claude CLI JSON result. Default: `sudo -n /usr/local/bin/erp-ai-ask`.
- `ai.assistant.timeout-seconds` — default 180.
- The assistant's runtime (sandbox wrapper, prompt and data skills) is environment-specific and **not included** in this repository. Without it the chat returns an error; everything else, including smart suggestions, works without AI.
- Daily limits are set from the **AI Usage** admin page (built-in default: 5 questions per user per day).

### Run order

1. Start MySQL and create the master + tenant schemas.
2. Run each service's `CreateViews.sql` against the appropriate schema(s) (see Database section above).
3. Start `sc-common-service` (gateway), then `sc-inventory-service` and `sc-crm-service`.
4. In `SC UI`: `npm install` then `npm start` (serves on port 3000).

## License

Released under the [MIT License](LICENSE) — free to use, modify, and distribute.

## Contact

Sridhar Patnaik — bsridharpatnaik@gmail.com

# CLAUDE.md — AI Agent Context

Context for AI coding agents (Claude Code, etc.) working in this repo. Humans:
see [README.md](README.md) first.

## What this is

Multi-tenant Real Estate ERP/CRM. Three Spring Boot services + a React UI, all
deployed as WARs to Tomcat on a VPS.

| Component | Path | Stack |
|-----------|------|-------|
| Inventory service | `sc-inventory-service/` | Spring Boot 2.2.2, Java 8, WAR, MySQL (multi-tenant), iText (PDF), Apache POI (Excel), MinIO (files), Log4j2 |
| CRM service | `sc-crm-service/` | Spring Boot 2.2.2, Java 8, WAR, WebFlux, Lombok, ModelMapper |
| Common service | `sc-common-service/` | Spring Boot, Java 8, WAR — auth, users, roles, tenants, notifications |
| Frontend | `SC UI/` | React (Create React App), Material-UI, axios, amCharts, Syncfusion |

Backend base package: `com.ec.application` (inventory). Feature code is split
into `controller`, `service`, `repository`, `model`, plus cross-cutting
packages (`multitenant`, `IDGenerator`, `aspects`, `Filters`, `mapper`).

## Build / run

```bash
# Backend (per service)
cd sc-inventory-service && mvn clean package        # -> target/*.war
mvn spring-boot:run                                 # local run

# Frontend
cd "SC UI" && npm install && npm start              # dev server
npm run build                                       # prod bundle (also build-eg / build-pc per env)

# Full deploy to VPS Tomcat
./deploy.sh                                          # builds all + ships WARs
```

## Architecture rules — read before editing

- **Multi-tenant.** Schemas are prefixed per tenant; tenant is resolved at
  request time (see `com.ec.application.multitenant`). Never hardcode a schema
  or assume a single DB.
- **Custom IDs.** IDs come from `com.ec.application.IDGenerator`, not DB
  auto-increment. Don't assume sequential/auto IDs.
- **WAR packaging.** Deployed to Tomcat — don't add anything that breaks WAR
  build or assumes an embedded-container-only setup.
- **PDF = iText, Excel = POI.** Reuse existing generators; don't add new report
  libraries.
- **REST contract sync.** If a backend request/response or endpoint changes,
  update the matching axios call in `SC UI/src`.

## Conventions

- Show file paths before code changes; prefer minimal diffs over rewrites.
- Don't introduce new frameworks/libraries or refactor unrelated code without
  being asked.
- Don't assume DB schema changes unless specified.
- Keep backend and frontend in sync when an API contract moves.

## Scope note

`sc-inventory-service/` and `SC UI/` are the primary active areas. Touch
`sc-crm-service/` and `sc-common-service/` only when the task calls for it.

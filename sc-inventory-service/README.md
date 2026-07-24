# Inventory Service

Operational core of the ERP: material planning, stock movement, procurement,
and BOQ-driven control. Spring Boot 2.2.2 (Java 8), packaged as a WAR for
Tomcat, backed by multi-tenant MySQL.

## Modules

- Inventory masters & reference data
- BOQ management and reporting
- Indents and purchase orders
- Inward / outward / stock movement
- Live stock, stock summary, dead stock, reorder monitoring
- Cross-project / cross-tenant inventory transfers
- Pricing, usage, and operational reporting; global dashboards

## Stack notes

- **Multi-tenant** — schema resolved per request (`com.ec.application.multitenant`)
- **Custom IDs** — via `com.ec.application.IDGenerator`, not DB auto-increment
- **PDF** — iText (purchase orders); **Excel** — Apache POI; **Files** — MinIO

## Build & run

```bash
mvn clean package        # -> target/*.war
mvn spring-boot:run      # local
```

Base package: `com.ec.application` (`controller` / `service` / `repository` /
`model`). See root [CLAUDE.md](../CLAUDE.md) for architecture rules.

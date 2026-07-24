# Common Service

Shared platform layer used by the inventory and CRM services. Spring Boot
(Java 8), packaged as a WAR for Tomcat.

## Responsibilities

- Authentication and token-based access
- User and role management
- Tenant and access mapping
- Shared notification support

## Build & run

```bash
mvn clean package        # -> target/*.war
mvn spring-boot:run      # local
```

See root [CLAUDE.md](../CLAUDE.md) for architecture rules.

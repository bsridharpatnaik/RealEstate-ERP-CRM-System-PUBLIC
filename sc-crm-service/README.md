# CRM Service

Customer lifecycle and post-sales workflows: leads, pipeline, customer
engagement, and payment tracking. Spring Boot 2.2.2 (Java 8) with WebFlux,
Lombok, and ModelMapper; packaged as a WAR for Tomcat.

## Responsibilities

- Lead capture and pipeline execution
- Customer engagement and post-sales workflows
- Payment tracking

## Build & run

```bash
mvn clean package        # -> target/*.war
mvn spring-boot:run      # local
```

Auth, users, and tenants come from the common service. See root
[CLAUDE.md](../CLAUDE.md) for architecture rules.

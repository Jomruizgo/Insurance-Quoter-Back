---
name: Insurance-Quoter-Core — setup y configuración base
description: Paquete base, dependencias y configuración real del proyecto Insurance-Quoter-Core
type: project
---

Datos verificados del proyecto `Insurance-Quoter-Core` (plataforma-core-ohs):

- **Paquete base real**: `com.sofka.insurancequoter.core`
- **Puerto**: 8081
- **Base de datos**: `insurance_core_db` en PostgreSQL 5433
- **Credenciales Docker Compose**: user=`myuser`, password=`secret`, db=`insurance_core_db`
- **Build**: Gradle con Kotlin DSL (`build.gradle.kts`), Java 21, Spring Boot 4
- **Dependencias añadidas**: `flyway-core`, `flyway-database-postgresql`, `springdoc-openapi-starter-webmvc-ui:2.8.6`
- **application.properties** contiene datasource + JPA + Flyway configurados

**Why:** El proyecto venía sin datasource, Flyway ni Swagger configurados — se agregaron al implementar folio-generator.

**How to apply:** Al agregar nuevas features al core, el paquete base y la configuración ya están listos. Flyway busca migraciones en `classpath:db/migration`.

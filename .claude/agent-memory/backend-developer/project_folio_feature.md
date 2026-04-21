---
name: folio-generator feature — implementation state
description: Estado de implementación del feature folio-generator en Insurance-Quoter-Core (SPEC-001)
type: project
---

Feature `folio-generator` (SPEC-001) implementado y compilando correctamente.

**Why:** El microservicio core expone `GET /v1/folios` que genera folios únicos `FOL-<YYYY>-<NNNNN>` usando `nextval('folio_sequence')` de PostgreSQL. Concurrencia garantizada por el motor, sin locks de aplicación.

**How to apply:** Al trabajar sobre folios en Insurance-Quoter-Core, la estructura de paquetes ya existe bajo `com.sofka.insurancequoter.core.folio`. El contexto usa `Clock` inyectable para facilitar tests. No agregar `@Service` a `GenerateFolioUseCaseImpl` — se registra vía `@Bean` en `FolioConfig`.

Archivos clave:
- Migración: `src/main/resources/db/migration/V1__create_folio_sequence.sql`
- Domain: `folio/domain/model/Folio.java` (record puro)
- Use case: `folio/application/usecase/GenerateFolioUseCaseImpl.java`
- Adapter: `folio/infrastructure/adapter/out/persistence/adapter/FolioSequenceJpaAdapter.java`
- Controller: `folio/infrastructure/adapter/in/rest/FolioController.java`
- Config: `folio/infrastructure/config/FolioConfig.java`

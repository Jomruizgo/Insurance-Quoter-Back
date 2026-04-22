---
id: SPEC-006
status: DRAFT
feature: coverage-options
created: 2026-04-22
updated: 2026-04-22
author: spec-generator
version: "1.0"
related-specs:
  - SPEC-001  # folio-generator (Quote aggregate raíz, QuoteJpa con @Version)
  - SPEC-002  # folio-management (QuoteJpa, GlobalExceptionHandler)
  - SPEC-005  # quote-state (QuoteSections.coverageOptions — SectionStatus)
---

# Spec: Opciones de Cobertura de Cotización

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción

`Insurance-Quoter-Back` expone dos endpoints para gestionar las opciones de cobertura de una cotización: consultar la lista persistida y reemplazar/actualizar la lista completa. Las coberturas disponibles se validan contra el catálogo del servicio core (`GET /v1/catalogs/guarantees`). Solo se permite seleccionar coberturas cuyo código exista en el catálogo. El versionado optimista es obligatorio en la operación de escritura.

### Requerimiento de Negocio

> - `GET /v1/quotes/{folio}/coverage-options` — retorna las opciones de cobertura actualmente configuradas para la cotización.
> - `PUT /v1/quotes/{folio}/coverage-options` — reemplaza la lista completa de opciones de cobertura. Valida que cada `code` exista en el catálogo del core service. Valida rangos de `deductiblePercentage` y `coinsurancePercentage` (0.0–100.0). Requiere `version` para control de concurrencia optimista.
> - El catálogo de coberturas disponibles proviene de `GET /v1/catalogs/guarantees` del core service (puerto 8081).
> - Solo se pueden seleccionar coberturas (`selected: true`) cuyo `code` exista en el catálogo del core.
> - `deductiblePercentage` y `coinsurancePercentage`: valor decimal inclusivo entre 0.0 y 100.0.
> - El versionado optimista se aplica sobre `QuoteJpa` (aggregate raíz), incrementando `version` en cada PUT exitoso.
> - Entidad JPA: `CoverageOptionJpa` (tabla `coverage_options`), con FK a `quotes`.

### Historias de Usuario

#### HU-01: Consultar opciones de cobertura de una cotización

```
Como:        agente de seguros
Quiero:      llamar GET /v1/quotes/{folio}/coverage-options
Para:        visualizar las opciones de cobertura configuradas y su estado de selección

Prioridad:   Alta
Estimación:  S
Dependencias: SPEC-002 (Quote existe en la tabla quotes)
Capa:        Backend
```

#### Criterios de Aceptación — HU-01

**Happy Path**
```gherkin
CRITERIO-1.1: Consulta exitosa de opciones de cobertura configuradas
  Dado que:  existe el folio "FOL-2026-00042" con dos opciones de cobertura persistidas
  Cuando:    se realiza GET /v1/quotes/FOL-2026-00042/coverage-options
  Entonces:  la respuesta tiene HTTP 200
             Y el body contiene "folioNumber": "FOL-2026-00042"
             Y "coverageOptions" es un array con las dos opciones
             Y cada opción incluye: code, description, selected, deductiblePercentage, coinsurancePercentage
             Y "version" refleja la versión actual de la cotización
```

**Edge Case — sin opciones configuradas**
```gherkin
CRITERIO-1.2: Cotización sin opciones de cobertura persistidas
  Dado que:  existe el folio "FOL-2026-00042" sin ninguna opción de cobertura registrada
  Cuando:    se realiza GET /v1/quotes/FOL-2026-00042/coverage-options
  Entonces:  la respuesta tiene HTTP 200
             Y "coverageOptions" es un array vacío []
             Y "version" refleja la versión actual de la cotización
```

**Error Path**
```gherkin
CRITERIO-1.3: Folio inexistente
  Dado que:  el folio "FOL-9999-00001" no existe en la base de datos
  Cuando:    se realiza GET /v1/quotes/FOL-9999-00001/coverage-options
  Entonces:  la respuesta tiene HTTP 404
             Y el body es {"error": "Folio not found", "code": "FOLIO_NOT_FOUND"}
```

---

#### HU-02: Configurar opciones de cobertura de una cotización

```
Como:        agente de seguros
Quiero:      llamar PUT /v1/quotes/{folio}/coverage-options con la lista de coberturas seleccionadas
Para:        registrar qué coberturas aplican a la cotización con sus parámetros (deducible, coaseguro)

Prioridad:   Alta
Estimación:  M
Dependencias: HU-01, SPEC-002
Capa:        Backend
```

#### Criterios de Aceptación — HU-02

**Happy Path — configuración exitosa**
```gherkin
CRITERIO-2.1: PUT exitoso con coberturas válidas del catálogo
  Dado que:  existe el folio "FOL-2026-00042" con version 6
             Y el core service retorna el catálogo con codes "GUA-FIRE" y "GUA-THEFT"
             Y el request incluye ambos codes con deductiblePercentage y coinsurancePercentage en rango válido
  Cuando:    se realiza PUT /v1/quotes/FOL-2026-00042/coverage-options con version 6
  Entonces:  la respuesta tiene HTTP 200
             Y "coverageOptions" refleja la lista enviada con sus parámetros
             Y "version" es 7
             Y "updatedAt" es un timestamp ISO-8601 UTC
```

**Happy Path — mix de seleccionadas y no seleccionadas**
```gherkin
CRITERIO-2.2: PUT con coberturas seleccionadas y no seleccionadas
  Dado que:  el catálogo del core tiene "GUA-FIRE" y "GUA-THEFT"
  Cuando:    se realiza PUT con "GUA-FIRE" selected: true y "GUA-THEFT" selected: false
  Entonces:  la respuesta tiene HTTP 200
             Y "coverageOptions[0].selected" es true para GUA-FIRE
             Y "coverageOptions[1].selected" es false para GUA-THEFT
             Y la versión se incrementa en 1
```

**Error Path — version conflict**
```gherkin
CRITERIO-2.3: Conflicto de versión optimista
  Dado que:  la versión actual del folio "FOL-2026-00042" es 7
  Cuando:    se realiza PUT /v1/quotes/FOL-2026-00042/coverage-options con version 6
  Entonces:  la respuesta tiene HTTP 409
             Y el body es {"error": "Optimistic lock conflict", "code": "VERSION_CONFLICT"}
             Y los datos de cobertura no se modifican
```

**Error Path — code no existe en catálogo**
```gherkin
CRITERIO-2.4: Cobertura con code inválido rechazada
  Dado que:  el catálogo del core NO contiene el code "COV-INVALID"
  Cuando:    se realiza PUT /v1/quotes/FOL-2026-00042/coverage-options con un elemento cuyo code es "COV-INVALID"
  Entonces:  la respuesta tiene HTTP 422
             Y el body contiene "code": "VALIDATION_ERROR"
             Y "fields" identifica el code inválido
```

**Error Path — deductiblePercentage fuera de rango**
```gherkin
CRITERIO-2.5: Porcentaje de deducible fuera de rango [0,100]
  Dado que:  el request incluye una cobertura con deductiblePercentage: 150.0
  Cuando:    se realiza PUT /v1/quotes/FOL-2026-00042/coverage-options
  Entonces:  la respuesta tiene HTTP 422
             Y el body contiene "code": "VALIDATION_ERROR"
             Y "fields" identifica deductiblePercentage como inválido
```

**Error Path — coinsurancePercentage fuera de rango**
```gherkin
CRITERIO-2.6: Porcentaje de coaseguro fuera de rango [0,100]
  Dado que:  el request incluye una cobertura con coinsurancePercentage: -5.0
  Cuando:    se realiza PUT /v1/quotes/FOL-2026-00042/coverage-options
  Entonces:  la respuesta tiene HTTP 422
             Y el body contiene "code": "VALIDATION_ERROR"
             Y "fields" identifica coinsurancePercentage como inválido
```

**Error Path — folio inexistente**
```gherkin
CRITERIO-2.7: Folio inexistente en PUT
  Dado que:  el folio "FOL-9999-00001" no existe
  Cuando:    se realiza PUT /v1/quotes/FOL-9999-00001/coverage-options con cualquier body válido
  Entonces:  la respuesta tiene HTTP 404
             Y el body contiene "code": "FOLIO_NOT_FOUND"
```

### Reglas de Negocio

1. **Catálogo del core obligatorio:** antes de persistir, el backend debe consultar `GET /v1/catalogs/guarantees` al core service (puerto 8081) y validar que cada `code` en el request exista en el catálogo retornado.
2. **Solo códigos del catálogo:** si algún `code` del request no existe en el catálogo, la operación es rechazada con HTTP 422 `VALIDATION_ERROR`. El catálogo es la fuente de verdad.
3. **Rango de deductiblePercentage:** valor decimal `>= 0.0` y `<= 100.0`. Validación a nivel de Bean Validation (`@DecimalMin("0.0")` / `@DecimalMax("100.0")`).
4. **Rango de coinsurancePercentage:** valor decimal `>= 0.0` y `<= 100.0`. Misma validación que `deductiblePercentage`.
5. **Versionado optimista:** el campo `version` del request debe coincidir con el valor almacenado en `QuoteJpa`. Si difiere → HTTP 409 `VERSION_CONFLICT`. El versionado se gestiona en `QuoteJpa` (aggregate raíz), no en `CoverageOptionJpa`.
6. **Reemplazo completo:** el PUT reemplaza la lista completa de opciones de cobertura para el folio (delete all + insert all en la misma transacción).
7. **`description` enriquecida:** al persistir, la `description` se toma del catálogo del core (no del request). El request solo envía `code`, `selected`, `deductiblePercentage` y `coinsurancePercentage`.
8. **Actualización de `updatedAt`:** toda escritura exitosa actualiza `updatedAt` en `QuoteJpa` vía `@UpdateTimestamp`.
9. **Folio debe existir:** tanto GET como PUT validan la existencia del folio; si no existe → HTTP 404 `FOLIO_NOT_FOUND`.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `CoverageOptionJpa` | tabla `coverage_options` | **nueva** | Opción de cobertura con FK a `quotes` |
| `QuoteJpa` | tabla `quotes` | sin cambios estructurales | `@Version` gestiona el optimistic lock |

#### Tabla `coverage_options` (migración V7 — nueva)

| Columna | Tipo SQL | Obligatorio | Constraint | Descripción |
|---------|----------|-------------|------------|-------------|
| `id` | `BIGSERIAL` | sí | PK | Identificador interno |
| `quote_id` | `BIGINT` | sí | FK → `quotes.id` ON DELETE CASCADE | Cotización propietaria |
| `code` | `VARCHAR(50)` | sí | NOT NULL | Código de cobertura del catálogo (ej. GUA-FIRE) |
| `description` | `VARCHAR(255)` | no | — | Descripción enriquecida desde el catálogo del core |
| `selected` | `BOOLEAN` | sí | NOT NULL DEFAULT FALSE | ¿Cobertura seleccionada? |
| `deductible_percentage` | `DECIMAL(5,2)` | no | CHECK >= 0 AND <= 100 | % deducible |
| `coinsurance_percentage` | `DECIMAL(5,2)` | no | CHECK >= 0 AND <= 100 | % coaseguro |

#### Índices / Constraints

- `pk_coverage_options` — PRIMARY KEY en `id`
- `fk_coverage_options_quote_id` — FK a `quotes.id` ON DELETE CASCADE
- `uk_coverage_options_quote_code` — UNIQUE `(quote_id, code)` — un folio no repite el mismo code
- `idx_coverage_options_quote_id` — índice en `quote_id` para consultas por folio

#### Migración Flyway

```sql
-- V7__create_coverage_options_table.sql
CREATE TABLE IF NOT EXISTS coverage_options (
    id                     BIGSERIAL PRIMARY KEY,
    quote_id               BIGINT         NOT NULL
        REFERENCES quotes(id) ON DELETE CASCADE,
    code                   VARCHAR(50)    NOT NULL,
    description            VARCHAR(255),
    selected               BOOLEAN        NOT NULL DEFAULT FALSE,
    deductible_percentage  DECIMAL(5,2)
        CONSTRAINT chk_deductible_pct CHECK (deductible_percentage >= 0 AND deductible_percentage <= 100),
    coinsurance_percentage DECIMAL(5,2)
        CONSTRAINT chk_coinsurance_pct CHECK (coinsurance_percentage >= 0 AND coinsurance_percentage <= 100),
    CONSTRAINT uk_coverage_options_quote_code UNIQUE (quote_id, code)
);

CREATE INDEX IF NOT EXISTS idx_coverage_options_quote_id ON coverage_options (quote_id);
```

#### Domain Model

```java
// coverage/domain/model/CoverageOption.java
public record CoverageOption(
    String code,
    String description,
    boolean selected,
    BigDecimal deductiblePercentage,
    BigDecimal coinsurancePercentage
) {}
```

### API Endpoints

#### GET /v1/quotes/{folio}/coverage-options

- **Descripción:** Retorna todas las opciones de cobertura persistidas para la cotización
- **Auth requerida:** no
- **Path param:** `folio` — número de folio (ej. `FOL-2026-00042`)
- **Response 200:**
  ```json
  {
    "folioNumber": "FOL-2026-00042",
    "coverageOptions": [
      {
        "code": "GUA-FIRE",
        "description": "Incendio edificios",
        "selected": true,
        "deductiblePercentage": 2.0,
        "coinsurancePercentage": 80.0
      },
      {
        "code": "GUA-THEFT",
        "description": "Robo",
        "selected": false,
        "deductiblePercentage": 5.0,
        "coinsurancePercentage": 100.0
      }
    ],
    "version": 6
  }
  ```
- **Response 404:** `{"error": "Folio not found", "code": "FOLIO_NOT_FOUND"}`

---

#### PUT /v1/quotes/{folio}/coverage-options

- **Descripción:** Reemplaza la lista completa de opciones de cobertura; valida contra catálogo del core
- **Auth requerida:** no
- **Path param:** `folio` — número de folio
- **Request Body:**
  ```json
  {
    "coverageOptions": [
      {
        "code": "GUA-FIRE",
        "selected": true,
        "deductiblePercentage": 2.0,
        "coinsurancePercentage": 80.0
      },
      {
        "code": "GUA-THEFT",
        "selected": true,
        "deductiblePercentage": 5.0,
        "coinsurancePercentage": 100.0
      }
    ],
    "version": 6
  }
  ```
- **Response 200:**
  ```json
  {
    "folioNumber": "FOL-2026-00042",
    "coverageOptions": [
      {
        "code": "GUA-FIRE",
        "description": "Incendio edificios",
        "selected": true,
        "deductiblePercentage": 2.0,
        "coinsurancePercentage": 80.0
      },
      {
        "code": "GUA-THEFT",
        "description": "Robo",
        "selected": true,
        "deductiblePercentage": 5.0,
        "coinsurancePercentage": 100.0
      }
    ],
    "updatedAt": "2026-04-22T15:45:00Z",
    "version": 7
  }
  ```
- **Response 404:** `{"error": "Folio not found", "code": "FOLIO_NOT_FOUND"}`
- **Response 409:** `{"error": "Optimistic lock conflict", "code": "VERSION_CONFLICT"}`
- **Response 422 — code inválido:** `{"error": "Validation failed", "code": "VALIDATION_ERROR", "fields": [{"field": "coverageOptions[0].code", "message": "Code not found in catalog"}]}`
- **Response 422 — porcentaje inválido:** `{"error": "Validation failed", "code": "VALIDATION_ERROR", "fields": [{"field": "coverageOptions[0].deductiblePercentage", "message": "must be between 0.0 and 100.0"}]}`

---

### Arquitectura Hexagonal — Descomposición de Componentes

#### Bounded context: `coverage`

```
com.sofka.insurancequoter.back.coverage/
├── domain/
│   ├── model/
│   │   └── CoverageOption.java                            ← Record (code, description, selected, deductiblePercentage, coinsurancePercentage)
│   └── port/
│       ├── in/
│       │   ├── GetCoverageOptionsUseCase.java             ← Input Port: CoverageOptionsResponse getCoverageOptions(String folioNumber)
│       │   └── SaveCoverageOptionsUseCase.java            ← Input Port: CoverageOptionsResponse saveCoverageOptions(SaveCoverageOptionsCommand)
│       └── out/
│           ├── CoverageOptionRepository.java             ← Output Port: findByFolioNumber / replaceAll
│           ├── QuoteLookupPort.java                      ← Output Port: verifica existencia y versión del folio
│           └── GuaranteeCatalogClient.java               ← Output Port: List<GuaranteeDto> fetchGuarantees()
├── application/
│   └── usecase/
│       ├── GetCoverageOptionsUseCaseImpl.java            ← Orquesta: lookup folio → buscar opciones → retornar
│       ├── SaveCoverageOptionsUseCaseImpl.java           ← Orquesta: lookup folio → validar versión → validar codes → enriquecer → persistir
│       ├── command/
│       │   └── SaveCoverageOptionsCommand.java           ← Record: folioNumber, coverageOptions, version
│       ├── dto/
│       │   └── CoverageOptionsResponse.java             ← Record: folioNumber, coverageOptions, updatedAt, version
│       └── exception/
│           └── InvalidCoverageCodeException.java         ← Error de dominio: code no existe en catálogo
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   └── rest/
    │   │       ├── CoverageController.java               ← implementa CoverageApi; inyecta use cases y mapper
    │   │       ├── swaggerdocs/
    │   │       │   └── CoverageApi.java                  ← @Tag "Coverage Options"; @Operation GET y PUT
    │   │       ├── dto/
    │   │       │   ├── request/
    │   │       │   │   ├── SaveCoverageOptionsRequest.java    ← { coverageOptions: [...], version }
    │   │       │   │   └── CoverageOptionItemRequest.java     ← { code, selected, deductiblePercentage, coinsurancePercentage }
    │   │       │   └── response/
    │   │       │       ├── CoverageOptionsListResponse.java   ← { folioNumber, coverageOptions, updatedAt, version }
    │   │       │       └── CoverageOptionItemResponse.java    ← { code, description, selected, deductiblePercentage, coinsurancePercentage }
    │   │       └── mapper/
    │   │           └── CoverageRestMapper.java
    │   └── out/
    │       ├── persistence/
    │       │   ├── adapter/
    │       │   │   └── CoverageOptionJpaAdapter.java      ← implementa CoverageOptionRepository + QuoteLookupPort
    │       │   ├── repositories/
    │       │   │   ├── CoverageOptionJpaRepository.java   ← extiende JpaRepository<CoverageOptionJpa, Long>
    │       │   │   └── QuoteJpaRepository.java            ← existente de SPEC-002 — reutilizar para lookup por folioNumber
    │       │   ├── entities/
    │       │   │   └── CoverageOptionJpa.java             ← @Entity tabla coverage_options; @ManyToOne QuoteJpa
    │       │   └── mappers/
    │       │       └── CoverageOptionPersistenceMapper.java
    │       └── http/
    │           ├── adapter/
    │           │   └── GuaranteeCatalogClientAdapter.java ← implementa GuaranteeCatalogClient; llama GET /v1/catalogs/guarantees al core
    │           └── dto/
    │               ├── GuaranteeCatalogResponse.java      ← { guarantees: [...] }
    │               └── GuaranteeDto.java                  ← { code, description, tarifable }
    └── config/
        └── CoverageConfig.java                           ← @Bean wiring de use cases + RestClient para core
```

#### Contratos de interfaces clave

```java
// domain/port/in/GetCoverageOptionsUseCase.java
public interface GetCoverageOptionsUseCase {
    CoverageOptionsResponse getCoverageOptions(String folioNumber);
}

// domain/port/in/SaveCoverageOptionsUseCase.java
public interface SaveCoverageOptionsUseCase {
    CoverageOptionsResponse saveCoverageOptions(SaveCoverageOptionsCommand command);
}

// domain/port/out/CoverageOptionRepository.java
public interface CoverageOptionRepository {
    List<CoverageOption> findByFolioNumber(String folioNumber);
    List<CoverageOption> replaceAll(String folioNumber, List<CoverageOption> options);
}

// domain/port/out/QuoteLookupPort.java
public interface QuoteLookupPort {
    void assertFolioExists(String folioNumber);      // lanza FolioNotFoundException si no existe
    long getCurrentVersion(String folioNumber);       // retorna version actual
    void assertVersionMatches(String folioNumber, long expectedVersion); // lanza VersionConflictException si difiere
}

// domain/port/out/GuaranteeCatalogClient.java
public interface GuaranteeCatalogClient {
    List<GuaranteeDto> fetchGuarantees();  // llama GET /v1/catalogs/guarantees al core
}
```

#### Flujo de llamada — PUT (reemplazo)

```
CoverageController.saveCoverageOptions(folio, request)
  → SaveCoverageOptionsUseCase.saveCoverageOptions(command)
      1. QuoteLookupPort.assertFolioExists(folio)            ← 404 si no existe
      2. QuoteLookupPort.assertVersionMatches(folio, version) ← 409 si conflict
      3. GuaranteeCatalogClient.fetchGuarantees()            ← obtiene catálogo del core
      4. Para cada opción en command.coverageOptions:
         a. Validar que code existe en el catálogo            ← InvalidCoverageCodeException si no
         b. Enriquecer description desde el catálogo
      5. CoverageOptionRepository.replaceAll(folio, enrichedOptions) ← delete + insert en una tx
      6. return CoverageOptionsResponse (con version incrementada por @Version de QuoteJpa)
  ← ResponseEntity<CoverageOptionsListResponse>(200)
```

#### Manejo de excepciones

| Excepción de dominio | HTTP | Código |
|---------------------|------|--------|
| `FolioNotFoundException` | 404 | `FOLIO_NOT_FOUND` |
| `VersionConflictException` | 409 | `VERSION_CONFLICT` |
| `InvalidCoverageCodeException` | 422 | `VALIDATION_ERROR` |
| `MethodArgumentNotValidException` | 422 | `VALIDATION_ERROR` |

> `GlobalExceptionHandler` (existente de SPEC-002) debe agregar handler para `InvalidCoverageCodeException` → 422 `VALIDATION_ERROR`.

### Notas de Implementación

- **Reutilizar `FolioNotFoundException` y `VersionConflictException`:** estas excepciones ya existen en `location/application/usecase/`. Mover a un paquete compartido (ej. `com.sofka.insurancequoter.back.shared.exception`) o reusar desde `location`. Verificar que `GlobalExceptionHandler` ya las registra.
- **`QuoteLookupPort` — `QuoteJpa` y `@Version`:** el incremento de `version` es automático vía Hibernate cuando `QuoteJpa` se guarda. La implementación de `replaceAll` debe realizar un `save(quoteJpa)` con un cambio de campo (ej. `updatedAt`) para que `@Version` incremente. Alternativa: usar `@Query` con `UPDATE ... SET version = version + 1`.
- **`CoverageOptionJpaAdapter.replaceAll`:** debe implementarse como una transacción atómica: (1) `deleteByQuoteId(quote.id)` + (2) `saveAll(newOptions)`. Marcar con `@Transactional`.
- **`GuaranteeCatalogClientAdapter`:** utilizar el `RestClient` configurado en `CoverageConfig` apuntando al core service (puerto 8081). En caso de error del core → lanzar `CoreServiceException` (ya existente). Para tests, usar WireMock.
- **Validación de rangos con Bean Validation:** `CoverageOptionItemRequest` debe usar `@DecimalMin("0.0")` y `@DecimalMax("100.0")` en `deductiblePercentage` y `coinsurancePercentage`. La validación de `code` contra catálogo es lógica de negocio → se hace en el use case (no con anotaciones).
- **`QuoteJpaRepository` reutilizado:** el repositorio JPA de `quotes` ya existe en el contexto `folio`. `CoverageOptionJpaAdapter` puede inyectarlo directamente o exponerlo vía un nuevo output port `QuoteLookupPort` implementado por un adapter en `coverage/infrastructure/adapter/out/persistence/`.
- **Orden de DELETE + INSERT:** al reemplazar, siempre hacer `deleteAllByQuoteId` antes del `saveAll` dentro de la misma transacción para respetar el constraint `UNIQUE (quote_id, code)`.

---

## 3. LISTA DE TAREAS

> Checklist accionable. Marcar cada ítem (`[x]`) al completarlo.

### Backend

#### Base de Datos
- [ ] Crear migración `V7__create_coverage_options_table.sql` — tabla `coverage_options` con columnas, FK a `quotes`, constraint UNIQUE `(quote_id, code)`, CHECK constraints para porcentajes, índice `idx_coverage_options_quote_id`

#### Dominio
- [ ] Crear record `CoverageOption` en `coverage/domain/model/` — campos: `code`, `description`, `selected`, `deductiblePercentage`, `coinsurancePercentage`
- [ ] Crear Input Port `GetCoverageOptionsUseCase` en `coverage/domain/port/in/`
- [ ] Crear Input Port `SaveCoverageOptionsUseCase` en `coverage/domain/port/in/`
- [ ] Crear Output Port `CoverageOptionRepository` en `coverage/domain/port/out/` — métodos: `findByFolioNumber`, `replaceAll`
- [ ] Crear Output Port `QuoteLookupPort` en `coverage/domain/port/out/` — métodos: `assertFolioExists`, `getCurrentVersion`, `assertVersionMatches`
- [ ] Crear Output Port `GuaranteeCatalogClient` en `coverage/domain/port/out/` — método: `fetchGuarantees`

#### Aplicación
- [ ] Crear record `SaveCoverageOptionsCommand` en `coverage/application/usecase/command/` — campos: `folioNumber`, `coverageOptions`, `version`
- [ ] Crear record `CoverageOptionsResponse` en `coverage/application/usecase/dto/` — campos: `folioNumber`, `coverageOptions`, `updatedAt`, `version`
- [ ] Crear `InvalidCoverageCodeException` en `coverage/application/usecase/exception/`
- [ ] Crear `GetCoverageOptionsUseCaseImpl` en `coverage/application/usecase/` — orquesta lookup + buscar opciones + retornar
- [ ] Crear `SaveCoverageOptionsUseCaseImpl` en `coverage/application/usecase/` — orquesta validación de versión + validación de codes + enriquecimiento + persistencia

#### Infraestructura — Persistencia
- [ ] Crear `CoverageOptionJpa` en `coverage/infrastructure/adapter/out/persistence/entities/` — `@Entity`, `@ManyToOne` a `QuoteJpa`, columnas según diseño
- [ ] Crear `CoverageOptionJpaRepository` en `coverage/infrastructure/adapter/out/persistence/repositories/` — extiende `JpaRepository<CoverageOptionJpa, Long>`; método `findByQuoteId` y `deleteAllByQuoteId`
- [ ] Crear `CoverageOptionPersistenceMapper` en `coverage/infrastructure/adapter/out/persistence/mappers/` — `CoverageOption ↔ CoverageOptionJpa`
- [ ] Crear `CoverageOptionJpaAdapter` en `coverage/infrastructure/adapter/out/persistence/adapter/` — implementa `CoverageOptionRepository` y `QuoteLookupPort`; método `replaceAll` con `@Transactional`

#### Infraestructura — HTTP Client
- [ ] Crear `GuaranteeDto` y `GuaranteeCatalogResponse` en `coverage/infrastructure/adapter/out/http/dto/`
- [ ] Crear `GuaranteeCatalogClientAdapter` en `coverage/infrastructure/adapter/out/http/adapter/` — implementa `GuaranteeCatalogClient`; llama `GET /v1/catalogs/guarantees` al core

#### Infraestructura — REST
- [ ] Crear `CoverageOptionItemRequest` en `coverage/infrastructure/adapter/in/rest/dto/request/` — `@DecimalMin`/`@DecimalMax` en porcentajes
- [ ] Crear `SaveCoverageOptionsRequest` en `coverage/infrastructure/adapter/in/rest/dto/request/` — `@Valid @NotNull` en `coverageOptions`; `@NotNull` en `version`
- [ ] Crear `CoverageOptionItemResponse` en `coverage/infrastructure/adapter/in/rest/dto/response/`
- [ ] Crear `CoverageOptionsListResponse` en `coverage/infrastructure/adapter/in/rest/dto/response/`
- [ ] Crear `CoverageRestMapper` en `coverage/infrastructure/adapter/in/rest/mapper/`
- [ ] Crear Swagger interface `CoverageApi` en `coverage/infrastructure/adapter/in/rest/swaggerdocs/` — `@Tag "Coverage Options"`, `@Operation` para GET y PUT
- [ ] Crear `CoverageController` en `coverage/infrastructure/adapter/in/rest/` — implementa `CoverageApi`; inyecta use cases vía constructor
- [ ] Actualizar `GlobalExceptionHandler` — agregar handler para `InvalidCoverageCodeException` → 422 `VALIDATION_ERROR` con `fields`

#### Configuración
- [ ] Crear `CoverageConfig` en `coverage/infrastructure/config/` — `@Bean` wiring de `GetCoverageOptionsUseCaseImpl`, `SaveCoverageOptionsUseCaseImpl`, `CoverageOptionJpaAdapter`, `GuaranteeCatalogClientAdapter`

#### Tests Backend (TDD — escribir el test ANTES de la implementación)

**GetCoverageOptionsUseCaseImpl**
- [ ] `GetCoverageOptionsUseCaseImplTest` — folio existente con opciones → retorna lista con todos los campos
- [ ] `GetCoverageOptionsUseCaseImplTest` — folio existente sin opciones → retorna lista vacía
- [ ] `GetCoverageOptionsUseCaseImplTest` — folio inexistente → lanza FolioNotFoundException

**SaveCoverageOptionsUseCaseImpl**
- [ ] `SaveCoverageOptionsUseCaseImplTest` — version conflict → lanza VersionConflictException sin persistir
- [ ] `SaveCoverageOptionsUseCaseImplTest` — folio inexistente → lanza FolioNotFoundException
- [ ] `SaveCoverageOptionsUseCaseImplTest` — code no existe en catálogo → lanza InvalidCoverageCodeException
- [ ] `SaveCoverageOptionsUseCaseImplTest` — guardado exitoso: retorna opciones con descripción enriquecida desde catálogo y version incrementada
- [ ] `SaveCoverageOptionsUseCaseImplTest` — mix de selected true/false: persiste correctamente ambos estados

**CoverageOptionJpaAdapter**
- [ ] `CoverageOptionJpaAdapterTest` — findByFolioNumber retorna lista vacía si no hay opciones
- [ ] `CoverageOptionJpaAdapterTest` — replaceAll elimina existentes y persiste nuevas en la misma transacción
- [ ] `CoverageOptionJpaAdapterTest` — assertFolioExists lanza FolioNotFoundException si el folio no existe
- [ ] `CoverageOptionJpaAdapterTest` — assertVersionMatches lanza VersionConflictException si version difiere

**GuaranteeCatalogClientAdapter**
- [ ] `GuaranteeCatalogClientAdapterTest` — respuesta exitosa del core → retorna lista de GuaranteeDto (WireMock)
- [ ] `GuaranteeCatalogClientAdapterTest` — error del core (5xx) → lanza CoreServiceException (WireMock)

**CoverageController**
- [ ] `CoverageControllerTest` — GET /v1/quotes/{folio}/coverage-options → HTTP 200 con lista de opciones
- [ ] `CoverageControllerTest` — GET con folio inexistente → HTTP 404 FOLIO_NOT_FOUND
- [ ] `CoverageControllerTest` — PUT exitoso → HTTP 200 con versión incrementada
- [ ] `CoverageControllerTest` — PUT con version conflict → HTTP 409 VERSION_CONFLICT
- [ ] `CoverageControllerTest` — PUT con code inválido → HTTP 422 VALIDATION_ERROR
- [ ] `CoverageControllerTest` — PUT con deductiblePercentage > 100 → HTTP 422 VALIDATION_ERROR
- [ ] `CoverageControllerTest` — PUT con coinsurancePercentage < 0 → HTTP 422 VALIDATION_ERROR

**Tests de integración**
- [ ] `CoverageOptionsIntegrationTest` — `@SpringBootTest` + Testcontainers PostgreSQL + WireMock para core: PUT luego GET retorna datos persistidos con descripción del catálogo
- [ ] `CoverageOptionsIntegrationTest` — PUT con version conflict: HTTP 409 y datos sin modificar
- [ ] `CoverageOptionsIntegrationTest` — PUT con code inválido (WireMock retorna catálogo sin ese code): HTTP 422
- [ ] `CoverageOptionsIntegrationTest` — GET folio sin opciones: HTTP 200 con array vacío

### QA
- [ ] Ejecutar skill `/gherkin-case-generator` → escenarios para CRITERIO-1.1…2.7
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD (concurrencia en version, core unavailable, constraint UNIQUE)
- [ ] Validar manualmente con Bruno/Postman: GET y PUT
- [ ] Verificar que `description` en la respuesta proviene del catálogo del core, no del request
- [ ] Verificar que DELETE + INSERT son atómicos (rollback si falla el INSERT)
- [ ] Verificar comportamiento cuando el core service no está disponible
- [ ] Actualizar estado spec: `status: IMPLEMENTED`

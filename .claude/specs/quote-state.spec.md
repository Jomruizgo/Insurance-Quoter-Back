---
id: SPEC-005
status: IMPLEMENTED
feature: quote-state
created: 2026-04-21
updated: 2026-04-21
author: spec-generator
version: "1.0"
related-specs:
  - SPEC-001  # folio-generator (Quote aggregate raíz)
  - SPEC-002  # folio-management (POST /v1/folios)
  - SPEC-003  # location-layout (layout columns en quotes)
  - SPEC-004  # location-management (locations + validationStatus + blockingAlerts)
---

# Spec: Estado y Progreso de Completitud de Cotización

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción

`Insurance-Quoter-Back` expone `GET /v1/quotes/{folio}/state` para consultar el estado actual y el porcentaje de completitud de una cotización. El endpoint evalúa cinco secciones del folio (`generalInfo`, `layout`, `locations`, `coverageOptions`, `calculation`), calcula el porcentaje en el use case (no se persiste) y retorna un snapshot de progreso que el frontend usa para renderizar el indicador de avance del flujo.

### Requerimiento de Negocio

El frontend necesita conocer, en una sola llamada, qué tan completo está el folio para habilitar o deshabilitar el botón de cálculo y mostrar el progreso por sección. Sin esta consulta el usuario no puede saber qué le falta completar antes de ejecutar el cálculo.

### Historias de Usuario

#### HU-01: Consultar estado y progreso de una cotización

```
Como:        agente de seguros que trabaja en un folio activo
Quiero:      consultar GET /v1/quotes/{folio}/state
Para:        conocer qué secciones están completas e incompletas y el progreso total

Prioridad:   Alta
Estimación:  S
Dependencias: SPEC-001, SPEC-002, SPEC-003, SPEC-004
Capa:        Backend
```

#### Criterios de Aceptación — HU-01

**Happy Path — folio con secciones mixtas**
```gherkin
CRITERIO-1.1: Consulta exitosa de estado con progreso parcial
  Dado que:  existe un folio "FOL-2026-00042" con layout configurado y al menos una ubicación completa
             pero sin opciones de cobertura ni cálculo ejecutado
  Cuando:    el agente llama GET /v1/quotes/FOL-2026-00042/state
  Entonces:  el sistema retorna HTTP 200 con quoteStatus, completionPercentage, sections detalladas,
             version y updatedAt
             Y completionPercentage refleja la proporción de secciones COMPLETE sobre 5
```

**Happy Path — folio recién creado**
```gherkin
CRITERIO-1.2: Folio recién creado tiene progreso cero
  Dado que:  existe un folio en estado CREATED sin ninguna sección completada
  Cuando:    el agente llama GET /v1/quotes/{folio}/state
  Entonces:  el sistema retorna HTTP 200 con completionPercentage = 0
             Y todas las secciones retornan PENDING excepto las que puedan inferirse como IN_PROGRESS
```

**Happy Path — folio totalmente calculado**
```gherkin
CRITERIO-1.3: Folio CALCULATED muestra todas las secciones COMPLETE
  Dado que:  existe un folio en estado CALCULATED con todas las secciones completadas
  Cuando:    el agente llama GET /v1/quotes/{folio}/state
  Entonces:  el sistema retorna HTTP 200 con completionPercentage = 100
             Y todas las secciones retornan COMPLETE
             Y quoteStatus = "CALCULATED"
```

**Error Path — folio inexistente**
```gherkin
CRITERIO-1.4: Folio no encontrado retorna 404
  Dado que:  el folio "FOL-INEXISTENTE" no existe en la base de datos
  Cuando:    el agente llama GET /v1/quotes/FOL-INEXISTENTE/state
  Entonces:  el sistema retorna HTTP 404
             Y el body contiene { "error": "Folio not found", "code": "FOLIO_NOT_FOUND" }
```

**Edge Case — secciones con dependencias no implementadas**
```gherkin
CRITERIO-1.5: Secciones pendientes de implementación retornan PENDING
  Dado que:  las features general-info y coverage-options aún no están implementadas
             (columnas o entidades relacionadas no existen en DB)
  Cuando:    el agente consulta el estado de cualquier folio
  Entonces:  las secciones generalInfo y coverageOptions retornan PENDING
             Y el sistema no lanza excepción por campos nulos
```

### Reglas de Negocio

1. `completionPercentage` = `(secciones en COMPLETE) / 5 * 100`, redondeado al entero más cercano.
2. La evaluación es **read-only**: ninguna escritura, ningún cambio de estado.
3. Una sección es `COMPLETE` cuando todos sus campos obligatorios están presentes y sin alertas bloqueantes activas.
4. Una sección es `INCOMPLETE` cuando tiene datos parciales con alertas bloqueantes.
5. Una sección es `IN_PROGRESS` cuando tiene datos parciales sin alertas bloqueantes (rellena a medias, pero sin errores).
6. Una sección es `PENDING` cuando no tiene ningún dato registrado.
7. La sección `generalInfo` se evalúa con los campos de datos del asegurado e información de suscripción. Requiere SPEC-006 (general-info); hasta entonces devuelve `PENDING`.
8. La sección `layout` se evalúa con `numberOfLocations` y `locationType` de la tabla `quotes`.
9. La sección `locations` se evalúa cruzando el `validationStatus` y `blockingAlerts` de todas las ubicaciones del folio.
10. La sección `coverageOptions` requiere SPEC-007 (coverage-options); hasta entonces devuelve `PENDING`.
11. La sección `calculation` es `COMPLETE` si `quoteStatus = CALCULATED`, `PENDING` en cualquier otro caso.
12. Si `quoteStatus = CALCULATED`, el porcentaje debe ser 100 independientemente del estado de las secciones individuales.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `QuoteJpa` | tabla `quotes` | **ninguno** — solo lectura | Aggregate raíz; aporta quoteStatus, numberOfLocations, locationType, version, updatedAt |
| `LocationJpa` | tabla `locations` | **ninguno** — solo lectura | Aporta validationStatus y blockingAlerts por folio |

No se crean tablas nuevas ni columnas nuevas en esta spec.

#### Campos leídos de `quotes` para evaluación de secciones

| Campo | Sección evaluada | Regla |
|-------|-----------------|-------|
| `quote_status` | `calculation`, `quoteStatus` | CALCULATED → calculation COMPLETE |
| `number_of_locations` | `layout` | > 0 → layout IN_PROGRESS o COMPLETE |
| `location_type` | `layout` | no null → layout avanza a COMPLETE |
| `version` | response | retornado como está |
| `updated_at` | response | retornado como está |

#### Campos leídos de `locations` para evaluación de `locations`

| Campo | Regla |
|-------|-------|
| `validation_status` | Si todas son `COMPLETE` → sección COMPLETE; alguna `INCOMPLETE` → sección INCOMPLETE |
| `blocking_alerts` (tabla `location_blocking_alerts`) | Confirmación de alertas activas |
| conteo de filas | Sin filas para el folio → sección PENDING |

#### Lógica de evaluación por sección

| Sección | PENDING | IN_PROGRESS | COMPLETE | INCOMPLETE |
|---------|---------|-------------|----------|------------|
| `generalInfo` | campos de asegurado nulos (requiere SPEC-006) | parcialmente llenos sin alertas | todos los campos obligatorios presentes | campos con alertas bloqueantes |
| `layout` | `numberOfLocations IS NULL` | — | `numberOfLocations > 0` AND `locationType IS NOT NULL` | — |
| `locations` | sin registros en `locations` para el folio | al menos una ubicación, ninguna COMPLETE del todo | todas las ubicaciones con `validationStatus = COMPLETE` | al menos una ubicación con `blockingAlerts` no vacía |
| `coverageOptions` | no configurado (requiere SPEC-007) | parcialmente configurado | al menos una opción con `selected = true` | — |
| `calculation` | `quoteStatus != CALCULATED` | — | `quoteStatus = CALCULATED` | — |

### Modelos de Dominio (nuevos)

```
folio/domain/model/
├── QuoteState.java          ← record (folioNumber, quoteStatus, completionPercentage, sections, version, updatedAt)
├── SectionStatus.java       ← enum (PENDING, IN_PROGRESS, COMPLETE, INCOMPLETE)
└── QuoteSections.java       ← record (generalInfo, layout, locations, coverageOptions, calculation)
```

```java
// QuoteState.java
public record QuoteState(
    String folioNumber,
    String quoteStatus,
    int completionPercentage,
    QuoteSections sections,
    Long version,
    Instant updatedAt
) {}

// QuoteSections.java
public record QuoteSections(
    SectionStatus generalInfo,
    SectionStatus layout,
    SectionStatus locations,
    SectionStatus coverageOptions,
    SectionStatus calculation
) {}

// SectionStatus.java
public enum SectionStatus { PENDING, IN_PROGRESS, COMPLETE, INCOMPLETE }
```

### Puertos (nuevos)

```
folio/domain/port/in/
└── GetQuoteStateUseCase.java          ← QuoteState getState(String folioNumber)

folio/domain/port/out/
└── LocationStateReader.java           ← LocationStateSummary readByFolioNumber(String folioNumber)
```

```java
// LocationStateReader.java — output port (leído desde folio BC sin depender de location BC en dominio)
public interface LocationStateReader {
    LocationStateSummary readByFolioNumber(String folioNumber);
}

// LocationStateSummary.java — VO en folio/domain/model
public record LocationStateSummary(
    int total,
    long completeCount,
    long incompleteCount   // tienen blockingAlerts
) {}
```

### API Endpoints

#### GET /v1/quotes/{folio}/state

- **Descripción:** Retorna el estado actual y progreso de completitud del folio
- **Auth requerida:** no
- **Path param:** `folio` — número de folio (ej. `FOL-2026-00042`)

**Response 200:**
```json
{
  "folioNumber": "FOL-2026-00042",
  "quoteStatus": "IN_PROGRESS",
  "completionPercentage": 40,
  "sections": {
    "generalInfo": "PENDING",
    "layout": "COMPLETE",
    "locations": "INCOMPLETE",
    "coverageOptions": "PENDING",
    "calculation": "PENDING"
  },
  "version": 6,
  "updatedAt": "2026-04-20T15:35:00Z"
}
```

**Response 404:**
```json
{ "error": "Folio not found", "code": "FOLIO_NOT_FOUND" }
```

### Arquitectura y Dependencias

**Bounded context:** `folio` — el endpoint pertenece al aggregate raíz `Quote`.

**Paquetes nuevos:**

```
folio/
├── domain/
│   ├── model/
│   │   ├── QuoteState.java              ← nuevo
│   │   ├── QuoteSections.java           ← nuevo
│   │   ├── SectionStatus.java           ← nuevo (enum)
│   │   └── LocationStateSummary.java    ← nuevo (VO)
│   └── port/
│       ├── in/
│       │   └── GetQuoteStateUseCase.java ← nuevo
│       └── out/
│           └── LocationStateReader.java  ← nuevo
├── application/
│   └── usecase/
│       └── GetQuoteStateUseCaseImpl.java ← nuevo
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   └── rest/
    │   │       ├── QuoteStateController.java    ← nuevo
    │   │       ├── dto/
    │   │       │   ├── QuoteStateResponse.java  ← nuevo
    │   │       │   └── SectionsResponse.java    ← nuevo
    │   │       ├── mapper/
    │   │       │   └── QuoteStateRestMapper.java ← nuevo
    │   │       └── swaggerdocs/
    │   │           └── QuoteStateApi.java       ← nuevo
    │   └── out/
    │       └── persistence/
    │           └── adapter/
    │               └── LocationStateJpaAdapter.java ← nuevo (usa LocationJpaRepository)
    └── config/
        └── FolioConfig.java    ← actualizar: añadir bean GetQuoteStateUseCaseImpl
```

**Servicios externos:** ninguno. Lectura pura de DB.

**Impacto en puntos de entrada:** `FolioConfig.java` requiere un nuevo `@Bean`.

### Notas de Implementación

- `LocationStateJpaAdapter` inyecta `LocationJpaRepository` (del bounded context `location`). Esto es aceptable porque es una query de solo lectura dentro del mismo módulo; si el proyecto evoluciona a microservicios reales, se reemplaza por un HTTP client.
- `GetQuoteStateUseCaseImpl` no lanza excepción cuando campos de `generalInfo` o `coverageOptions` son nulos — devuelve `PENDING` con null-safe guards.
- El `completionPercentage` cuando `quoteStatus = CALCULATED` debe forzarse a 100, ignorando el conteo de secciones individuales (por si alguna quedó sin datos en DB).
- El `GlobalExceptionHandler` ya maneja `FolioNotFoundException` → 404 `FOLIO_NOT_FOUND`; no se necesita cambio.

---

## 3. LISTA DE TAREAS

> Checklist accionable para todos los agentes. Marcar cada ítem (`[x]`) al completarlo.

### Backend

#### Dominio
- [ ] Crear enum `SectionStatus` — `PENDING`, `IN_PROGRESS`, `COMPLETE`, `INCOMPLETE`
- [ ] Crear record `LocationStateSummary` — `total`, `completeCount`, `incompleteCount`
- [ ] Crear record `QuoteSections` — cinco campos `SectionStatus`
- [ ] Crear record `QuoteState` — `folioNumber`, `quoteStatus`, `completionPercentage`, `sections`, `version`, `updatedAt`
- [ ] Crear input port `GetQuoteStateUseCase` — `QuoteState getState(String folioNumber)`
- [ ] Crear output port `LocationStateReader` — `LocationStateSummary readByFolioNumber(String folioNumber)`

#### Application
- [ ] Implementar `GetQuoteStateUseCaseImpl`:
  - [ ] Resolver `Quote` via `QuoteRepository.findByFolioNumber()` (lanzar `FolioNotFoundException` si no existe)
  - [ ] Resolver `LocationStateSummary` via `LocationStateReader`
  - [ ] Evaluar sección `layout` con null-safe de `numberOfLocations` y `locationType`
  - [ ] Evaluar sección `locations` con totales y conteos de `LocationStateSummary`
  - [ ] Evaluar sección `calculation` basada en `quoteStatus`
  - [ ] Devolver `PENDING` para `generalInfo` y `coverageOptions` hasta que sus specs se implementen
  - [ ] Calcular `completionPercentage`; forzar 100 si `quoteStatus = CALCULATED`

#### Persistence Adapter
- [ ] Implementar `LocationStateJpaAdapter implements LocationStateReader`:
  - [ ] Inyectar `LocationJpaRepository`
  - [ ] Contar total de ubicaciones por `folioNumber`
  - [ ] Contar las que tienen `validationStatus = 'COMPLETE'`
  - [ ] Contar las que tienen al menos una fila en `location_blocking_alerts`
  - [ ] Retornar `LocationStateSummary`

#### REST Adapter
- [ ] Crear `QuoteStateResponse` — `folioNumber`, `quoteStatus`, `completionPercentage`, `sections` (mapa o `SectionsResponse`), `version`, `updatedAt`
- [ ] Crear `SectionsResponse` — cinco campos String (valores del enum)
- [ ] Crear `QuoteStateRestMapper` — `QuoteState → QuoteStateResponse`
- [ ] Crear `QuoteStateController`:
  - [ ] `GET /v1/quotes/{folioNumber}/state` → llama `GetQuoteStateUseCase.getState()`
  - [ ] Retorna `ResponseEntity<QuoteStateResponse>` con HTTP 200
- [ ] Crear `QuoteStateApi` — `@Tag`, `@Operation`, `@ApiResponse` (200, 404)

#### Configuración
- [ ] Actualizar `FolioConfig.java` — añadir `@Bean GetQuoteStateUseCaseImpl`

### Tests Backend

#### Unitarios (TDD — escribir ANTES del código de producción)
- [ ] `GetQuoteStateUseCaseImplTest`:
  - [ ] `getState_withLayoutCompleteAndOneIncompleteLocation_returnsPartialProgress` (CRITERIO-1.1)
  - [ ] `getState_withFreshFolio_returnsZeroPercentage` (CRITERIO-1.2)
  - [ ] `getState_withCalculatedFolio_returns100Percent` (CRITERIO-1.3)
  - [ ] `getState_folioNotFound_throwsFolioNotFoundException` (CRITERIO-1.4)
  - [ ] `getState_whenGeneralInfoFieldsNull_returnsGeneralInfoPending` (CRITERIO-1.5)
- [ ] `LocationStateJpaAdapterTest`:
  - [ ] `readByFolioNumber_withNoLocations_returnsTotalZero`
  - [ ] `readByFolioNumber_withMixedLocations_returnsCorrectCounts`
- [ ] `QuoteStateControllerTest`:
  - [ ] `getState_returns200WithCorrectBody`
  - [ ] `getState_unknownFolio_returns404`
- [ ] `QuoteStateRestMapperTest`:
  - [ ] `toResponse_mapsAllFieldsCorrectly`

### QA
- [ ] Ejecutar skill `/gherkin-case-generator quote-state` → generar escenarios CRITERIO-1.1..1.5
- [ ] Ejecutar skill `/risk-identifier quote-state` → clasificación ASD de riesgos
- [ ] Validar endpoint en vivo contra criterios de aceptación
- [ ] Actualizar estado spec: `status: IMPLEMENTED`

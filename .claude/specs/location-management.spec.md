---
id: SPEC-004
status: DRAFT
feature: location-management
created: 2026-04-21
updated: 2026-04-21
author: spec-generator
version: "1.1"
related-specs: ["SPEC-002", "SPEC-003-location-layout"]
---

# Spec: Gestión de Ubicaciones de Cotización

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción

`Insurance-Quoter-Back` expone cuatro endpoints para gestionar las ubicaciones dentro de una cotización: listar todas las ubicaciones con su detalle completo, reemplazar la lista completa, actualizar parcialmente una ubicación específica y obtener un resumen de validación. En cada escritura se recalculan las alertas bloqueantes (`blockingAlerts`) y el estado de validación (`validationStatus`) de cada ubicación afectada, consultando el core service para validar el código postal.

### Requerimiento de Negocio

> - `GET /v1/quotes/{folio}/locations` — lista todas las ubicaciones de la cotización con su detalle completo.
> - `PUT /v1/quotes/{folio}/locations` — reemplaza la lista completa de ubicaciones; recalcula alertas.
> - `PATCH /v1/quotes/{folio}/locations/{index}` — actualización parcial de una ubicación; solo se aplican campos presentes en el body; recalcula alertas.
> - `GET /v1/quotes/{folio}/locations/summary` — resumen de validación: solo `index`, `locationName`, `validationStatus` y `blockingAlerts`.
> - Validar `zipCode` contra el core service (`GET /v1/zip-codes/{zipCode}`). Si inválido o ausente → alerta `MISSING_ZIP_CODE` + `validationStatus = INCOMPLETE`.
> - Sin `businessLine.fireKey` → alerta `MISSING_FIRE_KEY`.
> - Sin al menos una guarantee con `tarifable = true` → alerta `NO_TARIFABLE_GUARANTEES`.
> - `blockingAlerts` se recalcula en cada escritura (PUT y PATCH).
> - Entidades JPA: `LocationJpa` (tabla `locations`), alertas via `@ElementCollection` en `LocationJpa`.

### Historias de Usuario

#### HU-01: Consultar todas las ubicaciones de una cotización

```
Como:        agente de seguros
Quiero:      llamar GET /v1/quotes/{folio}/locations
Para:        visualizar el detalle completo de todas las ubicaciones registradas

Prioridad:   Alta
Estimación:  S
Dependencias: SPEC-002 (Quote existe en la tabla quotes)
Capa:        Backend
```

#### Criterios de Aceptación — HU-01

**Happy Path**
```gherkin
CRITERIO-1.1: Listado exitoso de ubicaciones
  Dado que:  existe un folio "FOL-2026-00042" con dos ubicaciones registradas
  Cuando:    se realiza GET /v1/quotes/FOL-2026-00042/locations
  Entonces:  la respuesta tiene HTTP 200
             Y el body contiene "folioNumber": "FOL-2026-00042"
             Y "locations" es un array con los datos completos de ambas ubicaciones
             Y cada ubicación incluye index, locationName, address, zipCode, state, municipality,
               neighborhood, city, constructionType, level, constructionYear, businessLine,
               guarantees, catastrophicZone, validationStatus y blockingAlerts
             Y "version" refleja la versión actual de la cotización
```

**Error Path**
```gherkin
CRITERIO-1.2: Folio inexistente
  Dado que:  el folio "FOL-9999-00001" no existe en la base de datos
  Cuando:    se realiza GET /v1/quotes/FOL-9999-00001/locations
  Entonces:  la respuesta tiene HTTP 404
             Y el body es {"error": "Folio not found", "code": "FOLIO_NOT_FOUND"}
```

**Edge Case**
```gherkin
CRITERIO-1.3: Cotización sin ubicaciones registradas
  Dado que:  existe un folio "FOL-2026-00042" sin ubicaciones registradas
  Cuando:    se realiza GET /v1/quotes/FOL-2026-00042/locations
  Entonces:  la respuesta tiene HTTP 200
             Y "locations" es un array vacío []
```

---

#### HU-02: Reemplazar la lista completa de ubicaciones

```
Como:        agente de seguros
Quiero:      llamar PUT /v1/quotes/{folio}/locations con la lista completa
Para:        sobreescribir todas las ubicaciones y recalcular alertas bloqueantes

Prioridad:   Alta
Estimación:  M
Dependencias: HU-01, SPEC-002
Capa:        Backend
```

#### Criterios de Aceptación — HU-02

**Happy Path — todas las ubicaciones completas**
```gherkin
CRITERIO-2.1: Reemplazo exitoso sin alertas
  Dado que:  existe el folio "FOL-2026-00042" con version 4
             Y cada ubicación enviada tiene zipCode válido, businessLine con fireKey y al menos una guarantee tarifable
  Cuando:    se realiza PUT /v1/quotes/FOL-2026-00042/locations con body válido y version 4
  Entonces:  la respuesta tiene HTTP 200
             Y "locations" refleja la nueva lista con validationStatus COMPLETE y blockingAlerts vacíos
             Y "version" es 5
             Y "updatedAt" es un timestamp ISO-8601 UTC
```

**Happy Path — ubicación con zipCode inválido**
```gherkin
CRITERIO-2.2: Reemplazo con zipCode inválido genera alerta
  Dado que:  existe el folio "FOL-2026-00042" con version 4
             Y una ubicación tiene zipCode "00000" que el core service no reconoce
  Cuando:    se realiza PUT /v1/quotes/FOL-2026-00042/locations con esa ubicación
  Entonces:  la respuesta tiene HTTP 200
             Y esa ubicación tiene validationStatus INCOMPLETE
             Y blockingAlerts contiene {"code": "MISSING_ZIP_CODE", "message": "Código postal requerido"}
```

**Happy Path — sin fireKey**
```gherkin
CRITERIO-2.3: Reemplazo con businessLine sin fireKey genera alerta
  Dado que:  una ubicación no tiene businessLine.fireKey
  Cuando:    se realiza PUT /v1/quotes/FOL-2026-00042/locations con esa ubicación
  Entonces:  esa ubicación tiene blockingAlerts con {"code": "MISSING_FIRE_KEY", "message": "Clave incendio requerida"}
             Y validationStatus INCOMPLETE
```

**Happy Path — sin guarantees tarifables**
```gherkin
CRITERIO-2.4: Reemplazo sin guarantees tarifables genera alerta
  Dado que:  una ubicación no tiene guarantees o ninguna es tarifable
  Cuando:    se realiza PUT /v1/quotes/FOL-2026-00042/locations con esa ubicación
  Entonces:  esa ubicación tiene blockingAlerts con {"code": "NO_TARIFABLE_GUARANTEES", "message": "Sin garantías tarifables"}
             Y validationStatus INCOMPLETE
```

**Error Path — version conflict**
```gherkin
CRITERIO-2.5: Conflicto de versión optimista
  Dado que:  la version actual del folio es 5
  Cuando:    se realiza PUT /v1/quotes/FOL-2026-00042/locations con version 4
  Entonces:  la respuesta tiene HTTP 409
             Y el body es {"error": "Optimistic lock conflict", "code": "VERSION_CONFLICT"}
```

**Error Path — folio inexistente**
```gherkin
CRITERIO-2.6: Folio inexistente en PUT
  Dado que:  el folio "FOL-9999-00001" no existe
  Cuando:    se realiza PUT /v1/quotes/FOL-9999-00001/locations con cualquier body
  Entonces:  la respuesta tiene HTTP 404
             Y el body contiene "code": "FOLIO_NOT_FOUND"
```

---

#### HU-03: Actualización parcial de una ubicación

```
Como:        agente de seguros
Quiero:      llamar PATCH /v1/quotes/{folio}/locations/{index} con solo los campos que cambian
Para:        actualizar una ubicación sin sobreescribir los campos no enviados

Prioridad:   Alta
Estimación:  M
Dependencias: HU-02, SPEC-002
Capa:        Backend
```

#### Criterios de Aceptación — HU-03

**Happy Path — actualización parcial exitosa**
```gherkin
CRITERIO-3.1: PATCH aplica solo los campos enviados
  Dado que:  existe la ubicación con index 1 en el folio "FOL-2026-00042" con version 5
             Y la ubicación tiene locationName "Bodega Norte", zipCode "06600" y constructionYear 1995
  Cuando:    se realiza PATCH /v1/quotes/FOL-2026-00042/locations/1 con body {"locationName": "Almacén Central", "version": 5}
  Entonces:  la respuesta tiene HTTP 200
             Y "location.locationName" es "Almacén Central"
             Y "location.zipCode" sigue siendo "06600" (no modificado)
             Y "location.constructionYear" sigue siendo 1995 (no modificado)
             Y "version" es 6
             Y blockingAlerts se recalcula con el nuevo estado
```

**Happy Path — PATCH que corrige alerta**
```gherkin
CRITERIO-3.2: PATCH con zipCode válido elimina alerta MISSING_ZIP_CODE
  Dado que:  la ubicación index 2 tiene blockingAlerts con MISSING_ZIP_CODE
  Cuando:    se realiza PATCH /v1/quotes/FOL-2026-00042/locations/2 con {"zipCode": "06600", "version": 5}
             Y el core service confirma que "06600" es válido
  Entonces:  la respuesta tiene HTTP 200
             Y "location.validationStatus" es COMPLETE (si no hay otras alertas)
             Y blockingAlerts no contiene MISSING_ZIP_CODE
```

**Error Path — índice inexistente**
```gherkin
CRITERIO-3.3: Índice de ubicación no existe
  Dado que:  la cotización tiene 2 ubicaciones (índices 1 y 2)
  Cuando:    se realiza PATCH /v1/quotes/FOL-2026-00042/locations/5
  Entonces:  la respuesta tiene HTTP 404
             Y el body es {"error": "Location index not found", "code": "LOCATION_NOT_FOUND"}
```

**Error Path — version conflict en PATCH**
```gherkin
CRITERIO-3.4: Conflicto de versión en PATCH
  Dado que:  la version actual del folio es 6
  Cuando:    se realiza PATCH /v1/quotes/FOL-2026-00042/locations/1 con version 4
  Entonces:  la respuesta tiene HTTP 409
             Y el body contiene "code": "VERSION_CONFLICT"
```

---

#### HU-04: Resumen de validación de ubicaciones

```
Como:        agente de seguros
Quiero:      llamar GET /v1/quotes/{folio}/locations/summary
Para:        visualizar rápidamente cuáles ubicaciones tienen alertas sin cargar el detalle completo

Prioridad:   Media
Estimación:  S
Dependencias: HU-01
Capa:        Backend
```

#### Criterios de Aceptación — HU-04

**Happy Path**
```gherkin
CRITERIO-4.1: Resumen exitoso con ubicaciones mixtas
  Dado que:  el folio "FOL-2026-00042" tiene 3 ubicaciones: 2 COMPLETE y 1 INCOMPLETE
  Cuando:    se realiza GET /v1/quotes/FOL-2026-00042/locations/summary
  Entonces:  la respuesta tiene HTTP 200
             Y "totalLocations" es 3
             Y "completeLocations" es 2
             Y "incompleteLocations" es 1
             Y "locations" contiene solo index, locationName, validationStatus y blockingAlerts
             Y NO contiene campos como address, zipCode, constructionType, etc.
```

**Error Path**
```gherkin
CRITERIO-4.2: Folio inexistente en summary
  Dado que:  el folio "FOL-9999-00001" no existe
  Cuando:    se realiza GET /v1/quotes/FOL-9999-00001/locations/summary
  Entonces:  la respuesta tiene HTTP 404
             Y el body contiene "code": "FOLIO_NOT_FOUND"
```

### Reglas de Negocio

1. **PATCH parcial:** solo se actualizan los campos explícitamente presentes en el body JSON. Campos ausentes conservan su valor previo.
2. **Recálculo de alertas en escritura:** en cada PUT y PATCH se recalculan todas las `blockingAlerts` de la(s) ubicación(es) afectada(s) desde cero antes de persistir.
3. **Validación de zipCode:** el backend llama `GET /v1/zip-codes/{zipCode}` al core service (puerto 8081). Si la respuesta es 404 o el campo `valid` es `false`, se agrega la alerta `MISSING_ZIP_CODE`.
4. **Alerta MISSING_FIRE_KEY:** se agrega si `businessLine` está ausente o si `businessLine.fireKey` está vacío o nulo.
5. **Alerta NO_TARIFABLE_GUARANTEES:** se agrega si la lista `guarantees` está vacía o si ninguna guarantee consultada al core service tiene `tarifable = true`.
6. **validationStatus:** `COMPLETE` si `blockingAlerts` está vacío; `INCOMPLETE` si contiene al menos una alerta.
7. **Versionado optimista:** el campo `version` en el request debe coincidir con el valor almacenado; si difiere → HTTP 409 `VERSION_CONFLICT`.
8. **Enriquecimiento de zipCode:** al validar un zipCode exitosamente, se llenan automáticamente `state`, `municipality`, `city` y `catastrophicZone` con los datos del core service.
9. **Summary no incluye versión:** `GET /summary` no expone `version` — es una proyección de solo lectura.
10. **Scope del @Version:** el versionado optimista se mantiene en `QuoteJpa` (aggregate raíz), no en `LocationJpa`. Cada escritura de ubicaciones incrementa la versión de la `Quote`.

---

## 2. DISEÑO

### Modelos de Datos

#### Estado actual del código (post feature/location-layout)

> **IMPORTANTE:** La tabla `locations` y la entidad `LocationJpa` ya existen desde el feature `location-layout` (V4). Esta spec los **extiende**, no los recrea.

| Artefacto | Estado | Acción requerida |
|-----------|--------|-----------------|
| `LocationJpa` | existe — solo `id, quote_id, index, active, location_name, created_at, updated_at` | agregar columnas de detalle |
| `LocationRepository` (port out) | existe — métodos de layout (`findActiveByQuoteId`, `insertAll`, etc.) | agregar métodos de gestión |
| `Location` (domain model) | existe — solo `record Location(int index, boolean active)` | extender con campos completos |
| tabla `locations` | existe con 7 columnas básicas (V4) | migración V5 agrega columnas |
| tabla `location_blocking_alerts` | no existe | migración V6 la crea |
| `LocationJpaAdapter` | existe — implementa métodos de layout | agregar implementación de nuevos métodos |

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `LocationJpa` | tabla `locations` | **modificada** — agregar columnas de detalle | Ubicación con datos completos |
| `BlockingAlertEmbeddable` | tabla `location_blocking_alerts` | **nueva** — `@ElementCollection` en `LocationJpa` | Alerta bloqueante (code + message) |
| `Location` | domain model | **modificada** — extender el record existente | Modelo de dominio completo |

#### Columnas a AGREGAR a `locations` (migración V5 — ALTER TABLE)

> Columnas ya existentes: `id, quote_id, index, active, location_name, created_at, updated_at`

| Columna nueva | Tipo SQL | Obligatorio | Descripción |
|---------------|----------|-------------|-------------|
| `address` | `VARCHAR(500)` | no | Dirección completa |
| `zip_code` | `VARCHAR(10)` | no | Código postal (validado contra core) |
| `state` | `VARCHAR(100)` | no | Estado (enriquecido desde core) |
| `municipality` | `VARCHAR(100)` | no | Municipio (enriquecido desde core) |
| `neighborhood` | `VARCHAR(100)` | no | Colonia seleccionada |
| `city` | `VARCHAR(100)` | no | Ciudad (enriquecido desde core) |
| `construction_type` | `VARCHAR(50)` | no | Tipo constructivo (ej. MASONRY) |
| `level` | `INT` | no | Nivel o piso |
| `construction_year` | `INT` | no | Año de construcción |
| `business_line_code` | `VARCHAR(50)` | no | Código del giro |
| `business_line_fire_key` | `VARCHAR(50)` | no | Clave incendio del giro |
| `business_line_description` | `VARCHAR(255)` | no | Descripción del giro |
| `guarantees` | `JSONB` | no | Array JSON de garantías [{code, insuredValue}] |
| `catastrophic_zone` | `VARCHAR(50)` | no | Zona catastrófica (enriquecido desde core) |
| `validation_status` | `VARCHAR(20)` | sí | COMPLETE o INCOMPLETE — DEFAULT 'INCOMPLETE' |

#### Campos de la tabla `location_blocking_alerts` (migración V6 — nueva)

| Columna | Tipo SQL | Obligatorio | Constraint | Descripción |
|---------|----------|-------------|------------|-------------|
| `location_id` | `BIGINT` | sí | FK → locations.id ON DELETE CASCADE | Ubicación propietaria |
| `alert_code` | `VARCHAR(50)` | sí | NOT NULL | Código de la alerta |
| `alert_message` | `VARCHAR(255)` | sí | NOT NULL | Mensaje legible |

#### Índices / Constraints

- `IDX_locations_quote_id` ya existe (V4)
- `UK_locations_quote_index` ya existe (V4)
- `idx_location_alerts_location_id` — nuevo en V6

#### Migraciones Flyway

```sql
-- V5__add_location_detail_columns.sql
ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS address                  VARCHAR(500),
    ADD COLUMN IF NOT EXISTS zip_code                 VARCHAR(10),
    ADD COLUMN IF NOT EXISTS state                    VARCHAR(100),
    ADD COLUMN IF NOT EXISTS municipality             VARCHAR(100),
    ADD COLUMN IF NOT EXISTS neighborhood             VARCHAR(100),
    ADD COLUMN IF NOT EXISTS city                     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS construction_type        VARCHAR(50),
    ADD COLUMN IF NOT EXISTS level                    INT,
    ADD COLUMN IF NOT EXISTS construction_year        INT,
    ADD COLUMN IF NOT EXISTS business_line_code       VARCHAR(50),
    ADD COLUMN IF NOT EXISTS business_line_fire_key   VARCHAR(50),
    ADD COLUMN IF NOT EXISTS business_line_description VARCHAR(255),
    ADD COLUMN IF NOT EXISTS guarantees               JSONB,
    ADD COLUMN IF NOT EXISTS catastrophic_zone        VARCHAR(50),
    ADD COLUMN IF NOT EXISTS validation_status        VARCHAR(20) NOT NULL DEFAULT 'INCOMPLETE';

-- V6__create_location_blocking_alerts_table.sql
CREATE TABLE IF NOT EXISTS location_blocking_alerts (
    location_id     BIGINT       NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    alert_code      VARCHAR(50)  NOT NULL,
    alert_message   VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_location_alerts_location_id ON location_blocking_alerts (location_id);
```

#### Domain Models

> `Location.java` ya existe como `record Location(int index, boolean active)`. Se reemplaza por versión extendida.

```java
// location/domain/model/Location.java — REEMPLAZA el record existente
public record Location(
    int index,
    boolean active,
    String locationName,
    String address,
    String zipCode,
    String state,
    String municipality,
    String neighborhood,
    String city,
    String constructionType,
    Integer level,
    Integer constructionYear,
    BusinessLine businessLine,
    List<Guarantee> guarantees,
    String catastrophicZone,
    ValidationStatus validationStatus,
    List<BlockingAlert> blockingAlerts
) {}

// location/domain/model/BusinessLine.java — nueva
public record BusinessLine(String code, String fireKey, String description) {}

// location/domain/model/Guarantee.java — nueva
public record Guarantee(String code, BigDecimal insuredValue) {}

// location/domain/model/BlockingAlert.java — nueva
public record BlockingAlert(String code, String message) {}

// location/domain/model/ValidationStatus.java — nueva
public enum ValidationStatus { COMPLETE, INCOMPLETE }

// location/domain/model/BlockingAlertCode.java — nueva
public enum BlockingAlertCode {
    MISSING_ZIP_CODE,
    MISSING_FIRE_KEY,
    NO_TARIFABLE_GUARANTEES
}
```

### API Endpoints

#### GET /v1/quotes/{folio}/locations

- **Descripción:** Lista todas las ubicaciones con detalle completo
- **Auth requerida:** no
- **Path param:** `folio` — número de folio (ej. `FOL-2026-00042`)
- **Response 200:**
  ```json
  {
    "folioNumber": "FOL-2026-00042",
    "locations": [
      {
        "index": 1,
        "locationName": "Bodega Principal",
        "address": "Av. Insurgentes 1000",
        "zipCode": "06600",
        "state": "Ciudad de México",
        "municipality": "Cuauhtémoc",
        "neighborhood": "Juárez",
        "city": "Ciudad de México",
        "constructionType": "MASONRY",
        "level": 2,
        "constructionYear": 1995,
        "businessLine": {
          "code": "BL-001",
          "fireKey": "FK-INC-01",
          "description": "Bodega de mercancías"
        },
        "guarantees": [
          { "code": "GUA-FIRE", "insuredValue": 5000000 }
        ],
        "catastrophicZone": "ZONE_A",
        "validationStatus": "COMPLETE",
        "blockingAlerts": []
      }
    ],
    "version": 4
  }
  ```
- **Response 404:** `{"error": "Folio not found", "code": "FOLIO_NOT_FOUND"}`

---

#### PUT /v1/quotes/{folio}/locations

- **Descripción:** Reemplaza la lista completa de ubicaciones y recalcula alertas
- **Auth requerida:** no
- **Request Body:**
  ```json
  {
    "locations": [
      {
        "index": 1,
        "locationName": "Bodega Principal",
        "address": "Av. Insurgentes 1000",
        "zipCode": "06600",
        "constructionType": "MASONRY",
        "level": 2,
        "constructionYear": 1995,
        "businessLine": { "code": "BL-001", "fireKey": "FK-INC-01" },
        "guarantees": [{ "code": "GUA-FIRE", "insuredValue": 5000000 }]
      }
    ],
    "version": 4
  }
  ```
- **Response 200:**
  ```json
  {
    "folioNumber": "FOL-2026-00042",
    "locations": [ { "...detalle completo con validationStatus y blockingAlerts..." } ],
    "updatedAt": "2026-04-21T15:30:00Z",
    "version": 5
  }
  ```
- **Response 404:** `{"error": "Folio not found", "code": "FOLIO_NOT_FOUND"}`
- **Response 409:** `{"error": "Optimistic lock conflict", "code": "VERSION_CONFLICT"}`
- **Response 422:** `{"error": "Validation failed", "code": "VALIDATION_ERROR", "fields": [...]}`

---

#### PATCH /v1/quotes/{folio}/locations/{index}

- **Descripción:** Actualización parcial — solo campos presentes en el body
- **Auth requerida:** no
- **Path params:** `folio`, `index` (entero 1-based)
- **Request Body** (todos los campos son opcionales salvo `version`):
  ```json
  {
    "locationName": "Bodega Norte",
    "zipCode": "44100",
    "guarantees": [{ "code": "GUA-FIRE", "insuredValue": 7000000 }],
    "version": 5
  }
  ```
- **Response 200:**
  ```json
  {
    "folioNumber": "FOL-2026-00042",
    "location": {
      "index": 1,
      "locationName": "Bodega Norte",
      "zipCode": "44100",
      "validationStatus": "COMPLETE",
      "blockingAlerts": [],
      "...resto de campos sin modificar..."
    },
    "updatedAt": "2026-04-21T15:35:00Z",
    "version": 6
  }
  ```
- **Response 404 (folio):** `{"error": "Folio not found", "code": "FOLIO_NOT_FOUND"}`
- **Response 404 (location):** `{"error": "Location index not found", "code": "LOCATION_NOT_FOUND"}`
- **Response 409:** `{"error": "Optimistic lock conflict", "code": "VERSION_CONFLICT"}`

---

#### GET /v1/quotes/{folio}/locations/summary

- **Descripción:** Resumen de validación — proyección reducida sin detalle de campos
- **Auth requerida:** no
- **Response 200:**
  ```json
  {
    "folioNumber": "FOL-2026-00042",
    "totalLocations": 3,
    "completeLocations": 2,
    "incompleteLocations": 1,
    "locations": [
      {
        "index": 1,
        "locationName": "Bodega Principal",
        "validationStatus": "COMPLETE",
        "blockingAlerts": []
      },
      {
        "index": 2,
        "locationName": "Oficina Sur",
        "validationStatus": "INCOMPLETE",
        "blockingAlerts": [
          { "code": "MISSING_ZIP_CODE", "message": "Código postal requerido" },
          { "code": "MISSING_FIRE_KEY", "message": "Clave incendio requerida" }
        ]
      }
    ]
  }
  ```
- **Response 404:** `{"error": "Folio not found", "code": "FOLIO_NOT_FOUND"}`

---

### Arquitectura Hexagonal — Descomposición de Componentes

#### Contexto: `location`

```
com.sofka.insurancequoter.back.location/
├── domain/
│   ├── model/
│   │   ├── Location.java                              ← Record (todos los campos)
│   │   ├── BusinessLine.java                          ← Record (code, fireKey, description)
│   │   ├── Guarantee.java                             ← Record (code, insuredValue)
│   │   ├── BlockingAlert.java                         ← Record (code, message)
│   │   ├── BlockingAlertCode.java                     ← Enum: MISSING_ZIP_CODE, MISSING_FIRE_KEY, NO_TARIFABLE_GUARANTEES
│   │   └── ValidationStatus.java                     ← Enum: COMPLETE, INCOMPLETE
│   ├── service/
│   │   └── LocationValidationService.java            ← Domain service: recalcula blockingAlerts dado los datos + result de zipCode
│   └── port/
│       ├── in/
│       │   ├── GetLocationsUseCase.java               ← Input Port: LocationsResponse getLocations(String folio)
│       │   ├── ReplaceLocationsUseCase.java           ← Input Port: LocationsResponse replaceLocations(ReplaceLocationsCommand)
│       │   ├── PatchLocationUseCase.java              ← Input Port: LocationPatchResponse patchLocation(PatchLocationCommand)
│       │   └── GetLocationsSummaryUseCase.java        ← Input Port: LocationsSummaryResponse getSummary(String folio)
│       └── out/
│           ├── LocationRepository.java               ← Output Port: findByFolio / replaceAll / patchOne / findSummaryByFolio
│           ├── QuoteVersionRepository.java           ← Output Port: findVersionByFolio / incrementVersion
│           └── ZipCodeValidationClient.java          ← Output Port: ZipCodeInfo validate(String zipCode)
├── application/
│   └── usecase/
│       ├── GetLocationsUseCaseImpl.java
│       ├── ReplaceLocationsUseCaseImpl.java          ← Orquesta: validar versión → validar cada ubicación → recalcular alertas → persistir
│       ├── PatchLocationUseCaseImpl.java             ← Orquesta: buscar ubicación → merge parcial → validar → recalcular alertas → persistir
│       ├── GetLocationsSummaryUseCaseImpl.java
│       ├── command/
│       │   ├── ReplaceLocationsCommand.java          ← Record: folio, locations, version
│       │   └── PatchLocationCommand.java             ← Record: folio, index, patch fields (todos Optional<>), version
│       └── dto/
│           ├── LocationsResponse.java
│           ├── LocationPatchResponse.java
│           └── LocationsSummaryResponse.java
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   └── rest/
    │   │       ├── LocationController.java            ← implementa LocationApi
    │   │       ├── swaggerdocs/
    │   │       │   └── LocationApi.java               ← @Tag Locations; @Operation por endpoint
    │   │       ├── dto/
    │   │       │   ├── request/
    │   │       │   │   ├── ReplaceLocationsRequest.java
    │   │       │   │   ├── LocationItemRequest.java
    │   │       │   │   ├── BusinessLineRequest.java
    │   │       │   │   ├── GuaranteeRequest.java
    │   │       │   │   └── PatchLocationRequest.java  ← todos los campos son Optional<> excepto version
    │   │       │   └── response/
    │   │       │       ├── LocationsListResponse.java
    │   │       │       ├── LocationDetailResponse.java
    │   │       │       ├── LocationSummaryItemResponse.java
    │   │       │       ├── BlockingAlertResponse.java
    │   │       │       └── LocationsSummaryWrapperResponse.java
    │   │       └── mapper/
    │   │           └── LocationRestMapper.java
    │   └── out/
    │       ├── persistence/
    │       │   ├── adapter/
    │       │   │   └── LocationJpaAdapter.java        ← implementa LocationRepository
    │       │   ├── repositories/
    │       │   │   └── LocationJpaRepository.java     ← extiende JpaRepository<LocationJpa, Long>
    │       │   ├── entities/
    │       │   │   ├── LocationJpa.java               ← @Entity tabla locations; @ManyToOne QuoteJpa
    │       │   │   └── BlockingAlertEmbeddable.java   ← @Embeddable para @ElementCollection
    │       │   └── mappers/
    │       │       └── LocationPersistenceMapper.java ← Location ↔ LocationJpa
    │       └── http/
    │           ├── adapter/
    │           │   └── ZipCodeValidationClientAdapter.java ← implementa ZipCodeValidationClient; llama GET /v1/zip-codes/{zipCode} al core
    │           └── dto/
    │               └── ZipCodeResponse.java           ← {zipCode, state, municipality, city, catastrophicZone, valid}
    └── config/
        └── LocationConfig.java                        ← @Bean wiring de los 4 use cases + RestClient para core
```

#### Contratos de interfaces clave

```java
// domain/port/in/GetLocationsUseCase.java
public interface GetLocationsUseCase {
    LocationsResponse getLocations(String folioNumber);
}

// domain/port/in/ReplaceLocationsUseCase.java
public interface ReplaceLocationsUseCase {
    LocationsResponse replaceLocations(ReplaceLocationsCommand command);
}

// domain/port/in/PatchLocationUseCase.java
public interface PatchLocationUseCase {
    LocationPatchResponse patchLocation(PatchLocationCommand command);
}

// domain/port/in/GetLocationsSummaryUseCase.java
public interface GetLocationsSummaryUseCase {
    LocationsSummaryResponse getSummary(String folioNumber);
}

// domain/port/out/LocationRepository.java
public interface LocationRepository {
    List<Location> findByFolioNumber(String folioNumber);
    List<Location> replaceAll(String folioNumber, List<Location> locations);
    Location patchOne(String folioNumber, int index, Location mergedLocation);
    List<LocationSummary> findSummaryByFolioNumber(String folioNumber);
    boolean existsByFolioAndIndex(String folioNumber, int index);
}

// domain/port/out/ZipCodeValidationClient.java
public interface ZipCodeValidationClient {
    Optional<ZipCodeInfo> validate(String zipCode);  // empty si 404 o invalid
}

// domain/service/LocationValidationService.java
public class LocationValidationService {
    public List<BlockingAlert> calculateAlerts(Location location, Optional<ZipCodeInfo> zipCodeInfo) {
        // Returns list of blocking alerts based on validation rules
    }
    public ValidationStatus deriveValidationStatus(List<BlockingAlert> alerts) {
        return alerts.isEmpty() ? ValidationStatus.COMPLETE : ValidationStatus.INCOMPLETE;
    }
}
```

#### Flujo de llamada — PUT (reemplazo)

```
LocationController.replaceLocations(folio, request)
  → ReplaceLocationsUseCase.replaceLocations(command)
      1. QuoteVersionRepository.findVersionByFolio(folio) → verificar version == command.version; si no → VersionConflictException
      2. Para cada location en command.locations:
         a. ZipCodeValidationClient.validate(location.zipCode)         ← llamada al core si zipCode presente
         b. LocationValidationService.calculateAlerts(location, zipInfo)← calcula MISSING_ZIP_CODE, MISSING_FIRE_KEY, NO_TARIFABLE_GUARANTEES
         c. location.withValidationStatus(derived) y location.withBlockingAlerts(alerts)
      3. LocationRepository.replaceAll(folio, enrichedLocations)
      4. QuoteVersionRepository.incrementVersion(folio)
      5. return LocationsResponse
  ← ResponseEntity<LocationsListResponse>(200)
```

#### Manejo de excepciones

| Excepción de dominio | HTTP | Código |
|---------------------|------|--------|
| `FolioNotFoundException` | 404 | `FOLIO_NOT_FOUND` |
| `LocationNotFoundException` | 404 | `LOCATION_NOT_FOUND` |
| `VersionConflictException` | 409 | `VERSION_CONFLICT` |
| `MethodArgumentNotValidException` | 422 | `VALIDATION_ERROR` |

> `GlobalExceptionHandler` (ya existente de SPEC-002) debe agregar handlers para `LocationNotFoundException` y `VersionConflictException`.

### Notas de Implementación

- **PATCH con `Optional<>`:** `PatchLocationRequest` usa `Optional<String>`, `Optional<Integer>`, etc. para distinguir campo ausente (no enviar) de campo nulo (borrar). Alternativa: usar `JsonNullable` de OpenAPI Generator o un `Map<String, Object>`. El enfoque con `Optional<>` + Jackson `@JsonInclude(NON_NULL)` es suficiente para este caso.
- **`guarantees` como JSONB:** la serialización/deserialización de la lista de garantías como JSONB en PostgreSQL requiere un `@Type(JsonType.class)` de Hypersistence (o un `@Convert` con `AttributeConverter<List<Guarantee>, String>`). Evaluar si ya existe la dependencia en el proyecto.
- **`@ElementCollection` para blockingAlerts:** `BlockingAlertEmbeddable` con `@CollectionTable(name = "location_blocking_alerts")`. Hibernate borra y reinserta toda la colección en cada save — es el comportamiento esperado dado que las alertas se recalculan siempre desde cero.
- **Orden de routing:** Spring MVC puede confundir `GET /v1/quotes/{folio}/locations/summary` con `GET /v1/quotes/{folio}/locations/{index}` si ambos están en el mismo controller. Usar `@GetMapping("/summary")` antes que `@GetMapping("/{index}")`, o asegurar que `summary` no sea un entero válido para `{index}`.
- **`QuoteVersionRepository`:** reutilizar el `QuoteJpaRepository` ya existente de SPEC-002 para leer/actualizar el campo `version` de `quotes`. No crear un nuevo repositorio JPA; exponer un método en el output port o compartir el `QuoteRepository` existente.
- **`ZipCodeValidationClientAdapter`:** debe manejar el caso en que el core service retorne 404 como `Optional.empty()`, sin lanzar excepción — la alerta la pone `LocationValidationService`.
- **Migración Flyway:** las migraciones V3 y V4 deben ir en `src/main/resources/db/migration/`. Verificar que V1 y V2 ya existan por SPEC-001/SPEC-002 antes de nombrar.

---

## 3. LISTA DE TAREAS

> Checklist accionable. Marcar cada ítem (`[x]`) al completarlo.

### Backend

#### Base de Datos
- [ ] Crear migración `V5__add_location_detail_columns.sql` — `ALTER TABLE locations` agrega las 15 columnas de detalle listadas en el diseño
- [ ] Crear migración `V6__create_location_blocking_alerts_table.sql` — tabla `location_blocking_alerts`, FK `locations.id` ON DELETE CASCADE, índice `idx_location_alerts_location_id`

#### Dominio
- [ ] **Reemplazar** `Location.java` — extender el record existente (`int index, boolean active`) con todos los campos de detalle listados en el diseño
- [ ] Crear records `BusinessLine`, `Guarantee`, `BlockingAlert` en `location/domain/model/` (nuevos)
- [ ] Crear enums `ValidationStatus`, `BlockingAlertCode` en `location/domain/model/` (nuevos)
- [ ] Crear `LocationValidationService` en `location/domain/service/` — método `calculateAlerts(Location, Optional<ZipCodeInfo>)`
- [ ] Crear Input Port `GetLocationsUseCase` en `location/domain/port/in/`
- [ ] Crear Input Port `ReplaceLocationsUseCase` en `location/domain/port/in/`
- [ ] Crear Input Port `PatchLocationUseCase` en `location/domain/port/in/`
- [ ] Crear Input Port `GetLocationsSummaryUseCase` en `location/domain/port/in/`
- [ ] **Extender** `LocationRepository` (port out existente) — agregar métodos: `findByFolioNumber`, `replaceAll`, `patchOne`, `findSummaryByFolioNumber`, `existsByFolioAndIndex`
- [ ] Crear Output Port `ZipCodeValidationClient` en `location/domain/port/out/` (nuevo)

#### Aplicación
- [ ] Crear record `ReplaceLocationsCommand` en `location/application/usecase/command/`
- [ ] Crear record `PatchLocationCommand` en `location/application/usecase/command/` — campos con `Optional<>`
- [ ] Crear `GetLocationsUseCaseImpl` en `location/application/usecase/`
- [ ] Crear `ReplaceLocationsUseCaseImpl` en `location/application/usecase/` — validar versión → validar zip → calcular alertas → persistir
- [ ] Crear `PatchLocationUseCaseImpl` en `location/application/usecase/` — buscar → merge → validar → calcular alertas → persistir
- [ ] Crear `GetLocationsSummaryUseCaseImpl` en `location/application/usecase/`
- [ ] Crear excepciones `FolioNotFoundException`, `LocationNotFoundException`, `VersionConflictException` (si no existen de SPEC-002)

#### Infraestructura — Persistencia
- [ ] Crear `BlockingAlertEmbeddable` en `location/infrastructure/adapter/out/persistence/entities/` — `@Embeddable` (nuevo)
- [ ] **Extender** `LocationJpa` — agregar los 15 campos de detalle + `@ElementCollection` para `blockingAlerts` usando `BlockingAlertEmbeddable`
- [ ] **Extender** `LocationJpaRepository` — agregar métodos necesarios para los nuevos casos de uso (ej. `findByQuoteId` con join fetch de alertas)
- [ ] **Extender** `LocationPersistenceMapper` — mapear los nuevos campos de `Location ↔ LocationJpa`
- [ ] **Extender** `LocationJpaAdapter` — implementar los nuevos métodos del port: `findByFolioNumber`, `replaceAll`, `patchOne`, `findSummaryByFolioNumber`, `existsByFolioAndIndex`

#### Infraestructura — HTTP Client
- [ ] Crear `ZipCodeResponse` DTO en `location/infrastructure/adapter/out/http/dto/`
- [ ] Crear `ZipCodeValidationClientAdapter` en `location/infrastructure/adapter/out/http/adapter/` — implementa `ZipCodeValidationClient`; llama `GET /v1/zip-codes/{zipCode}` al core; retorna `Optional.empty()` si 404

#### Infraestructura — REST
- [ ] Crear DTOs de request: `ReplaceLocationsRequest`, `LocationItemRequest`, `BusinessLineRequest`, `GuaranteeRequest`, `PatchLocationRequest` en `location/infrastructure/adapter/in/rest/dto/request/`
- [ ] Crear DTOs de response: `LocationsListResponse`, `LocationDetailResponse`, `LocationSummaryItemResponse`, `BlockingAlertResponse`, `LocationsSummaryWrapperResponse` en `location/infrastructure/adapter/in/rest/dto/response/`
- [ ] Crear `LocationRestMapper` en `location/infrastructure/adapter/in/rest/mapper/`
- [ ] Crear Swagger interface `LocationApi` en `location/infrastructure/adapter/in/rest/swaggerdocs/` — `@Tag`, `@Operation` para los 4 endpoints
- [ ] Crear `LocationController` en `location/infrastructure/adapter/in/rest/` — implementa `LocationApi`; inyecta los 4 use cases y mapper
- [ ] Actualizar `GlobalExceptionHandler` — agregar handlers para `LocationNotFoundException` → 404 `LOCATION_NOT_FOUND`, `VersionConflictException` → 409 `VERSION_CONFLICT`

#### Configuración
- [ ] Crear `LocationConfig` en `location/infrastructure/config/` — `@Bean` wiring de los 4 use cases y `ZipCodeValidationClientAdapter`

#### Tests Backend (TDD — escribir el test ANTES de la implementación)

**LocationValidationService**
- [ ] `LocationValidationServiceTest` — zipCode nulo/ausente → alerta MISSING_ZIP_CODE
- [ ] `LocationValidationServiceTest` — zipCode inválido (Optional.empty del client) → alerta MISSING_ZIP_CODE
- [ ] `LocationValidationServiceTest` — businessLine sin fireKey → alerta MISSING_FIRE_KEY
- [ ] `LocationValidationServiceTest` — businessLine null → alerta MISSING_FIRE_KEY
- [ ] `LocationValidationServiceTest` — sin guarantees → alerta NO_TARIFABLE_GUARANTEES
- [ ] `LocationValidationServiceTest` — todas las guarantees no tarifables → alerta NO_TARIFABLE_GUARANTEES
- [ ] `LocationValidationServiceTest` — datos completos y válidos → sin alertas, validationStatus COMPLETE

**GetLocationsUseCaseImpl**
- [ ] `GetLocationsUseCaseImplTest` — folio existente → retorna lista de ubicaciones
- [ ] `GetLocationsUseCaseImplTest` — folio inexistente → lanza FolioNotFoundException

**ReplaceLocationsUseCaseImpl**
- [ ] `ReplaceLocationsUseCaseImplTest` — version conflict → lanza VersionConflictException sin persistir
- [ ] `ReplaceLocationsUseCaseImplTest` — reemplazo exitoso con zipCode válido → persistido con validationStatus COMPLETE y version incrementada
- [ ] `ReplaceLocationsUseCaseImplTest` — reemplazo con zipCode inválido → MISSING_ZIP_CODE en resultado
- [ ] `ReplaceLocationsUseCaseImplTest` — reemplazo sin fireKey → MISSING_FIRE_KEY en resultado
- [ ] `ReplaceLocationsUseCaseImplTest` — reemplazo sin guarantees tarifables → NO_TARIFABLE_GUARANTEES en resultado
- [ ] `ReplaceLocationsUseCaseImplTest` — múltiples ubicaciones: algunas completas, otras incompletas

**PatchLocationUseCaseImpl**
- [ ] `PatchLocationUseCaseImplTest` — index inexistente → lanza LocationNotFoundException
- [ ] `PatchLocationUseCaseImplTest` — version conflict → lanza VersionConflictException
- [ ] `PatchLocationUseCaseImplTest` — patch parcial: solo locationName → resto de campos sin cambio
- [ ] `PatchLocationUseCaseImplTest` — patch con zipCode válido → alerta MISSING_ZIP_CODE eliminada
- [ ] `PatchLocationUseCaseImplTest` — patch con zipCode inválido → alerta MISSING_ZIP_CODE presente

**GetLocationsSummaryUseCaseImpl**
- [ ] `GetLocationsSummaryUseCaseImplTest` — folio existente → totales correctos y proyección reducida
- [ ] `GetLocationsSummaryUseCaseImplTest` — folio inexistente → lanza FolioNotFoundException

**LocationJpaAdapter**
- [ ] `LocationJpaAdapterTest` — findByFolioNumber retorna lista vacía si no hay ubicaciones
- [ ] `LocationJpaAdapterTest` — replaceAll elimina existentes y persiste nuevas
- [ ] `LocationJpaAdapterTest` — patchOne actualiza solo la ubicación con el index dado

**LocationController**
- [ ] `LocationControllerTest` — GET /v1/quotes/{folio}/locations → HTTP 200 con locations
- [ ] `LocationControllerTest` — GET /v1/quotes/{folio}/locations con folio inexistente → HTTP 404
- [ ] `LocationControllerTest` — PUT /v1/quotes/{folio}/locations → HTTP 200 con version incrementada
- [ ] `LocationControllerTest` — PUT con version conflict → HTTP 409
- [ ] `LocationControllerTest` — PATCH /v1/quotes/{folio}/locations/{index} → HTTP 200 con campo actualizado
- [ ] `LocationControllerTest` — PATCH con index inexistente → HTTP 404 LOCATION_NOT_FOUND
- [ ] `LocationControllerTest` — GET /v1/quotes/{folio}/locations/summary → HTTP 200 solo con campos de resumen

**Tests de integración**
- [ ] `LocationManagementIntegrationTest` — `@SpringBootTest` + Testcontainers PostgreSQL + WireMock para core: PUT luego GET retorna datos persistidos
- [ ] `LocationManagementIntegrationTest` — PUT con zipCode inválido: WireMock retorna 404 para core → alerta MISSING_ZIP_CODE en respuesta
- [ ] `LocationManagementIntegrationTest` — PATCH parcial preserva campos no enviados
- [ ] `LocationManagementIntegrationTest` — GET summary retorna conteos correctos

### QA
- [ ] Ejecutar skill `/gherkin-case-generator` → escenarios para CRITERIO-1.1…4.2
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD (concurrencia en version, core unavailable, JSONB serialization)
- [ ] Validar manualmente con Bruno/Postman: los 4 endpoints
- [ ] Verificar que blockingAlerts se recalcula correctamente en cada escritura
- [ ] Verificar que GET /summary NO retorna campos de detalle (address, zipCode, etc.)
- [ ] Verificar que PATCH no sobreescribe campos no enviados
- [ ] Actualizar estado spec: `status: IMPLEMENTED`

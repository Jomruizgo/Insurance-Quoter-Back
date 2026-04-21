---
id: SPEC-003
status: DRAFT
feature: location-layout
created: 2026-04-21
updated: 2026-04-21
author: spec-generator
version: "1.0"
related-specs: ["SPEC-002"]
---

# Spec: Configuración de Layout de Ubicaciones

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción
Permite consultar y configurar el layout de ubicaciones de una cotización (cuántas ubicaciones y de qué tipo). Al guardar, el sistema sincroniza la colección de `LocationJpa` sin eliminar datos: agrega entradas vacías si `numberOfLocations` aumenta, o marca las excedentes como inactivas si disminuye.

### Requerimiento de Negocio
El usuario necesita definir cuántas ubicaciones (locations) tendrá su cotización y de qué tipo (`SINGLE` / `MULTIPLE`) antes de capturar el detalle de cada una. El sistema debe preservar datos ya ingresados al ajustar la cantidad.

### Historias de Usuario

#### HU-01: Consultar layout de ubicaciones

```
Como:        Agente de seguros
Quiero:      Consultar la configuración de layout de ubicaciones de una cotización
Para:        Saber cuántas ubicaciones hay configuradas y de qué tipo antes de editarlas

Prioridad:   Alta
Estimación:  S
Dependencias: SPEC-002 (folio existente)
Capa:        Backend
```

#### Criterios de Aceptación — HU-01

**Happy Path**
```gherkin
CRITERIO-1.1: Obtener layout de cotización con layout configurado
  Dado que:  existe la cotización con folio "FOL-2026-00042" con layoutConfiguration establecido
  Cuando:    el cliente hace GET /v1/quotes/FOL-2026-00042/locations/layout
  Entonces:  retorna HTTP 200 con folioNumber, layoutConfiguration (numberOfLocations, locationType) y version
```

**Error Path**
```gherkin
CRITERIO-1.2: Folio inexistente retorna 404
  Dado que:  no existe cotización con folio "FOL-9999-99999"
  Cuando:    el cliente hace GET /v1/quotes/FOL-9999-99999/locations/layout
  Entonces:  retorna HTTP 404 con { "error": "Folio not found", "code": "FOLIO_NOT_FOUND" }
```

**Edge Case**
```gherkin
CRITERIO-1.3: Cotización sin layout configurado retorna valores nulos
  Dado que:  existe la cotización "FOL-2026-00042" recién creada sin layoutConfiguration
  Cuando:    el cliente hace GET /v1/quotes/FOL-2026-00042/locations/layout
  Entonces:  retorna HTTP 200 con layoutConfiguration: { numberOfLocations: null, locationType: null }
```

---

#### HU-02: Guardar layout de ubicaciones

```
Como:        Agente de seguros
Quiero:      Definir o actualizar el número y tipo de ubicaciones de una cotización
Para:        Que el sistema prepare la estructura de ubicaciones correspondiente

Prioridad:   Alta
Estimación:  M
Dependencias: HU-01
Capa:        Backend
```

#### Criterios de Aceptación — HU-02

**Happy Path**
```gherkin
CRITERIO-2.1: Guardar layout por primera vez crea las ubicaciones vacías
  Dado que:  existe la cotización "FOL-2026-00042" sin ubicaciones, con version 1
  Cuando:    el cliente hace PUT /v1/quotes/FOL-2026-00042/locations/layout
             con { layoutConfiguration: { numberOfLocations: 3, locationType: "MULTIPLE" }, version: 1 }
  Entonces:  retorna HTTP 200 con layoutConfiguration actualizado y version 2
             Y existen 3 registros en la tabla locations asociados al folio, todos con active: true
```

**Happy Path**
```gherkin
CRITERIO-2.2: Aumentar numberOfLocations agrega ubicaciones vacías
  Dado que:  existe la cotización "FOL-2026-00042" con 2 ubicaciones activas, version 3
  Cuando:    el cliente hace PUT con numberOfLocations: 4, version: 3
  Entonces:  retorna HTTP 200 con version 4
             Y existen 4 ubicaciones activas: las 2 originales con sus datos y 2 nuevas vacías
```

**Happy Path**
```gherkin
CRITERIO-2.3: Reducir numberOfLocations marca excedentes como inactivas
  Dado que:  existe la cotización con 4 ubicaciones activas, version 4
  Cuando:    el cliente hace PUT con numberOfLocations: 2, version: 4
  Entonces:  retorna HTTP 200 con version 5
             Y las ubicaciones con índice 1 y 2 siguen activas
             Y las ubicaciones con índice 3 y 4 quedan con active: false (no se eliminan)
```

**Error Path**
```gherkin
CRITERIO-2.4: Conflicto de versión retorna 409
  Dado que:  existe la cotización con version 3 real
  Cuando:    el cliente envía PUT con version: 2 (desactualizada)
  Entonces:  retorna HTTP 409 con { "error": "Optimistic lock conflict", "code": "VERSION_CONFLICT" }
```

**Error Path**
```gherkin
CRITERIO-2.5: Folio inexistente retorna 404
  Dado que:  no existe la cotización "FOL-9999-99999"
  Cuando:    el cliente hace PUT /v1/quotes/FOL-9999-99999/locations/layout
  Entonces:  retorna HTTP 404 con { "error": "Folio not found", "code": "FOLIO_NOT_FOUND" }
```

**Error Path**
```gherkin
CRITERIO-2.6: numberOfLocations menor a 1 retorna 422
  Dado que:  existe la cotización con version 1
  Cuando:    el cliente envía PUT con numberOfLocations: 0
  Entonces:  retorna HTTP 422 con { "error": "Validation failed", "code": "VALIDATION_ERROR" }
```

### Reglas de Negocio

1. `numberOfLocations` debe ser ≥ 1 y ≤ 50.
2. `locationType` acepta solo los valores `SINGLE` o `MULTIPLE`. Si `locationType` es `SINGLE`, `numberOfLocations` debe ser exactamente 1.
3. Al aumentar `numberOfLocations`: se insertan `(nuevo - actual)` registros `LocationJpa` vacíos con `active: true` e `index` secuencial.
4. Al reducir `numberOfLocations`: las ubicaciones con `index > numberOfLocations` se marcan `active: false`. **Nunca se eliminan filas**.
5. El versionado optimista (`@Version`) aplica sobre `QuoteJpa`. Si hay conflicto, retorna 409.
6. El campo `layout_configuration` se almacena en `QuoteJpa` como columnas separadas (`number_of_locations`, `location_type`), no como JSONB, para facilitar queries futuras.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Tabla | Cambios | Descripción |
|---------|-------|---------|-------------|
| `QuoteJpa` | `quotes` | Agregar columnas `number_of_locations`, `location_type` | Almacena configuración de layout |
| `LocationJpa` | `locations` | **Nueva** | Una fila por ubicación dentro de la cotización |

#### Campos nuevos en QuoteJpa

| Campo Java | Columna DB | Tipo SQL | Obligatorio | Descripción |
|------------|-----------|----------|-------------|-------------|
| `numberOfLocations` | `number_of_locations` | `INTEGER` | no (null = sin configurar) | Cantidad total de ubicaciones |
| `locationType` | `location_type` | `VARCHAR(10)` | no (null = sin configurar) | `SINGLE` o `MULTIPLE` |

#### Entidad LocationJpa (nueva)

| Campo Java | Columna DB | Tipo SQL | Obligatorio | Descripción |
|------------|-----------|----------|-------------|-------------|
| `id` | `id` | `BIGSERIAL PK` | sí | Identificador interno |
| `quoteId` | `quote_id` | `BIGINT FK → quotes.id` | sí | Cotización propietaria |
| `index` | `index` | `INTEGER` | sí | Índice 1-based de la ubicación |
| `active` | `active` | `BOOLEAN` | sí | false = marcada como inactiva al reducir numberOfLocations |
| `locationName` | `location_name` | `VARCHAR(255)` | no | Nombre de la ubicación (llenado en fase posterior) |
| `createdAt` | `created_at` | `TIMESTAMP WITH TIME ZONE` | sí | Auto-generado |
| `updatedAt` | `updated_at` | `TIMESTAMP WITH TIME ZONE` | sí | Auto-actualizado |

#### Índices / Constraints

- `UK_locations_quote_index`: UNIQUE(`quote_id`, `index`) — evita índices duplicados por cotización.
- `IDX_locations_quote_id`: índice en `quote_id` — búsquedas frecuentes de todas las ubicaciones del folio.

#### Migraciones Flyway

- `V3__add_layout_columns_to_quotes.sql` — agrega `number_of_locations` y `location_type` a `quotes`.
- `V4__create_locations_table.sql` — crea tabla `locations` con FK, índices y constraint.

---

### API Endpoints

#### GET /v1/quotes/{folio}/locations/layout

- **Descripción**: Retorna la configuración de layout de ubicaciones de la cotización.
- **Path param**: `folio` — número de folio (ej. `FOL-2026-00042`)
- **Auth requerida**: no
- **Response 200**:
  ```json
  {
    "folioNumber": "FOL-2026-00042",
    "layoutConfiguration": {
      "numberOfLocations": 3,
      "locationType": "MULTIPLE"
    },
    "version": 3
  }
  ```
- **Response 404**: `{ "error": "Folio not found", "code": "FOLIO_NOT_FOUND" }`

#### PUT /v1/quotes/{folio}/locations/layout

- **Descripción**: Define o actualiza el layout de ubicaciones. Sincroniza la colección `LocationJpa`.
- **Path param**: `folio` — número de folio
- **Auth requerida**: no
- **Request Body**:
  ```json
  {
    "layoutConfiguration": {
      "numberOfLocations": 3,
      "locationType": "MULTIPLE"
    },
    "version": 3
  }
  ```
- **Response 200**:
  ```json
  {
    "folioNumber": "FOL-2026-00042",
    "layoutConfiguration": {
      "numberOfLocations": 3,
      "locationType": "MULTIPLE"
    },
    "updatedAt": "2026-04-21T15:20:00Z",
    "version": 4
  }
  ```
- **Response 404**: `{ "error": "Folio not found", "code": "FOLIO_NOT_FOUND" }`
- **Response 409**: `{ "error": "Optimistic lock conflict", "code": "VERSION_CONFLICT" }`
- **Response 422**: `{ "error": "Validation failed", "code": "VALIDATION_ERROR", "fields": [...] }`

---

### Arquitectura y Dependencias

El feature vive en el bounded context `location` dentro de `plataforma-danos-back`. Se crea un nuevo context separado del context `folio` existente para mantener la separación de responsabilidades.

#### Estructura de paquetes nueva

```
com.sofka.insurancequoter.back.location/
├── domain/
│   ├── model/
│   │   ├── LayoutConfiguration.java      ← Value Object (numberOfLocations, locationType)
│   │   ├── Location.java                 ← Domain model (index, active)
│   │   └── LocationType.java             ← Enum: SINGLE, MULTIPLE
│   └── port/
│       ├── in/
│       │   ├── GetLocationLayoutUseCase.java
│       │   └── SaveLocationLayoutUseCase.java
│       └── out/
│           ├── QuoteLayoutRepository.java  ← findByFolio, saveLayout
│           └── LocationRepository.java     ← findByQuoteId, saveAll
├── application/
│   └── usecase/
│       ├── GetLocationLayoutUseCaseImpl.java
│       └── SaveLocationLayoutUseCaseImpl.java
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── LocationLayoutController.java
    │   │   ├── swaggerdocs/LocationLayoutApi.java
    │   │   ├── dto/
    │   │   │   ├── LayoutConfigurationDto.java
    │   │   │   ├── GetLayoutResponse.java
    │   │   │   ├── SaveLayoutRequest.java
    │   │   │   └── SaveLayoutResponse.java
    │   │   └── mapper/LocationLayoutRestMapper.java
    │   └── out/persistence/
    │       ├── adapter/
    │       │   ├── QuoteLayoutJpaAdapter.java   ← implementa QuoteLayoutRepository
    │       │   └── LocationJpaAdapter.java      ← implementa LocationRepository
    │       ├── entities/
    │       │   └── LocationJpa.java
    │       ├── mappers/
    │       │   └── LocationPersistenceMapper.java
    │       └── repositories/
    │           ├── QuoteJpaRepository.java      ← ya existe, reusar
    │           └── LocationJpaRepository.java   ← nueva
    └── config/
        └── LocationLayoutConfig.java
```

#### Notas de implementación

- `QuoteJpa` ya existe en el context `folio`. Se debe agregar los campos `numberOfLocations` y `locationType` a esa clase y migrar con Flyway V3.
- `QuoteJpaRepository` ya existe — `QuoteLayoutJpaAdapter` puede inyectarlo o se puede crear un segundo adaptador que use el mismo `JpaRepository` (opción recomendada para no contaminar el context `folio`).
- La lógica de sincronización (agregar / desactivar ubicaciones) vive en `SaveLocationLayoutUseCaseImpl`, no en el adaptador de persistencia.
- `@Transactional` en `SaveLocationLayoutUseCaseImpl` para garantizar atomicidad entre el update de `QuoteJpa` y los inserts/updates de `LocationJpa`.
- Captura `ObjectOptimisticLockingFailureException` de Spring Data y la convierte en respuesta 409 en `GlobalExceptionHandler`.

---

## 3. LISTA DE TAREAS

### Backend

#### Modelo de Datos
- [ ] Crear migración `V3__add_layout_columns_to_quotes.sql` — columnas `number_of_locations`, `location_type` en `quotes`
- [ ] Crear migración `V4__create_locations_table.sql` — tabla `locations` con FK, constraint único y índices
- [ ] Agregar campos `numberOfLocations` y `locationType` a `QuoteJpa`
- [ ] Crear entidad `LocationJpa` con todos los campos definidos en el diseño

#### Domain
- [ ] Crear Value Object `LayoutConfiguration` (record: numberOfLocations, locationType)
- [ ] Crear enum `LocationType` (SINGLE, MULTIPLE)
- [ ] Crear domain model `Location` (record: index, active)
- [ ] Crear input port `GetLocationLayoutUseCase`
- [ ] Crear input port `SaveLocationLayoutUseCase`
- [ ] Crear output port `QuoteLayoutRepository` (findByFolioNumber, saveLayout)
- [ ] Crear output port `LocationRepository` (findActiveByQuoteId, saveAll)

#### Application
- [ ] Implementar `GetLocationLayoutUseCaseImpl` — busca QuoteJpa y mapea layoutConfiguration
- [ ] Implementar `SaveLocationLayoutUseCaseImpl` con lógica de sincronización:
  - Leer cuántas ubicaciones activas hay actualmente
  - Si nuevo > actual: insertar (nuevo - actual) LocationJpa vacíos con active=true
  - Si nuevo < actual: marcar LocationJpa con index > nuevo como active=false
  - Si igual: solo actualizar numberOfLocations y locationType en QuoteJpa
  - Guardar QuoteJpa (dispara @Version check)

#### Infrastructure
- [ ] Crear `LocationJpaRepository` extendiendo `JpaRepository<LocationJpa, Long>`
  - Método: `List<LocationJpa> findByQuoteIdAndActiveTrue(Long quoteId)`
  - Método: `List<LocationJpa> findByQuoteId(Long quoteId)`
- [ ] Crear `QuoteLayoutJpaAdapter` implementando `QuoteLayoutRepository`
- [ ] Crear `LocationJpaAdapter` implementando `LocationRepository`
- [ ] Crear `LocationPersistenceMapper`
- [ ] Crear DTOs: `LayoutConfigurationDto`, `GetLayoutResponse`, `SaveLayoutRequest`, `SaveLayoutResponse`
- [ ] Crear `LocationLayoutRestMapper`
- [ ] Crear `LocationLayoutApi` (interfaz Swagger en `swaggerdocs/`)
- [ ] Crear `LocationLayoutController` implementando `LocationLayoutApi`
- [ ] Crear `LocationLayoutConfig` — wiring de use cases con sus puertos
- [ ] Registrar `ObjectOptimisticLockingFailureException` en `GlobalExceptionHandler` → 409

#### Tests Backend (TDD — test antes de implementar)
- [ ] `GetLocationLayoutUseCaseImplTest` — happy path, folio not found
- [ ] `SaveLocationLayoutUseCaseImplTest` — primera vez, aumentar, reducir, igual, version conflict
- [ ] `LocationLayoutControllerTest` — GET 200, GET 404, PUT 200, PUT 404, PUT 409, PUT 422
- [ ] `LocationLayoutRestMapperTest` — mapeo request→command, result→response
- [ ] `LocationJpaAdapterTest` — findActive, saveAll
- [ ] `QuoteLayoutJpaAdapterTest` — findByFolioNumber, saveLayout
- [ ] `LocationPersistenceMapperTest` — domain→jpa, jpa→domain
- [ ] `SaveLocationLayoutIntegrationTest` — test de integración con Testcontainers: escenarios 2.1, 2.2, 2.3, 2.4

### QA
- [ ] Ejecutar skill `/gherkin-case-generator` → criterios CRITERIO-1.1 al 2.6
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD de riesgos
- [ ] Validar todos los criterios de aceptación contra la implementación
- [ ] Actualizar estado spec: `status: IMPLEMENTED`

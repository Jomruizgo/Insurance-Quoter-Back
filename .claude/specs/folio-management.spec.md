---
id: SPEC-002
status: APPROVED
feature: folio-management
created: 2026-04-21
updated: 2026-04-21
approved: 2026-04-21
author: spec-generator
version: "1.0"
related-specs: ["SPEC-001"]
---

# Spec: Creación de Folio con Idempotencia

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción

`Insurance-Quoter-Back` expone `POST /v1/folios` para crear un nuevo folio de cotización. El endpoint valida suscriptor y agente contra el core service, obtiene el número de folio del core service y persiste una entidad `Quote` en estado `CREATED`. Si ya existe un folio activo (mismo `subscriberId` + `agentCode` con `quoteStatus = CREATED`), retorna el existente con HTTP 200 en lugar de crear uno nuevo (idempotencia).

### Requerimiento de Negocio

> `POST /v1/folios` — crea un folio con idempotencia.
> Si ya existe folio para mismo `subscriberId` + `agentCode` sin cotización iniciada → retornar existente (HTTP 200).
> Número de folio se obtiene llamando `GET /v1/folios` al core service.
> Validar que `subscriberId` y `agentCode` existan en catálogos del core service.
> Entidad JPA: `QuoteJpa` (tabla `quotes`). Domain model: `Quote` (sin anotaciones JPA).
> Campos mínimos: `folioNumber`, `quoteStatus`, `subscriberId`, `agentCode`, `version`, `createdAt`, `updatedAt`.

### Historias de Usuario

#### HU-01: Crear folio nuevo

```
Como:        agente de seguros
Quiero:      llamar POST /v1/folios con mi código y el suscriptor
Para:        obtener un número de folio único que inicia el proceso de cotización

Prioridad:   Alta
Estimación:  M
Dependencias: SPEC-001 (core service GET /v1/folios operativo)
Capa:        Backend
```

#### Criterios de Aceptación — HU-01

**Happy Path — folio nuevo**
```gherkin
CRITERIO-1.1: Creación exitosa de folio nuevo
  Dado que:  subscriberId "SUB-001" y agentCode "AGT-123" existen en el core service
             Y no existe ningún folio con quoteStatus CREATED para esa combinación
  Cuando:    se realiza POST /v1/folios con body {"subscriberId": "SUB-001", "agentCode": "AGT-123"}
  Entonces:  la respuesta tiene HTTP 201
             Y el cuerpo contiene "folioNumber" con formato "FOL-<año>-<5dígitos>"
             Y "quoteStatus" es "CREATED"
             Y "underwritingData.subscriberId" es "SUB-001"
             Y "underwritingData.agentCode" es "AGT-123"
             Y "version" es 1
             Y "createdAt" es un timestamp ISO-8601 UTC
             Y el folio queda persistido en la tabla quotes
```

**Idempotencia — folio existente**
```gherkin
CRITERIO-1.2: Retorno de folio existente sin cotización iniciada
  Dado que:  subscriberId "SUB-001" y agentCode "AGT-123" existen en el core service
             Y ya existe un folio "FOL-2026-00042" con quoteStatus CREATED para esa combinación
  Cuando:    se realiza POST /v1/folios con body {"subscriberId": "SUB-001", "agentCode": "AGT-123"}
  Entonces:  la respuesta tiene HTTP 200
             Y el cuerpo contiene "folioNumber" "FOL-2026-00042"
             Y NO se genera un nuevo número de folio en el core service
             Y NO se crea un nuevo registro en la tabla quotes
```

**Error — suscriptor o agente inválido**
```gherkin
CRITERIO-1.3: Rechazo por referencia inválida
  Dado que:  subscriberId "SUB-999" NO existe en el core service
  Cuando:    se realiza POST /v1/folios con body {"subscriberId": "SUB-999", "agentCode": "AGT-123"}
  Entonces:  la respuesta tiene HTTP 400
             Y el cuerpo es {"error": "Invalid subscriber or agent", "code": "INVALID_REFERENCE"}
```

**Error — campos faltantes**
```gherkin
CRITERIO-1.4: Rechazo por validación de campos
  Dado que:  se envía un body sin el campo "agentCode"
  Cuando:    se realiza POST /v1/folios con body {"subscriberId": "SUB-001"}
  Entonces:  la respuesta tiene HTTP 422
             Y el cuerpo contiene "code": "VALIDATION_ERROR"
             Y "fields" incluye "agentCode"
```

**Edge Case — folio existente con cotización iniciada**
```gherkin
CRITERIO-1.5: Creación de folio nuevo aunque exista otro en progreso
  Dado que:  subscriberId "SUB-001" y agentCode "AGT-123" tienen un folio con quoteStatus IN_PROGRESS
  Cuando:    se realiza POST /v1/folios con body {"subscriberId": "SUB-001", "agentCode": "AGT-123"}
  Entonces:  la respuesta tiene HTTP 201
             Y se genera un nuevo folio distinto al existente
```

### Reglas de Negocio

1. **Idempotencia:** si existe Quote con `subscriberId = X` AND `agentCode = Y` AND `quoteStatus = CREATED`, retornar ese Quote con HTTP 200 sin llamar al core service.
2. **Orden de validación:** primero buscar folio existente (idempotencia); si no existe, validar referencias en el core service; si son válidas, obtener número de folio del core y persistir.
3. **Validación de referencias:** `subscriberId` y `agentCode` deben existir en los catálogos del core service (GET /v1/subscribers y GET /v1/agents respectivamente). Si alguno no existe → HTTP 400 `INVALID_REFERENCE`.
4. **Número de folio:** obtenido llamando `GET /v1/folios` al core service (puerto 8081). Nunca se genera localmente.
5. **Estado inicial:** todo folio nuevo inicia con `quoteStatus = CREATED`.
6. **Versión:** inicia en 1 al crear; se incrementa con cada modificación posterior (optimistic locking con `@Version` JPA).
7. **Timestamps:** `createdAt` y `updatedAt` en UTC; se generan automáticamente al persistir.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `QuoteJpa` | tabla `quotes` en `insurance_quoter_db` (:5432) | nueva | Registro de cotización persistida con JPA |
| `Quote` | — (domain model, no persiste directamente) | nueva | Aggregate raíz con lógica de negocio |

#### Campos de la tabla `quotes`

| Columna | Tipo SQL | Obligatorio | Constraint | Descripción |
|---------|----------|-------------|------------|-------------|
| `id` | `BIGSERIAL` | sí | PK | Identificador interno |
| `folio_number` | `VARCHAR(20)` | sí | UNIQUE, NOT NULL | Número de folio (ej. `FOL-2026-00042`) |
| `quote_status` | `VARCHAR(20)` | sí | NOT NULL | Estado: CREATED, IN_PROGRESS, CALCULATED, ISSUED |
| `subscriber_id` | `VARCHAR(50)` | sí | NOT NULL | ID del suscriptor |
| `agent_code` | `VARCHAR(50)` | sí | NOT NULL | Código del agente |
| `version` | `BIGINT` | sí | NOT NULL, DEFAULT 1 | Versión para optimistic locking |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | sí | NOT NULL | Timestamp creación UTC |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | sí | NOT NULL | Timestamp última actualización UTC |

#### Índices / Constraints

- `UNIQUE (folio_number)` — garantiza unicidad del número de folio
- `INDEX (subscriber_id, agent_code, quote_status)` — soporta la consulta de idempotencia (búsqueda frecuente en el happy path y en el path idempotente)

#### Migración Flyway

```sql
-- V1__create_quotes_table.sql
CREATE TABLE IF NOT EXISTS quotes (
    id              BIGSERIAL PRIMARY KEY,
    folio_number    VARCHAR(20)                 NOT NULL UNIQUE,
    quote_status    VARCHAR(20)                 NOT NULL,
    subscriber_id   VARCHAR(50)                 NOT NULL,
    agent_code      VARCHAR(50)                 NOT NULL,
    version         BIGINT                      NOT NULL DEFAULT 1,
    created_at      TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_quotes_idempotency
    ON quotes (subscriber_id, agent_code, quote_status);
```

#### Domain Model: `Quote`

```java
// domain/model/Quote.java  — POJO, sin anotaciones Spring ni JPA
public record Quote(
    String folioNumber,
    QuoteStatus quoteStatus,
    String subscriberId,
    String agentCode,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {}
```

```java
// domain/model/QuoteStatus.java
public enum QuoteStatus {
    CREATED, IN_PROGRESS, CALCULATED, ISSUED
}
```

#### Application Layer: `FolioCreationResult`

```java
// application/usecase/FolioCreationResult.java
public record FolioCreationResult(Quote quote, boolean created) {}
```

> `created = true` → controller retorna HTTP 201. `created = false` → HTTP 200 (idempotencia).

### API Endpoints

#### POST /v1/folios

- **Descripción:** Crea folio nuevo o retorna existente (idempotencia)
- **Auth requerida:** no
- **Request Body:**
  ```json
  {
    "subscriberId": "SUB-001",
    "agentCode": "AGT-123"
  }
  ```
- **Response 201 — Created:**
  ```json
  {
    "folioNumber": "FOL-2026-00042",
    "quoteStatus": "CREATED",
    "underwritingData": {
      "subscriberId": "SUB-001",
      "agentCode": "AGT-123"
    },
    "createdAt": "2026-04-20T14:30:00Z",
    "version": 1
  }
  ```
- **Response 200 — Already exists (idempotent):** misma estructura, datos del folio existente
- **Response 400:** `{"error": "Invalid subscriber or agent", "code": "INVALID_REFERENCE"}`
- **Response 422:** `{"error": "Validation failed", "code": "VALIDATION_ERROR", "fields": ["agentCode"]}`

### Arquitectura Hexagonal — Descomposición de Componentes

#### Contexto: `folio`

```
com.sofka.insurancequoter.back.folio/
├── domain/
│   ├── model/
│   │   ├── Quote.java                              ← Record (folioNumber, quoteStatus, subscriberId, agentCode, version, createdAt, updatedAt)
│   │   └── QuoteStatus.java                        ← Enum: CREATED | IN_PROGRESS | CALCULATED | ISSUED
│   └── port/
│       ├── in/
│       │   └── CreateFolioUseCase.java             ← Input Port: FolioCreationResult createFolio(CreateFolioCommand)
│       └── out/
│           ├── QuoteRepository.java                ← Output Port: findActiveBySubscriberAndAgent / save
│           └── CoreServiceClient.java              ← Output Port: validateSubscriber / validateAgent / nextFolioNumber
├── application/
│   └── usecase/
│       ├── CreateFolioCommand.java                 ← Record: subscriberId, agentCode
│       ├── FolioCreationResult.java                ← Record: quote, created (boolean)
│       └── CreateFolioUseCaseImpl.java             ← Orquesta: idempotencia → validación → core call → persist
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   └── rest/
    │   │       ├── FolioController.java            ← POST /v1/folios → CreateFolioUseCase
    │   │       ├── swaggerdocs/
    │   │       │   └── FolioApi.java               ← @Tag, @Operation, @ApiResponse(201), @ApiResponse(200)
    │   │       ├── dto/
    │   │       │   ├── CreateFolioRequest.java     ← { subscriberId, agentCode } con @NotBlank
    │   │       │   ├── FolioResponse.java          ← { folioNumber, quoteStatus, underwritingData, createdAt, version }
    │   │       │   └── UnderwritingDataDto.java    ← { subscriberId, agentCode }
    │   │       └── mapper/
    │   │           └── FolioRestMapper.java        ← Quote → FolioResponse
    │   └── out/
    │       ├── persistence/
    │       │   ├── adapter/
    │       │   │   └── QuoteJpaAdapter.java        ← implementa QuoteRepository; inyecta QuoteJpaRepository
    │       │   ├── repositories/
    │       │   │   └── QuoteJpaRepository.java     ← extiende JpaRepository<QuoteJpa, Long>
    │       │   ├── entities/
    │       │   │   └── QuoteJpa.java               ← @Entity tabla quotes; @Version en campo version
    │       │   └── mappers/
    │       │       └── QuotePersistenceMapper.java ← Quote ↔ QuoteJpa
    │       └── http/
    │           ├── adapter/
    │           │   └── CoreServiceClientAdapter.java ← implementa CoreServiceClient; usa RestClient
    │           └── dto/
    │               ├── SubscribersResponse.java    ← { subscribers: [...] }
    │               ├── AgentsResponse.java         ← { agents: [...] }
    │               └── CoreFolioResponse.java      ← { folioNumber, generatedAt }
    └── config/
        └── FolioConfig.java                        ← @Bean wiring CreateFolioUseCaseImpl + RestClient (core)
```

#### Contratos de interfaces clave

```java
// domain/port/in/CreateFolioUseCase.java
public interface CreateFolioUseCase {
    FolioCreationResult createFolio(CreateFolioCommand command);
}

// domain/port/out/QuoteRepository.java
public interface QuoteRepository {
    Optional<Quote> findActiveBySubscriberAndAgent(String subscriberId, String agentCode);
    Quote save(Quote quote);
}

// domain/port/out/CoreServiceClient.java
public interface CoreServiceClient {
    boolean existsSubscriber(String subscriberId);
    boolean existsAgent(String agentCode);
    String nextFolioNumber();
}
```

#### Flujo de llamada

```
FolioController.createFolio(request)
  → CreateFolioUseCase.createFolio(command)
      1. QuoteRepository.findActiveBySubscriberAndAgent(subscriberId, agentCode)
         → si encontrado: return FolioCreationResult(existingQuote, false)   ← HTTP 200
      2. CoreServiceClient.existsSubscriber(subscriberId)  → false → throw InvalidReferenceException
      3. CoreServiceClient.existsAgent(agentCode)          → false → throw InvalidReferenceException
      4. CoreServiceClient.nextFolioNumber()               ← "FOL-2026-00042"
      5. QuoteRepository.save(newQuote)
         ← Quote persisted
      6. return FolioCreationResult(newQuote, true)         ← HTTP 201
  ← ResponseEntity(FolioResponse, 201 | 200)
```

#### Manejo de excepciones

| Excepción de dominio | HTTP | Código |
|---------------------|------|--------|
| `InvalidReferenceException` | 400 | `INVALID_REFERENCE` |
| `jakarta.validation.ConstraintViolationException` / `MethodArgumentNotValidException` | 422 | `VALIDATION_ERROR` |

Manejar con `@RestControllerAdvice` en `infrastructure/adapter/in/rest/`.

### Notas de Implementación

- `QuoteJpaRepository.findBySubscriberIdAndAgentCodeAndQuoteStatus(subscriberId, agentCode, "CREATED")` — Spring Data deriva la query automáticamente con el nombre del método.
- `@Version` en `QuoteJpa.version` activa optimistic locking de JPA; el valor inicial es `null` antes del primer `save` y JPA lo asigna a `1`.
- `CoreServiceClientAdapter` usa `RestClient` (Spring 6 / Spring Boot 3+) — no `RestTemplate`. URL base configurable via `application.properties` (`core.service.base-url=http://localhost:8081`).
- `createdAt` y `updatedAt` se gestionan con `@CreationTimestamp` y `@UpdateTimestamp` de Hibernate en `QuoteJpa`.
- `CreateFolioCommand` y `FolioCreationResult` viven en `application/usecase/` — no son DTOs REST ni domain objects, son objetos de coordinación de aplicación.
- La validación de request (`@NotBlank`) se declara en `CreateFolioRequest` con Bean Validation; `@Valid` en el controller activa la validación automática.

---

## 3. LISTA DE TAREAS

> Checklist accionable. Marcar cada ítem (`[x]`) al completarlo.

### Backend

#### Base de Datos
- [ ] Crear migración Flyway `V1__create_quotes_table.sql` — tabla `quotes` con todos los campos definidos en el diseño
- [ ] Crear índice `idx_quotes_idempotency` en `(subscriber_id, agent_code, quote_status)`
- [ ] Configurar Flyway en `application.properties` apuntando a `insurance_quoter_db` (:5432)

#### Dominio
- [ ] Crear enum `QuoteStatus` en `folio/domain/model/QuoteStatus.java` — valores: CREATED, IN_PROGRESS, CALCULATED, ISSUED
- [ ] Crear record `Quote` en `folio/domain/model/Quote.java` — campos: folioNumber, quoteStatus, subscriberId, agentCode, version, createdAt, updatedAt
- [ ] Crear Input Port `CreateFolioUseCase` en `folio/domain/port/in/` — método `FolioCreationResult createFolio(CreateFolioCommand)`
- [ ] Crear Output Port `QuoteRepository` en `folio/domain/port/out/` — métodos: `findActiveBySubscriberAndAgent`, `save`
- [ ] Crear Output Port `CoreServiceClient` en `folio/domain/port/out/` — métodos: `existsSubscriber`, `existsAgent`, `nextFolioNumber`

#### Aplicación
- [ ] Crear record `CreateFolioCommand` en `folio/application/usecase/` — campos: subscriberId, agentCode
- [ ] Crear record `FolioCreationResult` en `folio/application/usecase/` — campos: quote (Quote), created (boolean)
- [ ] Crear excepción `InvalidReferenceException` en `folio/application/` (o `folio/domain/`)
- [ ] Crear `CreateFolioUseCaseImpl` en `folio/application/usecase/` — implementa lógica: idempotencia → validación referencias → nextFolioNumber → save

#### Infraestructura — Persistencia
- [ ] Crear `QuoteJpa` en `folio/infrastructure/adapter/out/persistence/entities/` — `@Entity @Table(name = "quotes")`, `@Version`, `@CreationTimestamp`, `@UpdateTimestamp`
- [ ] Crear `QuoteJpaRepository` en `folio/infrastructure/adapter/out/persistence/repositories/` — extiende `JpaRepository<QuoteJpa, Long>`; método `findBySubscriberIdAndAgentCodeAndQuoteStatus`
- [ ] Crear `QuotePersistenceMapper` en `folio/infrastructure/adapter/out/persistence/mappers/` — convierte `Quote ↔ QuoteJpa`
- [ ] Crear `QuoteJpaAdapter` en `folio/infrastructure/adapter/out/persistence/adapter/` — implementa `QuoteRepository`; inyecta `QuoteJpaRepository` y `QuotePersistenceMapper`

#### Infraestructura — HTTP Client
- [ ] Crear DTOs de respuesta del core: `SubscribersResponse`, `AgentsResponse`, `CoreFolioResponse` en `folio/infrastructure/adapter/out/http/dto/`
- [ ] Crear `CoreServiceClientAdapter` en `folio/infrastructure/adapter/out/http/adapter/` — implementa `CoreServiceClient`; usa `RestClient` con base URL configurable; llama GET /v1/subscribers, GET /v1/agents, GET /v1/folios

#### Infraestructura — REST
- [ ] Crear DTO `CreateFolioRequest` en `folio/infrastructure/adapter/in/rest/dto/` — campos con `@NotBlank`: subscriberId, agentCode
- [ ] Crear DTO `UnderwritingDataDto` en `folio/infrastructure/adapter/in/rest/dto/` — campos: subscriberId, agentCode
- [ ] Crear DTO `FolioResponse` en `folio/infrastructure/adapter/in/rest/dto/` — campos: folioNumber, quoteStatus, underwritingData, createdAt, version
- [ ] Crear `FolioRestMapper` en `folio/infrastructure/adapter/in/rest/mapper/` — método `FolioResponse toResponse(Quote quote)`
- [ ] Crear Swagger interface `FolioApi` en `folio/infrastructure/adapter/in/rest/swaggerdocs/` — `@PostMapping("/v1/folios")`, `@Tag`, `@Operation`, `@ApiResponse(201)`, `@ApiResponse(200)`, `@ApiResponse(400)`, `@ApiResponse(422)`
- [ ] Crear `FolioController` en `folio/infrastructure/adapter/in/rest/` — implementa `FolioApi`; inyecta `CreateFolioUseCase` y `FolioRestMapper`; retorna `ResponseEntity` con 201 o 200 según `FolioCreationResult.created`
- [ ] Crear `GlobalExceptionHandler` en `folio/infrastructure/adapter/in/rest/` — `@RestControllerAdvice`; maneja `InvalidReferenceException` → 400, `MethodArgumentNotValidException` → 422

#### Configuración
- [ ] Crear `FolioConfig` en `folio/infrastructure/config/` — `@Bean` para `CreateFolioUseCaseImpl`, `RestClient` configurado con `core.service.base-url`
- [ ] Añadir en `application.properties`: `core.service.base-url=http://localhost:8081`

#### Tests Backend (TDD — escribir el test ANTES de la implementación)
- [ ] `CreateFolioUseCaseImplTest` — idempotencia: folio existente en CREATED → retorna FolioCreationResult(quote, created=false) sin llamar core
- [ ] `CreateFolioUseCaseImplTest` — folio nuevo: referencias válidas → llama nextFolioNumber, persiste, retorna FolioCreationResult(quote, created=true)
- [ ] `CreateFolioUseCaseImplTest` — subscriberId inválido → lanza InvalidReferenceException
- [ ] `CreateFolioUseCaseImplTest` — agentCode inválido → lanza InvalidReferenceException
- [ ] `CreateFolioUseCaseImplTest` — folio existente en IN_PROGRESS → crea folio nuevo (HTTP 201)
- [ ] `QuoteJpaAdapterTest` — findActiveBySubscriberAndAgent devuelve Optional.empty cuando no hay folio CREATED
- [ ] `QuoteJpaAdapterTest` — save persiste correctamente y retorna Quote con version=1
- [ ] `QuotePersistenceMapperTest` — Quote → QuoteJpa y QuoteJpa → Quote mapean todos los campos correctamente
- [ ] `CoreServiceClientAdapterTest` — existsSubscriber true cuando core retorna el id en la lista
- [ ] `CoreServiceClientAdapterTest` — existsSubscriber false cuando core no retorna el id
- [ ] `CoreServiceClientAdapterTest` — nextFolioNumber retorna el folioNumber del core response
- [ ] `FolioRestMapperTest` — toResponse mapea folioNumber, quoteStatus, subscriberId, agentCode, createdAt, version
- [ ] `FolioControllerTest` — POST /v1/folios con creación nueva → HTTP 201 con body correcto
- [ ] `FolioControllerTest` — POST /v1/folios idempotente → HTTP 200 con body correcto
- [ ] `FolioControllerTest` — POST /v1/folios sin agentCode → HTTP 422 con VALIDATION_ERROR
- [ ] `FolioControllerTest` — POST /v1/folios con referencia inválida → HTTP 400 con INVALID_REFERENCE
- [ ] `CreateFolioIntegrationTest` — `@SpringBootTest` + Testcontainers PostgreSQL + WireMock para core: flujo completo 201
- [ ] `CreateFolioIntegrationTest` — flujo idempotente: segunda llamada retorna 200 con mismo folioNumber

### QA
- [ ] Ejecutar skill `/gherkin-case-generator` → escenarios para CRITERIO-1.1, 1.2, 1.3, 1.4, 1.5
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD de riesgos (core unavailable, concurrencia en idempotencia, versión conflict)
- [ ] Validar manualmente con Bruno/Postman: `POST http://localhost:8080/v1/folios`
- [ ] Verificar idempotencia: dos llamadas iguales → segunda retorna 200 con mismo folioNumber
- [ ] Verificar que folio en IN_PROGRESS no bloquea creación de nuevo folio
- [ ] Actualizar estado spec: `status: IMPLEMENTED`
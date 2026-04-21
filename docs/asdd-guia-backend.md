# Guía ASDD — Backends Insurance Quoter

> Esta guía aplica a **ambos microservicios backend**: `Insurance-Quoter-Back` (plataforma-danos-back) e `Insurance-Quoter-Core` (plataforma-core-ohs). Cada uno tiene sus propios features, pero comparten el mismo stack, arquitectura y metodología ASDD.

Paso a paso para trabajar los backends del cotizador con la metodología ASDD en Claude Code.

**Prerequisito:** tener Claude Code corriendo en la raíz del proyecto (`Sofka-IQ/`).

---

## Microservicios backend

| Proyecto | Nombre lógico | Puerto | Base de datos | Responsabilidad |
|----------|--------------|--------|---------------|-----------------|
| `Insurance-Quoter-Back/` | `plataforma-danos-back` | 8080 | `insurance_quoter_db` (:5432) | Cotizaciones, ubicaciones, coberturas, cálculo de prima |
| `Insurance-Quoter-Core/` | `plataforma-core-ohs` | 8081 | `insurance_core_db` (:5433) | Catálogos, tarifas, agentes, suscriptores, folios, CP |

Cada microservicio tiene su propio ciclo ASDD independiente. **No mezclar implementaciones entre proyectos.**

---

## Flujo ASDD por feature

```
[FASE 1 — Secuencial]
  /generate-spec <feature>
  → usuario revisa y aprueba (status: APPROVED)

[FASE 2 — Implementación paralela]
  backend-developer  ∥  database-agent (si hay modelos nuevos)

[FASE 3 — Tests paralelos]
  test-engineer-backend

[FASE 4 — QA]
  qa-agent
  └── /gherkin-case-generator  → test cases funcionales (Gherkin)
  └── /risk-identifier         → matriz de riesgos ASD
  └── /performance-analyzer    → si hay SLAs definidos
```

> **Atajo:** `/asdd-orchestrate <feature>` corre las 4 fases automáticamente.
> **Nunca saltar Fase 1** — sin spec `APPROVED` no hay implementación.

---

## Fase 1 — Generar y aprobar la spec

### Comando
```
/generate-spec <nombre-feature>
```

### Aprobación
Cuando `/generate-spec` crea `.claude/specs/<feature>.spec.md`, revisar y cambiar:

```yaml
status: APPROVED
updated: 2026-04-XX
```

---

## Fase 2 — Implementación (por microservicio)

### Insurance-Quoter-Back — features a implementar

| # | Feature | Endpoints |
|---|---------|-----------|
| 1 | `folio-management` | POST /v1/folios |
| 2 | `quote-general-info` | GET/PUT /v1/quotes/{folio}/general-info |
| 3 | `location-layout` | GET/PUT /v1/quotes/{folio}/locations/layout |
| 4 | `location-management` | GET/PUT/PATCH /v1/quotes/{folio}/locations + summary |
| 5 | `quote-state` | GET /v1/quotes/{folio}/state |
| 6 | `coverage-options` | GET/PUT /v1/quotes/{folio}/coverage-options |
| 7 | `premium-calculation` | POST /v1/quotes/{folio}/calculate |

### Insurance-Quoter-Core — features a implementar

| # | Feature | Endpoints |
|---|---------|-----------|
| 1 | `folio-generator` | GET /v1/folios |
| 2 | `subscribers-catalog` | GET /v1/subscribers |
| 3 | `agents-catalog` | GET /v1/agents |
| 4 | `business-lines-catalog` | GET /v1/business-lines |
| 5 | `zip-codes` | GET /v1/zip-codes/{zipCode}, POST /v1/zip-codes/validate |
| 6 | `risk-classification-catalog` | GET /v1/catalogs/risk-classification |
| 7 | `guarantees-catalog` | GET /v1/catalogs/guarantees |
| 8 | `tariffs` | GET /v1/tariffs |

### Comando de implementación
```
/implement-backend <nombre-feature>
```

### Swagger / OpenAPI (obligatorio en todo endpoint nuevo)

Las anotaciones Swagger **nunca van en el controller**. Se declaran en una interfaz dentro de `rest/swaggerdocs/`. El controller implementa esa interfaz y queda limpio de documentación.

```
infrastructure/adapter/in/rest/
├── swaggerdocs/
│   └── FolioApi.java       ← @Tag, @Operation, @ApiResponse aquí
└── FolioController.java    ← implements FolioApi, sin anotaciones Swagger
```

```java
// swaggerdocs/FolioApi.java
@Tag(name = "Folios", description = "Gestión de folios de cotización")
@RequestMapping("/v1/folios")
public interface FolioApi {

    @Operation(summary = "Crear o recuperar folio")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Folio creado"),
        @ApiResponse(responseCode = "200", description = "Folio existente recuperado"),
        @ApiResponse(responseCode = "422", description = "Datos inválidos")
    })
    @PostMapping
    ResponseEntity<FolioResponse> createFolio(@Valid @RequestBody CreateFolioRequest request);
}

// FolioController.java
@RestController
@RequiredArgsConstructor
public class FolioController implements FolioApi {
    private final CreateFolioUseCase createFolioUseCase;

    @Override
    public ResponseEntity<FolioResponse> createFolio(CreateFolioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createFolioUseCase.createFolio(request));
    }
}
```

Swagger UI: `http://localhost:<puerto>/swagger-ui/index.html`

> Ver patrón completo y anti-patrones en `.claude/rules/backend.md` — sección "OpenAPI / Swagger".

### Prompts de ejemplo por feature (Insurance-Quoter-Back)

#### Feature 1: Gestión de folios
```
/generate-spec folio-management

Contexto: Insurance-Quoter-Back — Java 21 + Spring Boot 4 + Hexagonal + PostgreSQL.
Feature: Creación de folios con idempotencia.

Endpoint: POST /v1/folios
Contrato completo en docs/api-contracts.md, sección 1.

Reglas de negocio:
- Si ya existe un folio para el mismo subscriberId + agentCode sin cotización iniciada,
  retornar el existente (HTTP 200) en lugar de crear uno nuevo (HTTP 201).
- El número de folio se genera llamando al core service GET /v1/folios.
- Validar que subscriberId y agentCode existan en los catálogos del core service.

Entidades JPA (infrastructure/adapter/out/persistence/): QuoteJpa (tabla: quotes).
Domain model (domain/model/): Quote — sin anotaciones JPA.
Campos mínimos: folioNumber, quoteStatus, subscriberId, agentCode, version, createdAt, updatedAt.
```

#### Feature 2: Datos generales
```
/generate-spec quote-general-info

Contexto: Insurance-Quoter-Back — Java 21 + Spring Boot 4 + Hexagonal + PostgreSQL.
Feature: Consulta y actualización de datos generales de una cotización.

Endpoints:
- GET /v1/quotes/{folio}/general-info
- PUT /v1/quotes/{folio}/general-info

Contratos en docs/api-contracts.md, sección 2.

Reglas de negocio:
- PUT valida versionado optimista (@Version en QuoteJpa). Si versión no coincide → 409.
- Al actualizar: incrementar version y actualizar updatedAt.
- insuredData y underwritingData se persisten dentro del aggregate Quote.
- Validar que riskClassification pertenezca al catálogo del core service.
```

#### Feature 3: Layout de ubicaciones
```
/generate-spec location-layout

Contexto: Insurance-Quoter-Back — Java 21 + Spring Boot 4 + Hexagonal + PostgreSQL.
Feature: Configuración del layout de ubicaciones de una cotización.

Endpoints:
- GET /v1/quotes/{folio}/locations/layout
- PUT /v1/quotes/{folio}/locations/layout

Contratos en docs/api-contracts.md, sección 3.

Reglas de negocio:
- Al guardar el layout, si numberOfLocations cambia: agregar entradas vacías o marcar
  excedentes como inactivas — nunca eliminar datos.
- Versionado optimista obligatorio en PUT.

Entidades: QuoteJpa (campo layoutConfiguration), LocationJpa (tabla: locations, FK a quotes).
```

#### Feature 4: Gestión de ubicaciones
```
/generate-spec location-management

Contexto: Insurance-Quoter-Back — Java 21 + Spring Boot 4 + Hexagonal + PostgreSQL.
Feature: Registro, consulta y edición de ubicaciones dentro de una cotización.

Endpoints:
- GET /v1/quotes/{folio}/locations
- PUT /v1/quotes/{folio}/locations
- PATCH /v1/quotes/{folio}/locations/{index}
- GET /v1/quotes/{folio}/locations/summary

Contratos en docs/api-contracts.md, sección 4.

Reglas de negocio:
- PATCH es actualización parcial: solo se actualizan campos presentes en el body.
- Validar zipCode contra el core service. Si inválido → alerta MISSING_ZIP_CODE + validationStatus = INCOMPLETE.
- Sin businessLine.fireKey → alerta MISSING_FIRE_KEY.
- Sin guarantees tarifables → alerta NO_TARIFABLE_GUARANTEES.
- blockingAlerts se recalcula en cada escritura.
- GET /summary devuelve solo index, locationName, validationStatus y blockingAlerts.

Entidades: LocationJpa (tabla: locations), BlockingAlertJpa (@ElementCollection en LocationJpa).
```

#### Feature 5: Estado de cotización
```
/generate-spec quote-state

Contexto: Insurance-Quoter-Back — Java 21 + Spring Boot 4 + Hexagonal + PostgreSQL.
Feature: Consulta del estado y progreso de completitud de una cotización.

Endpoint: GET /v1/quotes/{folio}/state
Contrato en docs/api-contracts.md, sección 5.

Reglas de negocio:
- completionPercentage se calcula en el use case (no se persiste).
- Secciones evaluadas: generalInfo, layout, locations, coverageOptions, calculation.
- Una sección está COMPLETE si todos sus campos obligatorios están llenos y sin alertas bloqueantes.
- quoteStatus: CREATED | IN_PROGRESS | CALCULATED | ISSUED.

Sin entidades nuevas: se lee del aggregate Quote y sus relaciones.
```

#### Feature 6: Opciones de cobertura
```
/generate-spec coverage-options

Contexto: Insurance-Quoter-Back — Java 21 + Spring Boot 4 + Hexagonal + PostgreSQL.
Feature: Consulta y configuración de opciones de cobertura para una cotización.

Endpoints:
- GET /v1/quotes/{folio}/coverage-options
- PUT /v1/quotes/{folio}/coverage-options

Contratos en docs/api-contracts.md, sección 6.

Reglas de negocio:
- Las coberturas disponibles vienen del catálogo del core service GET /v1/catalogs/guarantees.
- Solo se pueden seleccionar coberturas del catálogo.
- deductiblePercentage y coinsurancePercentage: rango 0-100.
- Versionado optimista obligatorio en PUT.

Entidades: CoverageOptionJpa (tabla: coverage_options, FK a quotes).
```

#### Feature 7: Cálculo de prima
```
/generate-spec premium-calculation

Contexto: Insurance-Quoter-Back — Java 21 + Spring Boot 4 + Hexagonal + PostgreSQL.
Feature: Cálculo de prima neta y comercial para todas las ubicaciones calculables.

Endpoint: POST /v1/quotes/{folio}/calculate
Contrato en docs/api-contracts.md, sección 7.

Reglas de negocio:
- Leer cotización completa: Quote + Locations + CoverageOptions.
- Leer tarifas y factores técnicos desde el core service GET /v1/tariffs.
- Por cada ubicación: determinar si es calculable (zipCode válido + fireKey + garantía tarifable).
- Si una ubicación no es calculable: registrar alerta, continuar con las demás.
- Si NINGUNA ubicación es calculable → HTTP 422 con código NO_CALCULABLE_LOCATIONS.
- Calcular netPremium por ubicación (desglose en docs/api-contracts.md sección 7).
- netPremium total = suma de ubicaciones calculables.
- commercialPremium = netPremium * factor comercial (de tarifas).
- Persistir en transacción única: Quote.quoteStatus = CALCULATED + CalculationResultJpa + PremiumByLocationJpa[].
- Versionado optimista obligatorio.

Entidades: CalculationResultJpa (tabla: calculation_results, @OneToOne QuoteJpa),
           PremiumByLocationJpa (tabla: premiums_by_location, @ManyToOne CalculationResultJpa).

IMPORTANTE: lógica de cálculo en CalculationService (domain), 100% TDD. Cada componente
del desglose tiene su propio método privado y su propio test.
```

---

## Fase 3 — Tests de integración

Ejecutado automáticamente en el flujo ASDD después de la Fase 2. Para invocación manual:

```
@test-engineer-backend ejecuta auditoría de tests para <feature>
```

Genera:
- Tests `@DataJpaTest` para repositories con queries custom
- Tests `@SpringBootTest` para flujos de integración completos
- Reporte de cobertura — quality gate: **≥ 80% en lógica de negocio**

---

## Fase 4 — QA y generación de test plan

```
@qa-agent ejecuta QA para .claude/specs/<feature>.spec.md
```

### Qué se genera

| Artefacto | Skill | Ubicación |
|-----------|-------|-----------|
| Test cases funcionales (Gherkin) | `/gherkin-case-generator` | `docs/output/qa/<feature>-gherkin.md` |
| Matriz de riesgos ASD | `/risk-identifier` | `docs/output/qa/<feature>-risks.md` |
| Plan de performance | `/performance-analyzer` | `docs/output/qa/<feature>-performance.md` (solo si hay SLAs) |
| Propuesta de automatización | `/automation-flow-proposer` | `docs/output/qa/automation-proposal.md` (bajo pedido) |

### Cuándo se generan los test cases
Los **test cases funcionales** (escenarios Gherkin) se producen en esta fase, **después** de que la implementación y los tests unitarios/integración están completos. Son la base para las pruebas automatizadas en `Auto_Api_Screenplay/`.

---

## Orden recomendado de implementación

Respetar dependencias entre features:

```
Insurance-Quoter-Core (primero — lo consume el Back):
  1. folio-generator
  2. subscribers-catalog + agents-catalog
  3. business-lines-catalog + zip-codes
  4. risk-classification-catalog + guarantees-catalog
  5. tariffs

Insurance-Quoter-Back (después — consume el Core):
  6. folio-management          ← entry point del flujo
  7. quote-general-info        ← depende de folio
  8. location-layout           ← depende de folio
  9. location-management       ← depende de layout
 10. quote-state               ← depende de todas las secciones
 11. coverage-options          ← depende de catálogos de garantías
 12. premium-calculation       ← depende de todo lo anterior
```

---

## Comandos de referencia rápida

| Qué hacer | Comando |
|-----------|---------|
| Generar spec | `/generate-spec <feature>` |
| Implementar backend | `/implement-backend <feature>` |
| Auditar tests | `@test-engineer-backend` |
| Generar test plan (Gherkin) | `/gherkin-case-generator <feature>` |
| Identificar riesgos | `/risk-identifier <feature>` |
| QA completo (Fase 4) | `@qa-agent` |
| Orquestar flujo completo | `/asdd-orchestrate <feature>` |
| Ver estado del flujo | `/asdd-orchestrate status` |
| Crear issues en GitHub | `/tasks-to-issues <feature>` |

---

## Notas importantes

- **Core primero** — todos los features del Back consumen endpoints del Core; implementar el Core antes.
- **TDD es obligatorio** — el test del use case se escribe antes que el código de producción.
- **Los contratos son la fuente de verdad** — si hay divergencia entre spec y `docs/api-contracts.md`, prevalece el contrato.
- **Swagger en cada endpoint** — anotar con `@Operation` y `@ApiResponse` al implementar el controller.
- **GitFlow** — cada feature en su propia rama `feature/<ticket>-<nombre>` desde `develop`.

# Reality Check — Insurance-Quoter-Back

**Fecha:** 2026-04-22  
**Alcance:** `src/main/java/com/sofka/insurancequoter/`  
**Resumen:** Arquitectura hexagonal bien implementada, sin violaciones. Un bug crítico de wiring en `location` y dos archivos huérfanos a eliminar.

---

## Estado general

| Dimensión | Estado |
|-----------|--------|
| Arquitectura hexagonal | ✅ Correcta en los 4 contextos |
| Regla de dependencia (domain no depende de nada) | ✅ Respetada |
| Constructor injection (sin @Autowired) | ✅ 100% |
| JPA solo en capa de persistencia | ✅ Domain models son `record` limpios |
| Lógica de negocio en use cases / domain service | ✅ Controllers solo parsean HTTP |
| Tests | ✅ 39 clases (unit + integración con Testcontainers + WireMock) |
| **Wiring completo en Spring** | ❌ 2 use cases en `location` no son `@Bean` |

**Implementado y funcional: ~85%**

---

## Bug crítico — Acción inmediata requerida

### `SaveLocationLayoutUseCase` y `GetLocationLayoutUseCase` no están wired en Spring

**Archivos afectados:**
- `back/location/infrastructure/config/LocationConfig.java` — faltan 2 `@Bean`
- `back/location/application/usecase/SaveLocationLayoutUseCaseImpl.java` — implementación existe (102 líneas) pero nunca se registra
- `back/location/application/usecase/GetLocationLayoutUseCaseImpl.java` — ídem

**Consecuencia:** Spring no puede inyectar estos use cases en `LocationLayoutController`. La aplicación falla al arrancar si ese controller los declara como dependencias.

**Fix:**
```java
// LocationConfig.java — agregar los dos @Bean que faltan
@Bean
public SaveLocationLayoutUseCase saveLocationLayoutUseCase(
        QuoteLayoutRepository quoteLayoutRepository,
        LocationRepository locationRepository) {
    return new SaveLocationLayoutUseCaseImpl(quoteLayoutRepository, locationRepository);
}

@Bean
public GetLocationLayoutUseCase getLocationLayoutUseCase(
        QuoteLayoutRepository quoteLayoutRepository) {
    return new GetLocationLayoutUseCaseImpl(quoteLayoutRepository);
}
```

---

## Dead code — Eliminar

### Stub huérfano (artefacto del Spring Initializr)

| Archivo | Problema |
|---------|---------|
| `insurancequoter/Insurance_Quoter/InsuranceQuoterApplication.java` | Clase vacía con comentario "Main class moved to...". Sin referencias. |
| `src/test/java/.../Insurance_Quoter/InsuranceQuoterApplicationTests.java` | Test vacío. Nunca se ejecuta. |

La clase principal real está en `insurancequoter/InsuranceQuoterApplication.java`.

---

## Análisis por contexto

### `back/folio` — Completo

| Capa | Estado |
|------|--------|
| Domain model (`Quote` record) | ✅ |
| Ports in/out | ✅ `CreateFolioUseCase`, `GetQuoteStateUseCase`, `QuoteRepository`, `CoreServiceClient` |
| Use cases | ✅ `CreateFolioUseCaseImpl` (60 líneas), `GetQuoteStateUseCaseImpl` (76 líneas) |
| REST adapter in | ✅ `FolioController`, `QuoteStateController` |
| Swagger docs separados | ✅ `FolioApi`, `QuoteStateApi` |
| Persistence adapter out | ✅ `QuoteJpaAdapter` con `@Version` (optimistic lock) |
| HTTP adapter out | ✅ `CoreServiceClientAdapter` (68 líneas, llama `/v1/subscribers`, `/v1/agents`, `/v1/folios`) |
| Wiring `@Bean` | ✅ `FolioConfig.java` completo |
| Tests | ✅ 10 clases incluye `CreateFolioIntegrationTest` con Testcontainers + WireMock |

---

### `back/location` — Casi completo (ver bug crítico)

| Capa | Estado |
|------|--------|
| Domain models (10 value objects) | ✅ Records sin anotaciones |
| Domain service `LocationValidationService` | ✅ 46 líneas, lógica pura |
| Ports in/out | ✅ 6 input ports + 4 output ports |
| Use case impls | ✅ 5 implementadas (50–102 líneas c/u) |
| REST adapters in | ✅ `LocationController` (4 endpoints), `LocationLayoutController` (2 endpoints) |
| Persistence adapter out | ✅ `LocationJpaAdapter` (159 líneas), `QuoteLayoutJpaAdapter` |
| HTTP adapter out | ✅ `ZipCodeValidationClientAdapter` — valida CP contra core `/v1/zip-codes/{zipCode}` |
| Wiring `@Bean` | ❌ Faltan `SaveLocationLayoutUseCase` y `GetLocationLayoutUseCase` |
| Tests | ⚠️ 11 clases, falta integration test para layout |

---

### `back/coverage` — Completo

| Capa | Estado |
|------|--------|
| Domain model (`CoverageOption` record) | ✅ |
| Ports in/out | ✅ `SaveCoverageOptionsUseCase`, `GetCoverageOptionsUseCase`, `GuaranteeCatalogClient` |
| Use case impls | ✅ `SaveCoverageOptionsUseCaseImpl` (72 líneas) |
| REST adapter in | ✅ `CoverageController` (44 líneas) |
| HTTP adapter out | ✅ `GuaranteeCatalogClientAdapter` — GET `/v1/catalogs/guarantees` |
| Wiring `@Bean` | ✅ `CoverageConfig.java` completo |
| Tests | ✅ 6 clases incluye `CoverageOptionsIntegrationTest` |

---

### `back/calculation` — Completo

| Capa | Estado |
|------|--------|
| Domain models (`CalculationResult`, `PremiumByLocation`, `Tariff`, `CoverageBreakdown`) | ✅ Records |
| Domain service `CalculationService` | ✅ 215 líneas, 13 métodos, lógica pura sin Spring |
| Ports in/out | ✅ `CalculatePremiumUseCase`, `TariffClient`, `QuoteCalculationReader`, `CalculationResultRepository` |
| Use case impl | ✅ `CalculatePremiumUseCaseImpl` (109 líneas) — lee → consulta tarifa → calcula → persiste |
| HTTP adapter out | ✅ `TariffClientAdapter` (74 líneas, con timeout 5s/10s) |
| Persistence adapter out | ✅ `CalculationResultJpaAdapter` (108 líneas, implementa 2 puertos) |
| Wiring `@Bean` | ✅ `CalculationConfig.java` (90 líneas) completo |
| Tests | ✅ 6 clases incluye `PremiumCalculationIntegrationTest` con Testcontainers |

---

## Calidad de tests

| Tipo | Herramientas | Estado |
|------|-------------|--------|
| Unit use cases | JUnit 5 + Mockito | ✅ |
| Unit domain service | JUnit 5 puro | ✅ |
| Unit adapters JPA | `@DataJpaTest` | ✅ |
| Unit controllers | `@WebMvcTest` | ✅ |
| Integration | `@SpringBootTest` + Testcontainers (PostgreSQL 16) + WireMock | ✅ |
| Cobertura (Jacoco) | — | ❌ No configurado |
| E2E automatizado (Serenity BDD) | — | ❌ No implementado (roadmap) |

---

## Deuda técnica menor

| # | Descripción | Impacto |
|---|-------------|---------|
| 1 | Sin reporte de cobertura Jacoco — no se sabe si se cumple el 80% exigido en DoD | Medio |
| 2 | `CalculationService` (215 líneas) — métodos privados como `calculateFireBuildings` y `calculateCattev` solo se prueban indirectamente | Bajo |
| 3 | No hay test del camino de `VersionConflictException` (optimistic lock) | Bajo |
| 4 | `SaveLocationLayoutIntegrationTest` no existe (solo unit) | Bajo |

---

## Resumen de hallazgos a actuar

| Prioridad | Acción |
|-----------|--------|
| 🔴 Crítico | Agregar 2 `@Bean` en `LocationConfig.java` para `SaveLocationLayoutUseCase` y `GetLocationLayoutUseCase` |
| 🟡 Pendiente | Eliminar `Insurance_Quoter/InsuranceQuoterApplication.java` y su test vacío |
| 🟡 Pendiente | Configurar Jacoco para medir cobertura (DoD exige ≥ 80%) |
| 🟢 Opcional | Integration test para el flujo layout en `location` |

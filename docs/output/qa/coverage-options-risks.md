# Matriz de Riesgos — Opciones de Cobertura de Cotización (SPEC-006)

> Generado por `/risk-identifier` | Feature: `coverage-options`
> Spec: `SPEC-006` | Fecha: 2026-04-22

---

## Resumen

Total: 9 | Alto (A): 4 | Medio (S): 3 | Bajo (D): 2

---

## Detalle

| ID    | HU           | Descripción del Riesgo                                                                                                                      | Factores                                                         | Nivel | Testing     |
|-------|--------------|---------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|-------|-------------|
| R-001 | HU-02        | Operación `replaceAll` ejecuta DELETE masivo + INSERT en la misma TX; si la TX no está delimitada con `@Transactional`, un fallo entre DELETE e INSERT deja la cotización sin coberturas — dato irrecuperable sin respaldo | Operación destructiva, integridad transaccional, código nuevo    | **A** | Obligatorio |
| R-002 | HU-02        | Condición de carrera en versionado optimista manual: la verificación `version == command.version` corre antes de persistir; dos threads concurrentes pueden pasar la validación simultáneamente y el segundo sobrescribir al primero sin 409 | Concurrencia, integridad de datos, SLA                           | **A** | Obligatorio |
| R-003 | HU-02        | Integración con core service inestable: si `GET /v1/catalogs/guarantees` falla (timeout, 5xx), el flujo debe propagar `CoreServiceException` → 502; un manejo incorrecto puede propagar 500 o validar incorrectamente codes ausentes | Integración con sistema externo, datos sensibles del catálogo     | **A** | Obligatorio |
| R-004 | HU-02        | Constraint `UNIQUE (quote_id, code)` en la tabla: si el request contiene el mismo `code` dos veces, el `saveAll` lanza `DataIntegrityViolationException` no manejada, retornando 500 en lugar de 422 | Flujo crítico sin validación explícita en el use case            | **A** | Obligatorio |
| R-005 | HU-02        | La `description` debe enriquecerse desde el catálogo del core, no del request. Si el mapper toma la descripción del request en lugar del catálogo, el constraint de dominio se viola silenciosamente sin error visible | Lógica de enriquecimiento, fuente de verdad del catálogo         | **S** | Recomendado |
| R-006 | HU-02        | El catálogo del core puede cambiar entre la llamada `fetchGuarantees()` y la validación de codes: un code válido al momento de la solicitud puede desaparecer entre peticiones concurrentes, provocando un rechazo inesperado | Consistencia eventual, integración con sistema externo           | **S** | Recomendado |
| R-007 | HU-01/HU-02  | El campo `version` en el response GET no refleja la versión post-PUT si la caché de Hibernate no se invalida correctamente; un `EntityManager` con snapshot desactualizado puede retornar `version` anterior | Comportamiento ORM, versionado Hibernate                         | **S** | Recomendado |
| R-008 | HU-01        | Serialización de `BigDecimal` para `deductiblePercentage` y `coinsurancePercentage`: Jackson puede serializar como número con escala variable (`2.0` vs `2.00`); puede romper el contrato con el frontend si espera un formato fijo | Ajuste de serialización, contrato de API                         | **D** | Opcional    |
| R-009 | HU-02        | El body `PUT` permite enviar `coverageOptions: []` (lista vacía); el comportamiento esperado es que se eliminen todas las coberturas previas sin error; sin test explícito puede no estar cubierto | Edge case no cubierto explícitamente en criterios de aceptación  | **D** | Opcional    |

---

## Plan de Mitigación — Riesgos ALTO

---

### R-001: Operación replaceAll destructiva sin atomicidad garantizada

**Descripción:** `CoverageOptionJpaAdapter.replaceAll` ejecuta `deleteAllByQuote_Id(quoteId)` seguido de `saveAll(newOptions)`. Si la transacción no está correctamente delimitada con `@Transactional`, un fallo en el INSERT (ej. violación de constraint) después del DELETE deja el folio sin coberturas — estado inconsistente irrecuperable sin snapshot previo.

**Mitigación:**
- Verificar que `replaceAll` en `CoverageOptionJpaAdapter` tiene `@Transactional`.
- El test de integración `CoverageOptionsIntegrationTest` ya cubre PUT+GET; agregar verificación explícita de rollback forzando fallo en INSERT.
- No ejecutar DELETE y INSERT como llamadas separadas fuera de un bloque `@Transactional`.

**Tests obligatorios:**
- `CoverageOptionsIntegrationTest` — PUT exitoso → GET posterior retorna datos consistentes.
- `CoverageOptionJpaAdapterTest` — `replaceAll` ejecuta ambas operaciones en la misma transacción.

**Bloqueante para release:** ✅ Sí

---

### R-002: Condición de carrera en versionado optimista manual

**Descripción:** `SaveCoverageOptionsUseCaseImpl` llama `quoteLookupPort.assertVersionMatches(folio, command.version())` antes de persistir. Si dos threads concurrentes superan la validación simultáneamente (ambos leen version=6, ambos validan OK), el segundo save puede sobrescribir al primero sin disparar 409. La guarda final la provee `@Version` de Hibernate en `QuoteJpa`, que lanza `ObjectOptimisticLockingFailureException` — debe estar correctamente manejada en `GlobalExceptionHandler`.

**Mitigación:**
- Verificar que `GlobalExceptionHandler` captura `ObjectOptimisticLockingFailureException` → 409 (ya existe en el handler).
- El `replaceAll` debe incluir un `save(quoteJpa)` que dispare el incremento de `@Version` en Hibernate.
- Test de integración con dos requests concurrentes sobre el mismo folio.

**Tests obligatorios:**
- `SaveCoverageOptionsUseCaseImplTest` — version conflict en check manual → `VersionConflictException`.
- `CoverageOptionsIntegrationTest` — PUT con versión desactualizada → 409.

**Bloqueante para release:** ✅ Sí

---

### R-003: Integración con core service inestable (catálogo de garantías)

**Descripción:** `GuaranteeCatalogClientAdapter` llama `GET /v1/catalogs/guarantees` en cada PUT. Si el core no responde (timeout, 503) el adapter lanza `CoreServiceException` → `GlobalExceptionHandler` devuelve 502. Un manejo incorrecto puede: (a) propagar excepción de red como 500, (b) retornar lista vacía del catálogo y rechazar todos los codes como inválidos con 422.

**Mitigación:**
- `GuaranteeCatalogClientAdapter` usa `onStatus(HttpStatusCode::isError, ...)` para lanzar `CoreServiceException` — nunca retorna lista vacía ante error.
- WireMock en tests de integración simula: (a) 200 con catálogo, (b) 503 del core.
- Documentar en `CoverageConfig` el timeout del `RestClient` para el core.

**Tests obligatorios:**
- `GuaranteeCatalogClientAdapterTest` — 503 del core → `CoreServiceException` (no lista vacía).
- `CoverageOptionsIntegrationTest` — WireMock 503 → response 502 CORE_SERVICE_ERROR.

**Bloqueante para release:** ✅ Sí

---

### R-004: Constraint UNIQUE (quote_id, code) sin validación previa de duplicados

**Descripción:** El use case no valida que el mismo `code` no aparezca dos veces en el request. Si el request envía `[{code: "GUA-FIRE"}, {code: "GUA-FIRE"}]`, el `saveAll` lanza `DataIntegrityViolationException` (violación de UNIQUE constraint) que no está manejada en `GlobalExceptionHandler`, retornando HTTP 500 en lugar de 422.

**Mitigación:**
- Agregar validación en `SaveCoverageOptionsUseCaseImpl` que detecte codes duplicados en el request antes de llamar a `replaceAll`.
- O agregar handler para `DataIntegrityViolationException` en `GlobalExceptionHandler` → 422 VALIDATION_ERROR.
- Test unitario en `SaveCoverageOptionsUseCaseImplTest` con request que contiene codes duplicados.

**Tests obligatorios:**
- `SaveCoverageOptionsUseCaseImplTest` — request con codes duplicados → excepción controlada (no 500).
- `CoverageOptionsIntegrationTest` — PUT con codes duplicados → 422 o 409 (no 500).

**Bloqueante para release:** ✅ Sí

---

## Riesgos MEDIO — Acciones Recomendadas

| ID    | Acción recomendada |
|-------|--------------------|
| R-005 | En `CoverageOptionsIntegrationTest`, verificar explícitamente que la `description` del response es la del catálogo WireMock, no `null` ni la del request. Agregar assertion `"coverageOptions[0].description" == "Incendio edificios"`. |
| R-006 | Documentar que el catálogo se consulta en tiempo real en cada PUT (sin caché). Si se implementa caché en el futuro, agregar test de invalidación. Por ahora, aceptar como riesgo de baja probabilidad. |
| R-007 | En `CoverageOptionsIntegrationTest`, tras un PUT exitoso, verificar que el GET retorna `version` incrementada (version original + 1). Confirma que Hibernate invalida el snapshot tras la transacción. |

---

## Riesgos BAJO — Backlog

| ID    | Acción sugerida |
|-------|-----------------|
| R-008 | Verificar en `CoverageControllerTest` que `deductiblePercentage` y `coinsurancePercentage` serializan como número JSON (`2.0`) y no como string. Agregar `JsonPath` assertion sobre el tipo del campo. |
| R-009 | Agregar test explícito para PUT con `coverageOptions: []` — verificar que retorna 200 y que el GET posterior retorna lista vacía (reemplazo total hacia vacío). |

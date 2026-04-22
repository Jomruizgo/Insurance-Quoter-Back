# Matriz de Riesgos — Gestión de Ubicaciones (SPEC-004)

> Generado por `/risk-identifier` | Feature: `location-management`
> Spec: `SPEC-004` | Fecha: 2026-04-21

---

## Resumen

Total: 10 | Alto (A): 4 | Medio (S): 4 | Bajo (D): 2

---

## Detalle

| ID    | HU           | Descripción del Riesgo                                                                 | Factores                                              | Nivel | Testing       |
|-------|--------------|----------------------------------------------------------------------------------------|-------------------------------------------------------|-------|---------------|
| R-001 | HU-02 / HU-03| Operación `replaceAll` elimina todas las ubicaciones y reinserta; sin rollback parcial visible si falla a mitad de la transacción | Operación destructiva irrecuperable, integridad transaccional | **A** | Obligatorio |
| R-002 | HU-02 / HU-03| Versionado optimista manual: la verificación `version == command.version` corre ANTES de persistir; condición de carrera si dos requests concurrentes leen la misma versión simultáneamente antes de que ninguno haya incrementado | Concurrencia, integraciones, SLA | **A** | Obligatorio |
| R-003 | HU-02 / HU-03| Integración con core service (`GET /v1/zip-codes/{zipCode}`): si core no responde → alerta MISSING_ZIP_CODE incorrecta; si responde con datos parciales → enriquecimiento geográfico incompleto (`state`, `municipality`, `city`, `catastrophicZone`) | Integración con sistema externo | **A** | Obligatorio |
| R-004 | HU-02 / HU-03| Serialización JSONB de `guarantees` via `AttributeConverter` Jackson: deserialización silenciosa puede retornar lista vacía si el JSON en BD es inválido, disparando falsa alerta `NO_TARIFABLE_GUARANTEES` | Lógica de negocio compleja, código nuevo sin historial | **A** | Obligatorio |
| R-005 | HU-03        | Merge parcial en PATCH: campo no enviado (`Optional.empty`) debe preservarse exactamente; un error de lógica en el merge puede silenciosamente borrar datos persistidos | Lógica compleja, alta frecuencia de uso | **S** | Recomendado |
| R-006 | HU-02 / HU-03| `@ElementCollection` de `blockingAlerts`: Hibernate borra y reinserta la colección completa en cada save; si la transacción falla después del delete y antes del insert, la ubicación queda sin alertas (estado inconsistente) | Operación destructiva, integridad transaccional | **S** | Recomendado |
| R-007 | HU-04        | Routing ambiguo: `GET /v1/quotes/{folio}/locations/summary` vs `GET /v1/quotes/{folio}/locations/{index}`; si Spring MVC resuelve `/summary` como `index="summary"` → 400 o comportamiento inesperado | Código nuevo sin historial, lógica de routing | **S** | Recomendado |
| R-008 | HU-02 / HU-03| Recálculo de alertas siempre desde cero: si una alerta fue corregida pero otra regla falla simultáneamente, el resultado acumulado puede diferir del esperado por el agente | Lógica de negocio compleja, múltiples reglas que interactúan | **S** | Recomendado |
| R-009 | HU-04        | `GET /summary` no debe exponer `version`: si un mapper mapea todos los campos por defecto, la versión puede filtrarse inadvertidamente, exponiendo dato de control interno | Feature interna de proyección | **D** | Opcional |
| R-010 | HU-01        | `GET /locations` devuelve detalle completo incluyendo `guarantees` como array JSON; si la serialización incluye tipos numéricos incorrectos para `insuredValue` (`String` vs `Number`) → contrato roto con el frontend | Ajuste de serialización, sin cambio de lógica | **D** | Opcional |

---

## Plan de Mitigación — Riesgos ALTO

---

### R-001: Operación replaceAll destructiva sin rollback parcial

**Descripción:** `LocationJpaAdapter.replaceAll` ejecuta un DELETE masivo seguido de INSERTs. Si la transacción no está correctamente delimitada (`@Transactional` en el use case impl o en el adapter), un fallo entre el DELETE y el INSERT deja la cotización sin ubicaciones — dato irrecuperable sin respaldo.

**Mitigación:**
- Verificar que `replaceAll` corre dentro de una única transacción (`@Transactional` en el use case o en el JPA adapter).
- Test de integración que simula fallo en INSERT después de DELETE (forzando excepción) y verifica que el rollback restaura el estado previo.
- No usar `deleteAll + saveAll` en dos llamadas separadas fuera de un mismo `@Transactional`.

**Tests obligatorios:**
- `LocationManagementIntegrationTest` — transaction rollback: PUT que falla a mitad → GET posterior retorna las ubicaciones originales.
- `LocationJpaAdapterTest` — `replaceAll` ejecuta ambas operaciones en la misma transacción.

**Bloqueante para release:** ✅ Sí

---

### R-002: Condición de carrera en versionado optimista manual

**Descripción:** El use case lee la versión actual con `QuoteVersionRepository.findVersionByFolioNumber`, la compara con `command.version`, y solo entonces persiste. Si dos threads concurrentes superan la validación simultáneamente (ambos leen version=4, ambos validan OK), el segundo save puede sobrescribir al primero sin disparar 409.

**Mitigación:**
- La comparación manual es una pre-validación de UX (error más descriptivo). El control final de concurrencia lo garantiza `@Version` de Hibernate en `QuoteJpa` — el segundo save lanza `ObjectOptimisticLockingFailureException` que el `GlobalExceptionHandler` traduce a 409.
- Verificar que `@Version` en `QuoteJpa` esté correctamente mapeado y que `incrementVersion` use un `save()` que dispare el incremento de Hibernate.
- Test de integración con dos threads concurrentes sobre el mismo folio.

**Tests obligatorios:**
- `ReplaceLocationsUseCaseImplTest` — version conflict en check manual → `VersionConflictException`.
- `LocationManagementIntegrationTest` — dos PUTs concurrentes: primero OK (200), segundo → 409.

**Bloqueante para release:** ✅ Sí

---

### R-003: Integración con core service inestable

**Descripción:** `ZipCodeValidationClientAdapter` llama `GET /v1/zip-codes/{zipCode}` al core (puerto 8081). Si el core no responde (timeout, connection refused) el adapter debe retornar `Optional.empty()` para que `LocationValidationService` genere `MISSING_ZIP_CODE` — sin lanzar excepción que rompa el flujo del PUT/PATCH. Un manejo incorrecto de la excepción de red puede propagar un 500 al agente en lugar de generar la alerta correspondiente.

**Mitigación:**
- `ZipCodeValidationClientAdapter` captura `RestClientException` (timeout, connection refused) y retorna `Optional.empty()` — nunca propaga al use case.
- WireMock en tests de integración simula: (a) 404, (b) 200 válido, (c) timeout/connection refused.
- Documentar en `LocationConfig` el timeout configurado para el `RestClient` del core.

**Tests obligatorios:**
- `ZipCodeValidationClientAdapterTest` — 404 del core → `Optional.empty()`.
- `ZipCodeValidationClientAdapterTest` — timeout/connection refused → `Optional.empty()` (no excepción).
- `LocationManagementIntegrationTest` — WireMock 404 para zipCode → respuesta 200 con alerta `MISSING_ZIP_CODE`.

**Bloqueante para release:** ✅ Sí

---

### R-004: Serialización JSONB de guarantees con AttributeConverter

**Descripción:** `GuaranteesConverter` usa Jackson para serializar/deserializar `List<Guarantee>` hacia/desde `JSONB`. Si la columna `guarantees` contiene JSON malformado (migración manual, bug en versión anterior), la deserialización puede retornar `null` o lista vacía en lugar de lanzar excepción, disparando falsamente `NO_TARIFABLE_GUARANTEES` en ubicaciones que sí tenían guarantees.

**Mitigación:**
- `GuaranteesConverter.convertToEntityAttribute` debe retornar lista vacía (no null) ante JSON inválido, y loguear el error con el `location_id` para auditoría.
- Test unitario que pase JSON malformado al converter y verifique comportamiento controlado.
- Script de validación de integridad de datos en BD para verificar que `guarantees` en filas existentes es JSON válido (ejecutar post-migración V5).

**Tests obligatorios:**
- `GuaranteesConverterTest` — JSON válido → lista correcta; JSON inválido → lista vacía + log de error.
- `LocationManagementIntegrationTest` — round-trip: PUT con guarantees → GET retorna mismas guarantees con tipos correctos.

**Bloqueante para release:** ✅ Sí

---

## Riesgos MEDIO — Acciones Recomendadas

| ID    | Acción recomendada |
|-------|--------------------|
| R-005 | Test exhaustivo del merge parcial en `PatchLocationUseCaseImplTest`: enviar body con 1 campo → verificar que los otros N-1 campos no cambian. Cubrir también `Optional<null>` vs campo ausente. |
| R-006 | Verificar que `@Transactional` envuelve todo el ciclo de `@ElementCollection` (delete+insert). Revisar que `CascadeType` y `orphanRemoval` en `@ElementCollection` estén configurados para limpiar alertas huérfanas. |
| R-007 | Test de controller (`LocationControllerTest`) que llama `GET /v1/quotes/{folio}/locations/summary` y verifica que el handler correcto responde (no el de `/{index}`). Verificar orden de `@GetMapping` en el controller. |
| R-008 | Test parametrizado en `LocationValidationServiceTest` con todas las combinaciones de alertas (7 casos cubiertos por TDD). Agregar caso con múltiples alertas simultáneas para verificar acumulación correcta. |

---

## Riesgos BAJO — Backlog

| ID    | Acción sugerida |
|-------|-----------------|
| R-009 | Agregar test de snapshot del response de `GET /summary` para detectar si campos inesperados se filtran. |
| R-010 | Verificar en `LocationControllerTest` que `insuredValue` en el response de guarantees serializa como número JSON (no string). |

# Matriz de Riesgos — Quote State (SPEC-005)

**Feature:** Consulta del estado y progreso de completitud de una cotización  
**Endpoint:** `GET /v1/quotes/{folio}/state`  
**Evaluado:** 2026-04-21

---

## Resumen

Total: 6 | Alto (A): 0 | Medio (S): 3 | Bajo (D): 3

> **Nota:** El endpoint es estrictamente read-only. No maneja pagos, datos personales sensibles,
> operaciones destructivas ni integraciones externas. Ningún riesgo alcanza nivel ALTO.
> El riesgo principal está en la corrección de la lógica de evaluación por sección.

---

## Detalle

| ID    | HU     | Descripción del Riesgo                                              | Factores                                         | Nivel | Testing     |
|-------|--------|---------------------------------------------------------------------|--------------------------------------------------|-------|-------------|
| R-001 | HU-01  | `completionPercentage` calculado incorrectamente                    | Lógica compleja, código nuevo, alta frecuencia   | S     | Recomendado |
| R-002 | HU-01  | Evaluación de sección `locations` devuelve estado erróneo           | Lógica compleja, dependencia cross-BC            | S     | Recomendado |
| R-003 | HU-01  | Ruptura silenciosa al cambiar schema de location BC sin actualizar  | Componentes con muchas dependencias              | S     | Recomendado |
| R-004 | HU-01  | Regla "CALCULATED → 100%" oculta inconsistencias reales             | Lógica de negocio con caso especial              | D     | Opcional    |
| R-005 | HU-01  | SPEC-006/007 implementadas sin actualizar evaluación de secciones   | Código nuevo sin historial, dependencias futuras | D     | Opcional    |
| R-006 | HU-01  | `FolioNotFoundException` no propagada correctamente → 500 en vez de 404 | Código nuevo sin historial                  | D     | Opcional    |

---

## Plan de Mitigación — Riesgos MEDIO (S)

### R-001: `completionPercentage` calculado incorrectamente
- **Escenario:** `GetQuoteStateUseCaseImpl.computePercentage()` usa división entera incorrecta o cuenta mal las secciones COMPLETE.
- **Mitigación:** Test unitario cubre cada combinación: 0/5, 1/5, 2/5, 3/5, 4/5, 5/5 y el caso especial CALCULATED→100.
- **Tests obligatorios:**
  - `GetQuoteStateUseCaseImplTest` — 5 casos ya implementados (TDD GREEN)
  - Validación manual: `curl GET /v1/quotes/{folio}/state` con folio en estado conocido
- **Bloqueante para release:** ✅ Sí (mal progreso lleva al agente a tomar decisiones incorrectas)

### R-002: Evaluación de sección `locations` devuelve estado erróneo
- **Escenario:** `LocationStateJpaAdapter` cuenta incorrectamente `completeCount` vs `incompleteCount`, causando que una ubicación con alertas se muestre como COMPLETE.
- **Mitigación:** Test unitario cubre: sin ubicaciones, todas COMPLETE, mixtas (1 COMPLETE + 1 INCOMPLETE con alerts).
- **Tests obligatorios:**
  - `LocationStateJpaAdapterTest` — 3 casos ya implementados (TDD GREEN)
  - `GetQuoteStateUseCaseImplTest` — verifica que INCOMPLETE > 0 → sección INCOMPLETE
- **Bloqueante para release:** ✅ Sí (agente podría intentar calcular con ubicaciones sin datos válidos)

### R-003: Ruptura silenciosa al cambiar schema de location BC
- **Escenario:** Se renombra `validationStatus` o se modifica `BlockingAlertEmbeddable` en location BC y `LocationStateJpaAdapter` (folio BC) falla sin compilar o con NullPointerException en runtime.
- **Mitigación:** `LocationStateJpaAdapter` usa directamente `LocationJpa.getValidationStatus()` y `LocationJpa.getBlockingAlerts()`. Si el campo se renombra, el compilador detecta el error. El riesgo real es si se cambia el valor de la cadena "COMPLETE".
- **Controls técnicos:**
  - Constante compartida para `"COMPLETE"` en lugar de string literal inline (backlog)
  - Test de integración cross-BC en suite Serenity BDD
- **Tests obligatorios:** `LocationStateJpaAdapterTest` — verifica comparación de string "COMPLETE"
- **Bloqueante para release:** ⚠️ Parcial — solo si se modifica location BC en la misma release

---

## Riesgos BAJO (D) — Documentados, no bloqueantes

### R-004: Regla CALCULATED → 100% oculta inconsistencias
- **Escenario:** Un folio llega a CALCULATED con secciones que el sistema aún marca INCOMPLETE. La regla fuerza 100% y el agente no ve la inconsistencia.
- **Mitigación:** La regla está documentada en la spec (RN-12). Es comportamiento intencional: si el sistema calculó, el flujo se considera completo.
- **Acción:** Documentar en README del módulo como decisión de diseño.

### R-005: SPEC-006/007 implementadas sin actualizar evaluación
- **Escenario:** Se implementa general-info (SPEC-006) y las columnas `insured_name`, `insured_rfc` etc. existen en DB pero `GetQuoteStateUseCaseImpl` sigue devolviendo `PENDING` hardcoded.
- **Mitigación:** `GetQuoteStateUseCaseImpl` tiene comentarios explícitos `// requires SPEC-006` y `// requires SPEC-007`. Al implementar esas specs, el desarrollador debe actualizar este use case. Agregar como criterio de aceptación en SPEC-006 y SPEC-007.

### R-006: FolioNotFoundException no propagada → 500
- **Escenario:** `QuoteStateJpaAdapter` no lanza `FolioNotFoundException` y devuelve `null`, causando NPE en el use case que el GlobalExceptionHandler traduce como 500.
- **Mitigación:** `QuoteStateController` test verifica que folio inexistente → 404 con código `FOLIO_NOT_FOUND`. Implementado y pasando (TDD GREEN).

---

## Cobertura de tests implementados

| Test | Criterio cubierto | Estado |
|------|-------------------|--------|
| `GetQuoteStateUseCaseImplTest` (5 casos) | R-001, R-002 | ✅ GREEN |
| `LocationStateJpaAdapterTest` (3 casos) | R-002, R-003 | ✅ GREEN |
| `QuoteStateControllerTest` (2 casos) | R-006 | ✅ GREEN |
| `QuoteStateRestMapperTest` (1 caso) | — | ✅ GREEN |
| Validación manual live | R-001, R-002 | ⏳ Pendiente |

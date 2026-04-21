# Matriz de Riesgos — SPEC-003: Configuración de Layout de Ubicaciones

**Feature:** `location-layout`
**Spec:** `.claude/specs/location-layout.spec.md`
**Fecha de análisis:** 2026-04-21
**Analista:** risk-identifier (ASD Rule)

---

## Resumen Ejecutivo

| Nivel | Cantidad | Acción requerida |
|-------|----------|------------------|
| **Alto (A)** | 5 | Testing OBLIGATORIO — bloquea release |
| **Medio (S)** | 4 | Testing RECOMENDADO — documentar si se omite |
| **Bajo (D)** | 2 | Testing OPCIONAL — priorizar en backlog |
| **Total** | **11** | |

---

## Detalle de Riesgos

| ID | HU | Descripción del Riesgo | Factores ASD | Nivel | Testing |
|----|-----|------------------------|--------------|-------|---------|
| R-001 | HU-02 | La lógica de sincronización de ubicaciones (aumentar / reducir / igual) aplica transformaciones sobre colecciones persistidas. Un error en la lógica puede corromper datos de cotizaciones existentes o duplicar ubicaciones. | Lógica de negocio compleja con múltiples ramas + operación irreversible (datos ya capturados en ubicaciones se perdería si se eliminan en lugar de desactivar) | **A** | Obligatorio |
| R-002 | HU-02 | Regla de no-eliminación de filas (soft delete via `active=false`). Si se implementa un `DELETE` en lugar de `UPDATE active=false`, los datos de ubicaciones ya ingresados por el usuario se pierden de forma irrecuperable. | Operación destructiva irrecuperable + regla de negocio explícita | **A** | Obligatorio |
| R-003 | HU-02 | Conflicto de versión optimista (`@Version` sobre `QuoteJpa`). Bajo acceso concurrente (dos agentes editando el mismo folio simultáneamente), uno debe recibir 409. Si el manejo de `ObjectOptimisticLockingFailureException` es incorrecto, la respuesta puede ser 500 o silenciar el conflicto, causando pérdida silenciosa de datos. | Concurrencia — optimistic lock + integridad de datos entre sesiones | **A** | Obligatorio |
| R-004 | HU-02 | Atomicidad de la transacción entre `QuoteJpa` (update de columnas layout) y `LocationJpa` (inserts/updates). Si `@Transactional` no está correctamente configurado en el use case, es posible guardar el layout en `QuoteJpa` sin insertar las `LocationJpa` correspondientes, o viceversa, dejando el sistema en estado inconsistente. | Lógica de negocio compleja + múltiples entidades en una misma transacción | **A** | Obligatorio |
| R-005 | HU-02 | Constraint único `UK_locations_quote_index` (quote_id, index). Al aumentar `numberOfLocations`, si el cálculo del índice de las nuevas ubicaciones es incorrecto y colisiona con uno existente (incluso inactivo), la operación lanzará una violación de constraint en BD, resultando en 500 en lugar de un error controlado. | Integridad referencial + lógica de cálculo de índices | **A** | Obligatorio |
| R-006 | HU-01 | Cotización sin `layoutConfiguration` configurado debe retornar campos nulos en lugar de un error (CRITERIO-1.3). Si el mapper o el use case no maneja el caso null correctamente, se obtiene NullPointerException o un 500 inesperado. | Código nuevo sin historial + edge case de nulos | **S** | Recomendado |
| R-007 | HU-02 | Validación de la regla de negocio: si `locationType = SINGLE`, entonces `numberOfLocations` debe ser exactamente 1. Esta validación cruzada entre dos campos no es cubierta por anotaciones Bean Validation estándar y requiere lógica custom; es probable que sea omitida o incompleta. | Lógica de negocio compleja — validación cruzada entre campos | **S** | Recomendado |
| R-008 | HU-02 | Rango de `numberOfLocations` (1–50). Si el límite superior no se valida, un agente podría enviar 1000 ubicaciones y generar 1000 inserts en una sola transacción, degradando el rendimiento de la BD y causando timeouts o errores de memoria. | Funcionalidad de alta frecuencia de uso + ausencia de límite superior en validación | **S** | Recomendado |
| R-009 | HU-01 / HU-02 | El `QuoteJpaRepository` ya existe en el context `folio`. Reutilizarlo en el context `location` (via `QuoteLayoutJpaAdapter`) introduce un acoplamiento entre bounded contexts. Un cambio en el context `folio` puede romper la lógica de layout sin aviso. | Componente con muchas dependencias — shared JPA repository entre contextos | **S** | Recomendado |
| R-010 | HU-02 | Las migraciones Flyway V3 y V4 modifican tablas existentes (`quotes`) y crean una nueva tabla con FK. Si se ejecutan fuera de orden o en un entorno con datos existentes, pueden fallar con errores de columna duplicada o FK violation. | Feature nueva sin historial + dependencia de estado previo de BD | **D** | Opcional |
| R-011 | HU-01 / HU-02 | Los endpoints no tienen autenticación (`Auth requerida: no` en la spec). Si en el futuro se añade auth, todos los tests de integración y contratos actuales deberán actualizarse. Esto no es un riesgo funcional inmediato pero genera deuda técnica. | Feature interna / administrativa + sin impacto en lógica actual | **D** | Opcional |

---

## Plan de Mitigación — Riesgos ALTO

### R-001: Lógica de sincronización de ubicaciones

- **Descripcion completa:** `SaveLocationLayoutUseCaseImpl` tiene tres ramas lógicas (aumentar, reducir, sin cambio). Cada rama manipula listas de `LocationJpa` y debe preservar datos existentes.
- **Mitigacion tecnica:**
  - Separar cada rama en método privado con nombre descriptivo.
  - Usar `findByQuoteIdAndActiveTrue` para contar solo activas (no inactivas previas).
  - Calcular el índice de nuevas ubicaciones como `currentActive.size() + i + 1`.
- **Tests obligatorios:**
  - `SaveLocationLayoutUseCaseImplTest`: escenarios CRITERIO-2.1 (primera vez), CRITERIO-2.2 (aumentar), CRITERIO-2.3 (reducir), caso "sin cambio en cantidad".
  - `SaveLocationLayoutIntegrationTest` con Testcontainers: verificar el estado real de la BD para los cuatro escenarios.
- **Cobertura mínima requerida:** 100% de las ramas del método de sincronización.
- **Bloqueante para release:** Si

---

### R-002: Regla de no-eliminación (soft delete)

- **Descripcion completa:** La regla de negocio explícita prohíbe `DELETE FROM locations`. Solo se permite `UPDATE active = false`. Un error de implementación (usar `delete()` de JPA) destruye datos irrecuperablemente.
- **Mitigacion tecnica:**
  - El adaptador `LocationJpaAdapter` no debe exponer ni llamar métodos `delete*` del `LocationJpaRepository`.
  - Revisar que `LocationJpaRepository` no tenga `deleteBy*` ni métodos que llamen `deleteAll`.
  - Añadir test de integración que verifique que después de reducir `numberOfLocations`, el COUNT total de filas en `locations` no decrece.
- **Tests obligatorios:**
  - `SaveLocationLayoutIntegrationTest` CRITERIO-2.3: verificar `SELECT COUNT(*) FROM locations WHERE quote_id = X` antes y después — debe ser igual.
  - `LocationJpaAdapterTest`: verificar que el adaptador solo llama `save`/`saveAll`, nunca `delete`.
- **Bloqueante para release:** Si

---

### R-003: Conflicto de versión optimista (concurrencia)

- **Descripcion completa:** `ObjectOptimisticLockingFailureException` debe ser capturada en `GlobalExceptionHandler` y traducida a HTTP 409. Si no se registra o se captura en el handler equivocado, el cliente recibe 500 (o peor, 200 con datos corruptos si se hace retry silencioso).
- **Mitigacion tecnica:**
  - Registrar `ObjectOptimisticLockingFailureException` en `GlobalExceptionHandler` con respuesta `{ "error": "Optimistic lock conflict", "code": "VERSION_CONFLICT" }` y status 409.
  - No hacer retry automático en el handler — dejar que el cliente reintente con la versión correcta.
- **Tests obligatorios:**
  - `LocationLayoutControllerTest`: mock del use case lanzando `ObjectOptimisticLockingFailureException` → verificar respuesta 409 con body correcto.
  - `SaveLocationLayoutIntegrationTest` CRITERIO-2.4: dos transacciones concurrentes sobre el mismo folio → la segunda recibe 409.
- **Bloqueante para release:** Si

---

### R-004: Atomicidad de la transacción

- **Descripcion completa:** `SaveLocationLayoutUseCaseImpl` debe tener `@Transactional`. Si falta, un fallo en el `saveAll` de `LocationJpa` después del `save` de `QuoteJpa` deja el sistema con el layout actualizado pero sin las ubicaciones correspondientes.
- **Mitigacion tecnica:**
  - Anotar `SaveLocationLayoutUseCaseImpl.saveLayout(...)` con `@Transactional`.
  - Verificar que el `@Transactional` sea de Spring (no JPA) y que la propagación sea `REQUIRED` (default).
  - En `LocationLayoutConfig`, asegurarse de que el use case es un bean Spring gestionado (no instanciado con `new`).
- **Tests obligatorios:**
  - `SaveLocationLayoutIntegrationTest`: simular fallo en `saveAll` (inyectando un mock que lanza excepción tras el save de `QuoteJpa`) y verificar rollback — el `number_of_locations` en `quotes` no debe haber cambiado.
- **Bloqueante para release:** Si

---

### R-005: Violación de constraint único en índices de ubicación

- **Descripcion completa:** `UK_locations_quote_index (quote_id, index)` aplica sobre todas las filas, incluyendo inactivas. Al re-activar ubicaciones o al calcular índices para nuevas filas, si el índice calculado coincide con una fila inactiva existente, se produce `DataIntegrityViolationException`.
- **Mitigacion tecnica:**
  - Al calcular el índice de nuevas ubicaciones, usar `findByQuoteId` (todas, activas e inactivas) para obtener el máximo índice existente y partir de ahí.
  - Alternativamente, en el caso de reducir y volver a aumentar, re-activar las filas inactivas en orden antes de insertar nuevas.
  - Documentar en el código la decisión tomada sobre el manejo de inactivas al re-expandir.
- **Tests obligatorios:**
  - `SaveLocationLayoutIntegrationTest`: flujo reducir 4→2 seguido de aumentar 2→4 — verificar que no hay violación de constraint y que las 4 ubicaciones quedan con `active=true`.
  - `LocationJpaAdapterTest`: verificar que `saveAll` no genera índices duplicados en el escenario anterior.
- **Bloqueante para release:** Si

---

## Cobertura de Criterios de Aceptacion por Nivel de Riesgo

| Criterio | HU | Riesgo relacionado | Nivel | Cobertura requerida |
|----------|----|--------------------|-------|---------------------|
| CRITERIO-1.1 | HU-01 | R-006 | S | Test unitario + controller test |
| CRITERIO-1.2 | HU-01 | — | S | Controller test (404) |
| CRITERIO-1.3 | HU-01 | R-006 | S | Test unitario (nulos) |
| CRITERIO-2.1 | HU-02 | R-001, R-004, R-005 | **A** | Use case test + integración |
| CRITERIO-2.2 | HU-02 | R-001, R-004, R-005 | **A** | Use case test + integración |
| CRITERIO-2.3 | HU-02 | R-001, R-002, R-004 | **A** | Use case test + integración + count rows |
| CRITERIO-2.4 | HU-02 | R-003 | **A** | Controller test + integración concurrente |
| CRITERIO-2.5 | HU-02 | — | S | Controller test (404) |
| CRITERIO-2.6 | HU-02 | R-007, R-008 | S | Controller test (422) + validación cruzada |

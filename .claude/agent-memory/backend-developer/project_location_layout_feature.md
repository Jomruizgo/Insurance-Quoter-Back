---
name: location-layout feature — implementation state
description: SPEC-003 implementado y en verde. Decisiones de diseño clave del bounded context location.
type: project
---

SPEC-003 (location-layout) implementado completamente. Suite completa verde.

**Why:** Feature que permite configurar el layout de ubicaciones por cotización (numberOfLocations, locationType), sincronizando la tabla `locations` sin borrar filas.

**How to apply:** Consultar este registro al retomar trabajo relacionado con ubicaciones o al implementar SPEC-004+.

## Decisiones críticas de implementación

### Optimistic lock check manual en QuoteLayoutJpaAdapter
`QuoteLayoutJpaAdapter.save()` compara explícitamente `jpa.getVersion().equals(data.version())` antes de mutar la entidad y lanza `ObjectOptimisticLockingFailureException` manualmente si no coinciden.

**Motivo:** Hibernate ignora `setVersion()` en entidades managed — al cargar la entidad con `findById()` dentro de la misma sesión, el campo `@Version` queda bajo control de Hibernate y no puede ser sobreescrito para forzar el chequeo. La comparación manual es el único mecanismo confiable en este patrón sin transacciones anidadas.

### Test de integración sin @Transactional
`SaveLocationLayoutIntegrationTest` no usa `@Transactional` a nivel de clase. La limpieza se hace en `@BeforeEach` con `deleteAll()` (locations primero por FK, luego quotes).

**Motivo:** Con `@Transactional` todo el test corre en una sola transacción — los commits intermedios no ocurren y el chequeo de versión nunca se dispara, haciendo que el test 409 siempre pase como 200.

### LocationJpaAdapter.saveAll() — bifurcación insert vs deactivate
- Si todas las `Location` tienen `active=true` → bulk insert vía `saveAll`.
- Si alguna tiene `active=false` → deactivation path: busca por `findByQuoteIdAndIndexGreaterThan(quoteId, minIndex-1)` y hace `save()` individual por fila (para respetar el `@UpdateTimestamp`).

### GlobalExceptionHandler ampliado
Se agregaron handlers para:
- `FolioNotFoundException` → 404
- `ObjectOptimisticLockingFailureException` + `OptimisticLockingFailureException` → 409
- `IllegalArgumentException` → 422 (regla SINGLE con numberOfLocations != 1)

### QuoteJpaRepository.findByFolioNumber
Se agregó este método a `QuoteJpaRepository` existente (bounded context folio) para ser reutilizado por `QuoteLayoutJpaAdapter` sin duplicar el repositorio.

## Archivos clave creados/modificados

- `V3__add_layout_columns_to_quotes.sql` — columnas `number_of_locations`, `location_type` en `quotes`
- `V4__create_locations_table.sql` — tabla `locations` con FK, UNIQUE y índice
- `QuoteJpa` — campos `numberOfLocations`, `locationType` agregados
- `back.location` — bounded context completo (domain, application, infrastructure)
- `GlobalExceptionHandler` — handlers 404, 409, 422 nuevos

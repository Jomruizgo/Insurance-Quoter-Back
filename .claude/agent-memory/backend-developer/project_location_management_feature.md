---
name: location-management feature — implementation state
description: SPEC-004 implementado y todos los issues cerrados (#82-#118). Decisiones de diseño y archivos clave.
type: project
---

Feature location-management (SPEC-004) implementado completamente. Todos los issues #82-#118 cerrados (excepto #119-#125 que son QA).

**Why:** Feature de gestión completa de ubicaciones por folio: GET/PUT/PATCH/summary con validación de CP contra core service.

**How to apply:** Referencia de decisiones para features similares.

## Decisiones de diseño relevantes

- Input ports definen sus propios records internos (Command, Result) — no importan del paquete `application`. Esto cumple la regla de dependencia hexagonal (domain no depende de application).
- `@WebMvcTest` no existe en Spring Boot 4 → usar `MockMvcBuilders.standaloneSetup()` con `@ExtendWith(MockitoExtension.class)`, igual que `FolioControllerTest`.
- `guarantees` se serializa como TEXT/JSONB con `GuaranteesConverter implements AttributeConverter` + Jackson. Sin Hypersistence.
- `@ElementCollection` para `blockingAlerts` → tabla `location_blocking_alerts` con FK a `locations.id ON DELETE CASCADE`.
- `LocationJpaAdapter` ahora recibe `QuoteJpaRepository` por constructor para resolver folio→quoteId.
- `QuoteVersionJpaAdapter` hace `quoteJpaRepository.save(quote)` para que Hibernate incremente `@Version` automáticamente.
- El `@GetMapping("/summary")` se declara ANTES de `@GetMapping("/{index}")` en el controller para evitar conflicto de routing.
- `PatchLocationRequest` usa `Optional<T>` en todos los campos opcionales con normalización en el canonical constructor (null → Optional.empty()).
- `LocationValidationService` no es un `@Component` — se instancia como `@Bean` en `LocationConfig`.
- V5 usa `TEXT` en lugar de `JSONB` para `guarantees` en la migración porque JPA no necesita el tipo JSONB nativo (el converter maneja la serialización).

## Archivos clave creados/modificados

- `V5__add_location_detail_columns.sql`, `V6__create_location_blocking_alerts_table.sql`
- `location/domain/model/`: Location (reemplazado), BusinessLine, Guarantee, BlockingAlert, ValidationStatus, BlockingAlertCode, ZipCodeInfo, LocationSummary
- `location/domain/service/LocationValidationService`
- `location/domain/port/in/`: GetLocationsUseCase, ReplaceLocationsUseCase, PatchLocationUseCase, GetLocationsSummaryUseCase
- `location/domain/port/out/`: LocationRepository (extendido), ZipCodeValidationClient, QuoteVersionRepository
- `location/application/usecase/`: GetLocationsUseCaseImpl, ReplaceLocationsUseCaseImpl, PatchLocationUseCaseImpl, GetLocationsSummaryUseCaseImpl, VersionConflictException, LocationNotFoundException
- `location/infrastructure/adapter/out/persistence/`: LocationJpa (extendido), BlockingAlertEmbeddable, GuaranteesConverter, LocationJpaRepository (extendido), LocationPersistenceMapper (extendido), LocationJpaAdapter (extendido), QuoteVersionJpaAdapter
- `location/infrastructure/adapter/out/http/`: ZipCodeResponse, ZipCodeValidationClientAdapter
- `location/infrastructure/adapter/in/rest/`: LocationController, LocationApi, LocationRestMapper, todos los DTOs request/response
- `folio/infrastructure/adapter/in/rest/GlobalExceptionHandler` (extendido: VersionConflictException 409, LocationNotFoundException 404)
- `location/infrastructure/config/LocationConfig`

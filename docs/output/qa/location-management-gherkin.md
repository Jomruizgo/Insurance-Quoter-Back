# Escenarios Gherkin — Gestión de Ubicaciones (SPEC-004)

> Generado desde los criterios de aceptación CRITERIO-1.1…4.2
> Feature: `location-management` | Spec: `SPEC-004`

---

```gherkin
#language: es
Característica: Gestión de ubicaciones de cotización
  Como agente de seguros
  Quiero gestionar las ubicaciones de una cotización
  Para registrar los datos completos de cada inmueble y conocer su estado de validación

  Antecedentes:
    Dado que el sistema tiene un folio activo "FOL-2026-00042" con versión 4
    Y el core service responde en "http://localhost:8081"

  # ─────────────────────────────────────────────
  # HU-01: Consultar todas las ubicaciones
  # ─────────────────────────────────────────────

  @smoke @critico @HU-01 @CRITERIO-1.1
  Escenario: Listado exitoso de ubicaciones con detalle completo
    Dado que el folio "FOL-2026-00042" tiene dos ubicaciones registradas
      | index | locationName     | zipCode | businessLine.fireKey | validationStatus |
      | 1     | Bodega Principal | 06600   | FK-INC-01            | COMPLETE         |
      | 2     | Oficina Sur      | 44100   | FK-COM-02            | COMPLETE         |
    Cuando se consultan las ubicaciones del folio "FOL-2026-00042"
    Entonces la respuesta es HTTP 200
    Y el cuerpo contiene el folio "FOL-2026-00042"
    Y "locations" es un arreglo con 2 elementos
    Y cada ubicación incluye los campos: index, locationName, address, zipCode, state,
      municipality, neighborhood, city, constructionType, level, constructionYear,
      businessLine, guarantees, catastrophicZone, validationStatus y blockingAlerts
    Y "version" refleja la versión actual de la cotización

  @error-path @HU-01 @CRITERIO-1.2
  Escenario: Consulta de ubicaciones con folio inexistente retorna 404
    Dado que el folio "FOL-9999-00001" no existe en el sistema
    Cuando se consultan las ubicaciones del folio "FOL-9999-00001"
    Entonces la respuesta es HTTP 404
    Y el cuerpo contiene '{"error": "Folio not found", "code": "FOLIO_NOT_FOUND"}'

  @edge-case @HU-01 @CRITERIO-1.3
  Escenario: Consulta de ubicaciones cuando la cotización no tiene ubicaciones registradas
    Dado que el folio "FOL-2026-00042" existe pero no tiene ubicaciones registradas
    Cuando se consultan las ubicaciones del folio "FOL-2026-00042"
    Entonces la respuesta es HTTP 200
    Y "locations" es un arreglo vacío

  # ─────────────────────────────────────────────
  # HU-02: Reemplazar lista completa de ubicaciones
  # ─────────────────────────────────────────────

  @smoke @critico @HU-02 @CRITERIO-2.1
  Escenario: Reemplazo exitoso de ubicaciones sin alertas
    Dado que el folio "FOL-2026-00042" tiene versión 4
    Y el core service valida que el código postal "06600" es válido
      y retorna estado "Ciudad de México", municipio "Cuauhtémoc", zona catastrófica "ZONE_A"
    Cuando se reemplaza la lista de ubicaciones del folio "FOL-2026-00042" con versión 4
      | index | locationName     | zipCode | businessLine.code | businessLine.fireKey | guarantee.code | insuredValue |
      | 1     | Bodega Principal | 06600   | BL-001            | FK-INC-01            | GUA-FIRE       | 5000000      |
    Entonces la respuesta es HTTP 200
    Y "locations[0].validationStatus" es "COMPLETE"
    Y "locations[0].blockingAlerts" es vacío
    Y "version" es 5
    Y "updatedAt" tiene formato ISO-8601 UTC

  @happy-path @HU-02 @CRITERIO-2.2
  Escenario: Reemplazo con código postal inválido genera alerta MISSING_ZIP_CODE
    Dado que el folio "FOL-2026-00042" tiene versión 4
    Y el core service retorna 404 para el código postal "00000"
    Cuando se reemplaza la lista de ubicaciones con una ubicación que tiene zipCode "00000"
      y versión 4
    Entonces la respuesta es HTTP 200
    Y "locations[0].validationStatus" es "INCOMPLETE"
    Y "locations[0].blockingAlerts" contiene el código "MISSING_ZIP_CODE"
      con mensaje "Código postal requerido"

  @happy-path @HU-02 @CRITERIO-2.3
  Escenario: Reemplazo con businessLine sin clave incendio genera alerta MISSING_FIRE_KEY
    Dado que el folio "FOL-2026-00042" tiene versión 4
    Y una ubicación no tiene fireKey en su businessLine
    Cuando se reemplaza la lista de ubicaciones con esa ubicación y versión 4
    Entonces la respuesta es HTTP 200
    Y "locations[0].validationStatus" es "INCOMPLETE"
    Y "locations[0].blockingAlerts" contiene el código "MISSING_FIRE_KEY"
      con mensaje "Clave incendio requerida"

  @happy-path @HU-02 @CRITERIO-2.4
  Escenario: Reemplazo sin garantías tarifables genera alerta NO_TARIFABLE_GUARANTEES
    Dado que el folio "FOL-2026-00042" tiene versión 4
    Y una ubicación no tiene guarantees registradas
    Cuando se reemplaza la lista de ubicaciones con esa ubicación y versión 4
    Entonces la respuesta es HTTP 200
    Y "locations[0].validationStatus" es "INCOMPLETE"
    Y "locations[0].blockingAlerts" contiene el código "NO_TARIFABLE_GUARANTEES"
      con mensaje "Sin garantías tarifables"

  @happy-path @HU-02 @CRITERIO-2.4b
  Escenario: Reemplazo con guarantees ninguna tarifable también genera alerta
    Dado que el folio "FOL-2026-00042" tiene versión 4
    Y una ubicación tiene guarantees pero ninguna con tarifable igual a true
    Cuando se reemplaza la lista de ubicaciones con esa ubicación y versión 4
    Entonces la respuesta es HTTP 200
    Y "locations[0].blockingAlerts" contiene el código "NO_TARIFABLE_GUARANTEES"

  @error-path @HU-02 @CRITERIO-2.5
  Escenario: Conflicto de versión optimista en reemplazo retorna 409
    Dado que el folio "FOL-2026-00042" tiene versión actual 5
    Cuando se reemplaza la lista de ubicaciones con versión 4 (desactualizada)
    Entonces la respuesta es HTTP 409
    Y el cuerpo contiene '{"error": "Optimistic lock conflict", "code": "VERSION_CONFLICT"}'
    Y no se persiste ningún cambio

  @error-path @HU-02 @CRITERIO-2.6
  Escenario: Reemplazo de ubicaciones con folio inexistente retorna 404
    Dado que el folio "FOL-9999-00001" no existe en el sistema
    Cuando se reemplaza la lista de ubicaciones del folio "FOL-9999-00001"
    Entonces la respuesta es HTTP 404
    Y el cuerpo contiene '{"code": "FOLIO_NOT_FOUND"}'

  @edge-case @HU-02
  Escenario: Reemplazo con múltiples ubicaciones mezcla de completas e incompletas
    Dado que el folio "FOL-2026-00042" tiene versión 4
    Y el core service valida "06600" como válido
    Y el core service retorna 404 para "00000"
    Cuando se reemplaza la lista con dos ubicaciones
      | index | locationName     | zipCode | businessLine.fireKey |
      | 1     | Bodega Principal | 06600   | FK-INC-01            |
      | 2     | Almacén Inválido | 00000   | (ausente)            |
    Entonces la respuesta es HTTP 200
    Y "locations[0].validationStatus" es "COMPLETE"
    Y "locations[1].validationStatus" es "INCOMPLETE"
    Y "locations[1].blockingAlerts" contiene "MISSING_ZIP_CODE" y "MISSING_FIRE_KEY"

  # ─────────────────────────────────────────────
  # HU-03: Actualización parcial de una ubicación
  # ─────────────────────────────────────────────

  @smoke @critico @HU-03 @CRITERIO-3.1
  Escenario: PATCH aplica solo los campos enviados y preserva los demás
    Dado que el folio "FOL-2026-00042" tiene versión 5
    Y la ubicación con índice 1 tiene locationName "Bodega Norte", zipCode "06600"
      y constructionYear 1995
    Cuando se actualiza parcialmente la ubicación índice 1 con solo locationName "Almacén Central"
      y versión 5
    Entonces la respuesta es HTTP 200
    Y "location.locationName" es "Almacén Central"
    Y "location.zipCode" sigue siendo "06600"
    Y "location.constructionYear" sigue siendo 1995
    Y "version" es 6
    Y "blockingAlerts" se recalcula con el nuevo estado de la ubicación

  @happy-path @HU-03 @CRITERIO-3.2
  Escenario: PATCH con código postal válido elimina alerta MISSING_ZIP_CODE
    Dado que el folio "FOL-2026-00042" tiene versión 5
    Y la ubicación con índice 2 tiene blockingAlerts con "MISSING_ZIP_CODE"
    Y el core service valida que "06600" es válido
    Cuando se actualiza parcialmente la ubicación índice 2 con zipCode "06600" y versión 5
    Entonces la respuesta es HTTP 200
    Y "location.validationStatus" es "COMPLETE"
    Y "location.blockingAlerts" no contiene "MISSING_ZIP_CODE"
    Y "location.state", "location.municipality" y "location.city" se enriquecen con datos del core

  @error-path @HU-03 @CRITERIO-3.3
  Escenario: PATCH con índice de ubicación inexistente retorna 404
    Dado que el folio "FOL-2026-00042" tiene 2 ubicaciones con índices 1 y 2
    Cuando se actualiza parcialmente la ubicación con índice 5
    Entonces la respuesta es HTTP 404
    Y el cuerpo contiene '{"error": "Location index not found", "code": "LOCATION_NOT_FOUND"}'

  @error-path @HU-03 @CRITERIO-3.4
  Escenario: Conflicto de versión en actualización parcial retorna 409
    Dado que el folio "FOL-2026-00042" tiene versión actual 6
    Cuando se actualiza parcialmente la ubicación índice 1 con versión 4 (desactualizada)
    Entonces la respuesta es HTTP 409
    Y el cuerpo contiene '{"code": "VERSION_CONFLICT"}'

  @edge-case @HU-03
  Escenario: PATCH que corrige businessLine con fireKey elimina alerta MISSING_FIRE_KEY
    Dado que la ubicación índice 1 tiene alerta "MISSING_FIRE_KEY"
    Y el folio "FOL-2026-00042" tiene versión 5
    Cuando se actualiza parcialmente con businessLine que incluye fireKey "FK-INC-01" y versión 5
    Entonces la respuesta es HTTP 200
    Y "location.blockingAlerts" no contiene "MISSING_FIRE_KEY"

  @edge-case @HU-03
  Escenario: PATCH con zipCode inválido agrega alerta MISSING_ZIP_CODE
    Dado que la ubicación índice 1 está COMPLETE en el folio "FOL-2026-00042" con versión 5
    Y el core service retorna 404 para el código postal "99999"
    Cuando se actualiza parcialmente la ubicación índice 1 con zipCode "99999" y versión 5
    Entonces la respuesta es HTTP 200
    Y "location.validationStatus" es "INCOMPLETE"
    Y "location.blockingAlerts" contiene "MISSING_ZIP_CODE"

  # ─────────────────────────────────────────────
  # HU-04: Resumen de validación de ubicaciones
  # ─────────────────────────────────────────────

  @smoke @critico @HU-04 @CRITERIO-4.1
  Escenario: Resumen de validación muestra conteos correctos y solo campos de resumen
    Dado que el folio "FOL-2026-00042" tiene 3 ubicaciones
      | index | locationName     | validationStatus |
      | 1     | Bodega Principal | COMPLETE         |
      | 2     | Oficina Sur      | COMPLETE         |
      | 3     | Almacén Inválido | INCOMPLETE       |
    Y la ubicación 3 tiene alertas "MISSING_ZIP_CODE" y "MISSING_FIRE_KEY"
    Cuando se consulta el resumen de validación del folio "FOL-2026-00042"
    Entonces la respuesta es HTTP 200
    Y "totalLocations" es 3
    Y "completeLocations" es 2
    Y "incompleteLocations" es 1
    Y cada elemento de "locations" contiene solo: index, locationName, validationStatus, blockingAlerts
    Y ningún elemento de "locations" contiene: address, zipCode, constructionType,
      constructionYear, businessLine, guarantees, catastrophicZone, state, municipality
    Y "locations[2].blockingAlerts" contiene "MISSING_ZIP_CODE" y "MISSING_FIRE_KEY"

  @error-path @HU-04 @CRITERIO-4.2
  Escenario: Resumen de validación con folio inexistente retorna 404
    Dado que el folio "FOL-9999-00001" no existe en el sistema
    Cuando se consulta el resumen de validación del folio "FOL-9999-00001"
    Entonces la respuesta es HTTP 404
    Y el cuerpo contiene '{"code": "FOLIO_NOT_FOUND"}'

  @edge-case @HU-04
  Escenario: Resumen de validación sin ubicaciones registradas
    Dado que el folio "FOL-2026-00042" existe pero no tiene ubicaciones
    Cuando se consulta el resumen de validación del folio "FOL-2026-00042"
    Entonces la respuesta es HTTP 200
    Y "totalLocations" es 0
    Y "completeLocations" es 0
    Y "incompleteLocations" es 0
    Y "locations" es un arreglo vacío

  # ─────────────────────────────────────────────
  # Reglas de negocio transversales
  # ─────────────────────────────────────────────

  @edge-case @regla-negocio
  Escenario: blockingAlerts se recalcula desde cero en cada escritura
    Dado que la ubicación índice 1 tenía alerta "MISSING_ZIP_CODE"
    Y el folio "FOL-2026-00042" tiene versión 5
    Cuando se reemplaza la lista completa con la ubicación índice 1 ahora con zipCode válido,
      businessLine con fireKey y al menos una guarantee
    Entonces la respuesta es HTTP 200
    Y "locations[0].blockingAlerts" es vacío
    Y "locations[0].validationStatus" es "COMPLETE"

  @edge-case @regla-negocio
  Escenario: Enriquecimiento automático de datos geográficos al validar código postal
    Dado que el core service valida "44100" como válido
      y retorna estado "Jalisco", municipio "Guadalajara", ciudad "Guadalajara", zona catastrófica "ZONE_B"
    Cuando se reemplaza la lista con una ubicación con zipCode "44100"
    Entonces la respuesta es HTTP 200
    Y "locations[0].state" es "Jalisco"
    Y "locations[0].municipality" es "Guadalajara"
    Y "locations[0].city" es "Guadalajara"
    Y "locations[0].catastrophicZone" es "ZONE_B"
```

---

## Datos de Prueba Sintéticos

| Escenario | Campo | Valor válido | Valor inválido | Borde |
|-----------|-------|-------------|----------------|-------|
| Validación zipCode | `zipCode` | `"06600"` (CDMX) / `"44100"` (GDL) | `"00000"` (core 404) | `"99999"` (desconocido) |
| Versión optimista | `version` | versión actual del folio | versión anterior | `0` |
| Índice de ubicación | `index` | `1`, `2` (existentes) | `5` (inexistente) | `0` (fuera de rango) |
| fireKey | `businessLine.fireKey` | `"FK-INC-01"` | `null` / `""` | `" "` (espacio) |
| Guarantees tarifables | `guarantees` | `[{"code": "GUA-FIRE", "insuredValue": 5000000}]` | `[]` (vacío) | lista con tarifable=false |
| locationName (PATCH) | `locationName` | `"Almacén Central"` | — | `null` (no enviar) |

### Folios de prueba

| Folio | Estado | Versión | Uso |
|-------|--------|---------|-----|
| `FOL-2026-00042` | CREATED | 4 | Happy paths HU-01..04 |
| `FOL-2026-00099` | CREATED | 5 | Escenarios con version conflict |
| `FOL-9999-00001` | (inexistente) | — | Error paths 404 |

### Respuestas WireMock para core service

```json
// GET /v1/zip-codes/06600 → 200
{
  "zipCode": "06600",
  "state": "Ciudad de México",
  "municipality": "Cuauhtémoc",
  "city": "Ciudad de México",
  "catastrophicZone": "ZONE_A",
  "valid": true
}

// GET /v1/zip-codes/44100 → 200
{
  "zipCode": "44100",
  "state": "Jalisco",
  "municipality": "Guadalajara",
  "city": "Guadalajara",
  "catastrophicZone": "ZONE_B",
  "valid": true
}

// GET /v1/zip-codes/00000 → 404
// GET /v1/zip-codes/99999 → 404
```

---

## Cobertura por criterio

| Criterio | Escenario Gherkin | Etiquetas |
|----------|-------------------|-----------|
| CRITERIO-1.1 | Listado exitoso con detalle completo | `@smoke @critico @HU-01` |
| CRITERIO-1.2 | Folio inexistente en GET | `@error-path @HU-01` |
| CRITERIO-1.3 | GET sin ubicaciones → array vacío | `@edge-case @HU-01` |
| CRITERIO-2.1 | Reemplazo exitoso → COMPLETE | `@smoke @critico @HU-02` |
| CRITERIO-2.2 | zipCode inválido → MISSING_ZIP_CODE | `@happy-path @HU-02` |
| CRITERIO-2.3 | Sin fireKey → MISSING_FIRE_KEY | `@happy-path @HU-02` |
| CRITERIO-2.4 | Sin guarantees → NO_TARIFABLE_GUARANTEES | `@happy-path @HU-02` |
| CRITERIO-2.5 | Version conflict PUT → 409 | `@error-path @HU-02` |
| CRITERIO-2.6 | Folio inexistente en PUT | `@error-path @HU-02` |
| CRITERIO-3.1 | PATCH parcial preserva campos | `@smoke @critico @HU-03` |
| CRITERIO-3.2 | PATCH zipCode válido elimina alerta | `@happy-path @HU-03` |
| CRITERIO-3.3 | Índice inexistente → 404 | `@error-path @HU-03` |
| CRITERIO-3.4 | Version conflict PATCH → 409 | `@error-path @HU-03` |
| CRITERIO-4.1 | Summary: conteos y proyección reducida | `@smoke @critico @HU-04` |
| CRITERIO-4.2 | Folio inexistente en summary | `@error-path @HU-04` |

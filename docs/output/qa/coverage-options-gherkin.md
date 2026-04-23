# Escenarios Gherkin — Opciones de Cobertura de Cotización (SPEC-006)

> Generado desde los criterios de aceptación CRITERIO-1.1…2.7
> Feature: `coverage-options` | Spec: `SPEC-006`

---

```gherkin
#language: es
Característica: Gestión de opciones de cobertura de cotización
  Como agente de seguros
  Quiero gestionar las opciones de cobertura de una cotización
  Para registrar qué coberturas aplican con sus parámetros de deducible y coaseguro

  Antecedentes:
    Dado que el sistema tiene un folio activo "FOL-2026-00042" con versión 6
    Y el core service responde en "http://localhost:8081"
    Y el core service expone el catálogo de garantías con los codes "GUA-FIRE" y "GUA-THEFT"

  # ─────────────────────────────────────────────
  # HU-01: Consultar opciones de cobertura
  # ─────────────────────────────────────────────

  @smoke @critico @HU-01 @CRITERIO-1.1
  Escenario: Consulta exitosa de opciones de cobertura configuradas
    Dado que el folio "FOL-2026-00042" tiene dos opciones de cobertura persistidas
      | code      | description      | selected | deductiblePercentage | coinsurancePercentage |
      | GUA-FIRE  | Incendio edificios | true   | 2.0                  | 80.0                  |
      | GUA-THEFT | Robo             | false    | 5.0                  | 100.0                 |
    Cuando se consultan las opciones de cobertura del folio "FOL-2026-00042"
    Entonces la respuesta es HTTP 200
    Y el cuerpo contiene "folioNumber": "FOL-2026-00042"
    Y "coverageOptions" es un arreglo con 2 elementos
    Y cada opción incluye los campos: code, description, selected, deductiblePercentage, coinsurancePercentage
    Y "version" es 6

  @edge-case @HU-01 @CRITERIO-1.2
  Escenario: Consulta de cotización sin opciones de cobertura retorna lista vacía
    Dado que el folio "FOL-2026-00042" existe pero no tiene opciones de cobertura registradas
    Cuando se consultan las opciones de cobertura del folio "FOL-2026-00042"
    Entonces la respuesta es HTTP 200
    Y "coverageOptions" es un arreglo vacío
    Y "version" es 6

  @error-path @HU-01 @CRITERIO-1.3
  Escenario: Consulta de opciones de cobertura con folio inexistente retorna 404
    Dado que el folio "FOL-9999-00001" no existe en el sistema
    Cuando se consultan las opciones de cobertura del folio "FOL-9999-00001"
    Entonces la respuesta es HTTP 404
    Y el cuerpo contiene '{"error": "Folio not found", "code": "FOLIO_NOT_FOUND"}'

  # ─────────────────────────────────────────────
  # HU-02: Configurar opciones de cobertura
  # ─────────────────────────────────────────────

  @smoke @critico @HU-02 @CRITERIO-2.1
  Escenario: Configuración exitosa de coberturas con codes válidos del catálogo
    Dado que el folio "FOL-2026-00042" tiene versión 6
    Y el core service retorna el catálogo con "GUA-FIRE" (Incendio edificios) y "GUA-THEFT" (Robo)
    Cuando se configuran las opciones de cobertura del folio "FOL-2026-00042" con versión 6
      | code      | selected | deductiblePercentage | coinsurancePercentage |
      | GUA-FIRE  | true     | 2.0                  | 80.0                  |
      | GUA-THEFT | true     | 5.0                  | 100.0                 |
    Entonces la respuesta es HTTP 200
    Y "coverageOptions" refleja las dos coberturas enviadas con sus parámetros
    Y "version" es 7
    Y "updatedAt" tiene formato ISO-8601 UTC

  @happy-path @HU-02 @CRITERIO-2.2
  Escenario: Configuración con mix de coberturas seleccionadas y no seleccionadas
    Dado que el folio "FOL-2026-00042" tiene versión 6
    Y el catálogo del core contiene "GUA-FIRE" y "GUA-THEFT"
    Cuando se configuran las opciones con "GUA-FIRE" seleccionada y "GUA-THEFT" no seleccionada con versión 6
    Entonces la respuesta es HTTP 200
    Y "coverageOptions[0].selected" es verdadero para GUA-FIRE
    Y "coverageOptions[1].selected" es falso para GUA-THEFT
    Y "version" es 7

  @error-path @HU-02 @CRITERIO-2.3
  Escenario: Conflicto de versión optimista retorna 409
    Dado que el folio "FOL-2026-00042" tiene versión actual 7
    Cuando se configuran las opciones de cobertura del folio "FOL-2026-00042" con versión 6 (desactualizada)
    Entonces la respuesta es HTTP 409
    Y el cuerpo contiene '{"error": "Optimistic lock conflict", "code": "VERSION_CONFLICT"}'
    Y los datos de cobertura no se modifican

  @error-path @HU-02 @CRITERIO-2.4
  Escenario: Configuración rechazada por code de cobertura no existente en catálogo
    Dado que el catálogo del core NO contiene el code "COV-INVALID"
    Y el folio "FOL-2026-00042" tiene versión 6
    Cuando se configuran las opciones con una cobertura cuyo code es "COV-INVALID" y versión 6
    Entonces la respuesta es HTTP 422
    Y el cuerpo contiene "code": "VALIDATION_ERROR"
    Y "fields" identifica el code inválido

  @error-path @HU-02 @CRITERIO-2.5
  Escenario: Configuración rechazada por deductiblePercentage fuera de rango
    Dado que el folio "FOL-2026-00042" tiene versión 6
    Cuando se configuran las opciones con una cobertura que tiene deductiblePercentage 150.0 y versión 6
    Entonces la respuesta es HTTP 422
    Y el cuerpo contiene "code": "VALIDATION_ERROR"
    Y "fields" identifica deductiblePercentage como inválido

  @error-path @HU-02 @CRITERIO-2.6
  Escenario: Configuración rechazada por coinsurancePercentage fuera de rango
    Dado que el folio "FOL-2026-00042" tiene versión 6
    Cuando se configuran las opciones con una cobertura que tiene coinsurancePercentage -5.0 y versión 6
    Entonces la respuesta es HTTP 422
    Y el cuerpo contiene "code": "VALIDATION_ERROR"
    Y "fields" identifica coinsurancePercentage como inválido

  @error-path @HU-02 @CRITERIO-2.7
  Escenario: Configuración de coberturas con folio inexistente retorna 404
    Dado que el folio "FOL-9999-00001" no existe en el sistema
    Cuando se configuran las opciones de cobertura del folio "FOL-9999-00001" con cualquier body válido
    Entonces la respuesta es HTTP 404
    Y el cuerpo contiene "code": "FOLIO_NOT_FOUND"

  # ─────────────────────────────────────────────
  # Reglas de negocio transversales
  # ─────────────────────────────────────────────

  @edge-case @regla-negocio
  Escenario: La description en la respuesta proviene del catálogo del core, no del request
    Dado que el catálogo del core retorna "GUA-FIRE" con description "Incendio edificios"
    Y el folio "FOL-2026-00042" tiene versión 6
    Cuando se configuran opciones enviando solo code, selected y porcentajes (sin description) con versión 6
    Entonces la respuesta es HTTP 200
    Y "coverageOptions[0].description" es "Incendio edificios" (enriquecida desde el catálogo)

  @edge-case @regla-negocio
  Escenario: El PUT reemplaza la lista completa de coberturas
    Dado que el folio "FOL-2026-00042" tiene dos opciones de cobertura persistidas: "GUA-FIRE" y "GUA-THEFT"
    Y tiene versión 6
    Cuando se configura solo la cobertura "GUA-FIRE" con versión 6
    Entonces la respuesta es HTTP 200
    Y "coverageOptions" contiene únicamente "GUA-FIRE"
    Y "GUA-THEFT" ya no aparece en la respuesta
    Y "version" es 7

  @edge-case @regla-negocio
  Escenario: Valores límite válidos para porcentajes (0.0 y 100.0)
    Dado que el folio "FOL-2026-00042" tiene versión 6
    Y el catálogo contiene "GUA-FIRE"
    Cuando se configura "GUA-FIRE" con deductiblePercentage 0.0 y coinsurancePercentage 100.0 y versión 6
    Entonces la respuesta es HTTP 200
    Y "coverageOptions[0].deductiblePercentage" es 0.0
    Y "coverageOptions[0].coinsurancePercentage" es 100.0

  @edge-case @regla-negocio
  Escenario: Valores justo fuera de rango rechazados con 422
    Dado que el folio "FOL-2026-00042" tiene versión 6
    Cuando se configura una cobertura con deductiblePercentage 100.01 y versión 6
    Entonces la respuesta es HTTP 422
    Y el cuerpo contiene "code": "VALIDATION_ERROR"

  @edge-case @regla-negocio
  Esquema del escenario: Validar rangos de porcentajes
    Dado que el folio "FOL-2026-00042" tiene versión 6
    Cuando se configura una cobertura con <campo> igual a <valor>
    Entonces la respuesta es HTTP <http_status>
    Ejemplos:
      | campo                  | valor  | http_status |
      | deductiblePercentage   | -0.01  | 422         |
      | deductiblePercentage   | 0.0    | 200         |
      | deductiblePercentage   | 50.0   | 200         |
      | deductiblePercentage   | 100.0  | 200         |
      | deductiblePercentage   | 100.01 | 422         |
      | coinsurancePercentage  | -5.0   | 422         |
      | coinsurancePercentage  | 0.0    | 200         |
      | coinsurancePercentage  | 100.0  | 200         |
      | coinsurancePercentage  | 150.0  | 422         |
```

---

## Datos de Prueba Sintéticos

| Escenario | Campo | Valor válido | Valor inválido | Borde |
|-----------|-------|-------------|----------------|-------|
| Código de cobertura | `code` | `"GUA-FIRE"`, `"GUA-THEFT"` | `"COV-INVALID"` | `""` (vacío) |
| Versión optimista | `version` | versión actual del folio | versión anterior (stale) | `0` |
| Porcentaje deducible | `deductiblePercentage` | `2.0`, `50.0` | `-0.01`, `150.0` | `0.0`, `100.0` |
| Porcentaje coaseguro | `coinsurancePercentage` | `80.0`, `100.0` | `-5.0`, `200.0` | `0.0`, `100.0` |
| Selección cobertura | `selected` | `true`, `false` | — | ambas en mismo request |

### Folios de prueba

| Folio | Estado | Versión inicial | Uso |
|-------|--------|-----------------|-----|
| `FOL-2026-00042` | CREATED | 6 | Happy paths HU-01..02 |
| `FOL-2026-00099` | CREATED | 7 | Escenarios con version conflict |
| `FOL-9999-00001` | (inexistente) | — | Error paths 404 |

### Respuestas WireMock para core service

```json
// GET /v1/catalogs/guarantees → 200 (catálogo estándar)
{
  "guarantees": [
    { "code": "GUA-FIRE",  "description": "Incendio edificios" },
    { "code": "GUA-THEFT", "description": "Robo" }
  ]
}

// GET /v1/catalogs/guarantees → 200 (catálogo reducido — solo GUA-FIRE)
{
  "guarantees": [
    { "code": "GUA-FIRE", "description": "Incendio edificios" }
  ]
}

// GET /v1/catalogs/guarantees → 503 (core no disponible)
HTTP 503 Service Unavailable
```

---

## Cobertura por criterio

| Criterio | Escenario Gherkin | Etiquetas |
|----------|-------------------|-----------|
| CRITERIO-1.1 | Consulta exitosa con dos coberturas configuradas | `@smoke @critico @HU-01` |
| CRITERIO-1.2 | GET sin opciones configuradas → array vacío | `@edge-case @HU-01` |
| CRITERIO-1.3 | Folio inexistente en GET → 404 | `@error-path @HU-01` |
| CRITERIO-2.1 | PUT exitoso con codes válidos → 200 y version +1 | `@smoke @critico @HU-02` |
| CRITERIO-2.2 | PUT con mix selected true/false → persiste ambos | `@happy-path @HU-02` |
| CRITERIO-2.3 | Version conflict PUT → 409 | `@error-path @HU-02` |
| CRITERIO-2.4 | Code no existe en catálogo → 422 VALIDATION_ERROR | `@error-path @HU-02` |
| CRITERIO-2.5 | deductiblePercentage > 100 → 422 VALIDATION_ERROR | `@error-path @HU-02` |
| CRITERIO-2.6 | coinsurancePercentage < 0 → 422 VALIDATION_ERROR | `@error-path @HU-02` |
| CRITERIO-2.7 | Folio inexistente en PUT → 404 | `@error-path @HU-02` |
| Regla 6 | PUT reemplaza lista completa (no merge) | `@edge-case @regla-negocio` |
| Regla 7 | description enriquecida desde catálogo, no del request | `@edge-case @regla-negocio` |
| Borde | Valores límite 0.0 y 100.0 son válidos | `@edge-case @regla-negocio` |

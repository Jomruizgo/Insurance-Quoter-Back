# Gherkin — SPEC-003: Configuración de Layout de Ubicaciones

**Spec:** `.claude/specs/location-layout.spec.md`
**Generado:** 2026-04-21
**Cobertura:** CRITERIO-1.1 al CRITERIO-2.6 + edge cases de reglas de negocio

---

## Datos de Prueba

| Escenario | Campo | Valor válido | Valor inválido | Valor borde |
|-----------|-------|-------------|----------------|-------------|
| Folio existente | `folioNumber` | `FOL-2026-00042` | `FOL-9999-99999` | — |
| Folio sin layout | `folioNumber` | `FOL-2026-00099` | — | — |
| Número de ubicaciones | `numberOfLocations` | `3` | `0`, `-1` | `1`, `50` |
| Número de ubicaciones | `numberOfLocations` | `3` | `51` | `50` |
| Tipo de ubicación | `locationType` | `MULTIPLE`, `SINGLE` | `OTRO`, `""` | — |
| SINGLE + cantidad | `locationType` + `numberOfLocations` | `SINGLE` + `1` | `SINGLE` + `3` | — |
| Versión | `version` | `3` (actual) | `2` (desactualizada) | — |

---

```gherkin
#language: es
Característica: Configuración de layout de ubicaciones de una cotización
  Como agente de seguros
  Quiero consultar y definir el número y tipo de ubicaciones de una cotización
  Para que el sistema prepare la estructura de ubicaciones antes de capturar su detalle

  Antecedentes:
    Dado que el sistema está en funcionamiento
    Y la base de datos está disponible

  # ===========================================================================
  # HU-01: Consultar layout de ubicaciones
  # ===========================================================================

  @smoke @critico
  Escenario: CRITERIO-1.1 — Obtener layout de cotización con configuración existente
    Dado que existe la cotización con folio "FOL-2026-00042"
    Y dicha cotización tiene configuradas 3 ubicaciones de tipo "MULTIPLE"
    Y la cotización está en la versión 3
    Cuando el agente consulta el layout de ubicaciones del folio "FOL-2026-00042"
    Entonces el sistema responde con código 200
    Y la respuesta incluye el folio "FOL-2026-00042"
    Y la respuesta incluye "numberOfLocations" igual a 3
    Y la respuesta incluye "locationType" igual a "MULTIPLE"
    Y la respuesta incluye "version" igual a 3

  @regression
  Escenario: CRITERIO-1.2 — Consulta de folio inexistente retorna 404
    Dado que no existe ninguna cotización con folio "FOL-9999-99999"
    Cuando el agente consulta el layout de ubicaciones del folio "FOL-9999-99999"
    Entonces el sistema responde con código 404
    Y la respuesta contiene el error "Folio not found"
    Y la respuesta contiene el código de error "FOLIO_NOT_FOUND"

  @regression
  Escenario: CRITERIO-1.3 — Cotización recién creada devuelve layout con valores nulos
    Dado que existe la cotización con folio "FOL-2026-00099" recién creada
    Y dicha cotización no tiene configuración de layout
    Cuando el agente consulta el layout de ubicaciones del folio "FOL-2026-00099"
    Entonces el sistema responde con código 200
    Y la respuesta incluye el folio "FOL-2026-00099"
    Y la respuesta incluye "numberOfLocations" con valor nulo
    Y la respuesta incluye "locationType" con valor nulo

  # ===========================================================================
  # HU-02: Guardar layout de ubicaciones
  # ===========================================================================

  @smoke @critico
  Escenario: CRITERIO-2.1 — Guardar layout por primera vez crea las ubicaciones vacías
    Dado que existe la cotización con folio "FOL-2026-00042" sin ubicaciones configuradas
    Y la cotización está en la versión 1
    Cuando el agente guarda el layout con 3 ubicaciones de tipo "MULTIPLE" indicando versión 1
    Entonces el sistema responde con código 200
    Y la respuesta incluye "numberOfLocations" igual a 3
    Y la respuesta incluye "locationType" igual a "MULTIPLE"
    Y la respuesta incluye "version" igual a 2
    Y existen exactamente 3 ubicaciones activas asociadas al folio "FOL-2026-00042"
    Y cada ubicación tiene los índices 1, 2 y 3 respectivamente
    Y todas las ubicaciones tienen el campo activo en verdadero

  @smoke @critico
  Escenario: CRITERIO-2.2 — Aumentar numberOfLocations agrega ubicaciones vacías
    Dado que existe la cotización con folio "FOL-2026-00042" con 2 ubicaciones activas ya rellenas
    Y la cotización está en la versión 3
    Cuando el agente actualiza el layout a 4 ubicaciones de tipo "MULTIPLE" indicando versión 3
    Entonces el sistema responde con código 200
    Y la respuesta incluye "version" igual a 4
    Y existen exactamente 4 ubicaciones activas asociadas al folio "FOL-2026-00042"
    Y las 2 ubicaciones originales conservan sus datos previos
    Y las 2 ubicaciones nuevas tienen los campos de detalle vacíos

  @smoke @critico
  Escenario: CRITERIO-2.3 — Reducir numberOfLocations marca excedentes como inactivas
    Dado que existe la cotización con folio "FOL-2026-00042" con 4 ubicaciones activas
    Y la cotización está en la versión 4
    Cuando el agente actualiza el layout a 2 ubicaciones de tipo "MULTIPLE" indicando versión 4
    Entonces el sistema responde con código 200
    Y la respuesta incluye "version" igual a 5
    Y las ubicaciones con índice 1 y 2 permanecen activas
    Y las ubicaciones con índice 3 y 4 quedan marcadas como inactivas
    Y en la base de datos existen 4 registros (ninguno fue eliminado)

  @regression
  Escenario: CRITERIO-2.4 — Conflicto de versión retorna 409
    Dado que existe la cotización con folio "FOL-2026-00042" en la versión 3
    Cuando el agente intenta guardar el layout con 2 ubicaciones indicando versión 2
    Entonces el sistema responde con código 409
    Y la respuesta contiene el error "Optimistic lock conflict"
    Y la respuesta contiene el código de error "VERSION_CONFLICT"
    Y el layout de la cotización no fue modificado

  @regression
  Escenario: CRITERIO-2.5 — Guardar layout en folio inexistente retorna 404
    Dado que no existe ninguna cotización con folio "FOL-9999-99999"
    Cuando el agente intenta guardar el layout con 2 ubicaciones para el folio "FOL-9999-99999"
    Entonces el sistema responde con código 404
    Y la respuesta contiene el error "Folio not found"
    Y la respuesta contiene el código de error "FOLIO_NOT_FOUND"

  @regression
  Escenario: CRITERIO-2.6 — numberOfLocations igual a cero retorna 422
    Dado que existe la cotización con folio "FOL-2026-00042" en la versión 1
    Cuando el agente intenta guardar el layout con 0 ubicaciones indicando versión 1
    Entonces el sistema responde con código 422
    Y la respuesta contiene el error "Validation failed"
    Y la respuesta contiene el código de error "VALIDATION_ERROR"
    Y la respuesta incluye el campo "fields" con los errores de validación

  # ===========================================================================
  # Edge Cases — Reglas de Negocio adicionales
  # ===========================================================================

  @regression
  Escenario: RN-1 — numberOfLocations negativo retorna 422
    Dado que existe la cotización con folio "FOL-2026-00042" en la versión 1
    Cuando el agente intenta guardar el layout con -1 ubicaciones indicando versión 1
    Entonces el sistema responde con código 422
    Y la respuesta contiene el código de error "VALIDATION_ERROR"

  @regression
  Escenario: RN-1 — numberOfLocations mayor a 50 retorna 422
    Dado que existe la cotización con folio "FOL-2026-00042" en la versión 1
    Cuando el agente intenta guardar el layout con 51 ubicaciones indicando versión 1
    Entonces el sistema responde con código 422
    Y la respuesta contiene el código de error "VALIDATION_ERROR"

  @smoke @critico
  Escenario: RN-1 — numberOfLocations igual a 50 (límite superior válido) es aceptado
    Dado que existe la cotización con folio "FOL-2026-00042" sin ubicaciones configuradas
    Y la cotización está en la versión 1
    Cuando el agente guarda el layout con 50 ubicaciones de tipo "MULTIPLE" indicando versión 1
    Entonces el sistema responde con código 200
    Y existen exactamente 50 ubicaciones activas asociadas al folio "FOL-2026-00042"

  @regression
  Escenario: RN-2 — locationType inválido retorna 422
    Dado que existe la cotización con folio "FOL-2026-00042" en la versión 1
    Cuando el agente intenta guardar el layout con tipo "PARCIAL" y 2 ubicaciones indicando versión 1
    Entonces el sistema responde con código 422
    Y la respuesta contiene el código de error "VALIDATION_ERROR"

  @regression
  Escenario: RN-2 — locationType SINGLE con numberOfLocations mayor a 1 retorna 422
    Dado que existe la cotización con folio "FOL-2026-00042" en la versión 1
    Cuando el agente intenta guardar el layout con tipo "SINGLE" y 3 ubicaciones indicando versión 1
    Entonces el sistema responde con código 422
    Y la respuesta contiene el código de error "VALIDATION_ERROR"
    Y la respuesta indica que SINGLE solo admite una ubicación

  @smoke @critico
  Escenario: RN-2 — locationType SINGLE con numberOfLocations igual a 1 es aceptado
    Dado que existe la cotización con folio "FOL-2026-00042" sin ubicaciones configuradas
    Y la cotización está en la versión 1
    Cuando el agente guarda el layout con 1 ubicación de tipo "SINGLE" indicando versión 1
    Entonces el sistema responde con código 200
    Y existen exactamente 1 ubicación activa asociada al folio "FOL-2026-00042"
    Y la respuesta incluye "locationType" igual a "SINGLE"

  @regression
  Escenario: RN-3 — Actualizar layout con mismo numberOfLocations solo actualiza locationType
    Dado que existe la cotización con folio "FOL-2026-00042" con 2 ubicaciones activas tipo "SINGLE"
    Y la cotización está en la versión 2
    Cuando el agente actualiza el layout a 2 ubicaciones de tipo "MULTIPLE" indicando versión 2
    Entonces el sistema responde con código 200
    Y la respuesta incluye "version" igual a 3
    Y existen exactamente 2 ubicaciones activas asociadas al folio (sin nuevas inserciones)
    Y la respuesta incluye "locationType" igual a "MULTIPLE"

  @regression
  Escenario: RN-4 — Reducir y luego aumentar numberOfLocations reactiva nuevas ubicaciones (no reactiva las inactivas)
    Dado que existe la cotización con folio "FOL-2026-00042" con 4 ubicaciones activas
    Y la cotización está en la versión 4
    Y el agente ya redujo el layout a 2 ubicaciones (índices 3 y 4 inactivos) y la versión quedó en 5
    Cuando el agente aumenta el layout a 3 ubicaciones indicando versión 5
    Entonces el sistema responde con código 200
    Y existe 1 nueva ubicación vacía con índice 5
    Y las ubicaciones con índice 3 y 4 permanecen inactivas
    Y existen en total 5 registros en la tabla locations para dicho folio

  @regression
  Escenario: RN-4 — Las filas de ubicaciones nunca se eliminan físicamente
    Dado que existe la cotización con folio "FOL-2026-00042" con 3 ubicaciones activas
    Y la cotización está en la versión 2
    Cuando el agente reduce el layout a 1 ubicación indicando versión 2
    Entonces el sistema responde con código 200
    Y en la base de datos existen 3 registros de ubicaciones para dicho folio
    Y solo 1 de los registros tiene el campo activo en verdadero
    Y 2 registros tienen el campo activo en falso

  @regression
  Esquema del escenario: Validar límites del campo numberOfLocations
    Dado que existe la cotización con folio "FOL-2026-00042" en la versión 1
    Cuando el agente intenta guardar el layout con <cantidad> ubicaciones de tipo "MULTIPLE"
    Entonces el sistema responde con código <codigoHttp>

    Ejemplos:
      | cantidad | codigoHttp |
      | -1       | 422        |
      | 0        | 422        |
      | 1        | 200        |
      | 50       | 200        |
      | 51       | 422        |
      | 100      | 422        |
```

---

## Resumen de cobertura

| Criterio | Escenario Gherkin | Tags |
|----------|------------------|------|
| CRITERIO-1.1 | Obtener layout con configuración existente | `@smoke @critico` |
| CRITERIO-1.2 | Consulta folio inexistente → 404 | `@regression` |
| CRITERIO-1.3 | Cotización sin layout → valores nulos | `@regression` |
| CRITERIO-2.1 | Guardar layout primera vez crea ubicaciones | `@smoke @critico` |
| CRITERIO-2.2 | Aumentar numberOfLocations agrega ubicaciones | `@smoke @critico` |
| CRITERIO-2.3 | Reducir numberOfLocations marca inactivas | `@smoke @critico` |
| CRITERIO-2.4 | Conflicto de versión → 409 | `@regression` |
| CRITERIO-2.5 | Folio inexistente en PUT → 404 | `@regression` |
| CRITERIO-2.6 | numberOfLocations = 0 → 422 | `@regression` |
| RN-1 | numberOfLocations negativo → 422 | `@regression` |
| RN-1 | numberOfLocations > 50 → 422 | `@regression` |
| RN-1 | numberOfLocations = 50 (límite válido) → 200 | `@smoke @critico` |
| RN-2 | locationType inválido → 422 | `@regression` |
| RN-2 | SINGLE + numberOfLocations > 1 → 422 | `@regression` |
| RN-2 | SINGLE + numberOfLocations = 1 → 200 | `@smoke @critico` |
| RN-3 | Misma cantidad → solo actualiza locationType | `@regression` |
| RN-4 | Aumentar tras reducir inserta nuevas, no reactiva | `@regression` |
| RN-4 | Nunca se eliminan filas físicas | `@regression` |
| RN-1 (esquema) | Límites tabulados 6 casos | `@regression` |

**Total escenarios:** 19 (6 `@smoke @critico` + 13 `@regression`)

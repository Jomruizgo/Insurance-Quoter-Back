# Escenarios Gherkin — Quote State (SPEC-005)

**Feature:** Consulta del estado y progreso de completitud de una cotización  
**Endpoint:** `GET /v1/quotes/{folio}/state`  
**Generado:** 2026-04-21

---

## Datos de Prueba Sintéticos

| Escenario | Campo | Válido | Inválido | Borde |
|-----------|-------|--------|----------|-------|
| Folio con layout completo | folioNumber | `FOL-TEST-001` | `FOL-INEXISTENTE` | `""` (vacío) |
| Progreso parcial | numberOfLocations | `2` | `null` | `0` |
| Folio calculado | quoteStatus | `CALCULATED` | `CREATED` | `ISSUED` |
| Ubicación completa | validationStatus | `COMPLETE` | `INCOMPLETE` | sin registros |
| Alertas bloqueantes | blockingAlerts | `[]` | `[MISSING_ZIP_CODE]` | múltiples alertas |

---

```gherkin
#language: es
Característica: Consulta de estado y progreso de completitud de una cotización
  Como agente de seguros que trabaja en un folio activo
  Quiero consultar el estado actual del folio en cualquier momento
  Para saber qué secciones están completas e incompletas y el porcentaje de avance

  Antecedentes:
    Dado que el sistema tiene acceso a la base de datos de cotizaciones

  # ---------------------------------------------------------------------------
  # CRITERIO-1.1 — Happy path: folio con progreso parcial
  # ---------------------------------------------------------------------------

  @happy-path @critico @smoke
  Escenario: Consulta exitosa de estado con layout completo y ubicación incompleta
    Dado que existe un folio "FOL-TEST-001" con estado "IN_PROGRESS"
    Y el folio tiene layout configurado con 2 ubicaciones de tipo "MULTIPLE"
    Y una de las ubicaciones tiene validationStatus "COMPLETE" y sin alertas bloqueantes
    Y la otra ubicación tiene alertas bloqueantes activas
    Y no se ha configurado opciones de cobertura ni ejecutado cálculo
    Cuando el agente consulta el estado del folio "FOL-TEST-001"
    Entonces el sistema retorna estado 200
    Y el campo "quoteStatus" es "IN_PROGRESS"
    Y el campo "sections.layout" es "COMPLETE"
    Y el campo "sections.locations" es "INCOMPLETE"
    Y el campo "sections.generalInfo" es "PENDING"
    Y el campo "sections.coverageOptions" es "PENDING"
    Y el campo "sections.calculation" es "PENDING"
    Y el campo "completionPercentage" es 20
    Y la respuesta incluye los campos "version" y "updatedAt"

  # ---------------------------------------------------------------------------
  # CRITERIO-1.2 — Happy path: folio recién creado con progreso cero
  # ---------------------------------------------------------------------------

  @happy-path @critico
  Escenario: Folio recién creado retorna progreso en cero
    Dado que existe un folio "FOL-TEST-002" con estado "CREATED"
    Y el folio no tiene layout configurado
    Y el folio no tiene ubicaciones registradas
    Cuando el agente consulta el estado del folio "FOL-TEST-002"
    Entonces el sistema retorna estado 200
    Y el campo "quoteStatus" es "CREATED"
    Y el campo "completionPercentage" es 0
    Y el campo "sections.layout" es "PENDING"
    Y el campo "sections.locations" es "PENDING"
    Y el campo "sections.generalInfo" es "PENDING"
    Y el campo "sections.coverageOptions" es "PENDING"
    Y el campo "sections.calculation" es "PENDING"

  # ---------------------------------------------------------------------------
  # CRITERIO-1.3 — Happy path: folio CALCULATED retorna 100%
  # ---------------------------------------------------------------------------

  @happy-path @critico @smoke
  Escenario: Folio calculado retorna progreso al cien por ciento
    Dado que existe un folio "FOL-TEST-003" con estado "CALCULATED"
    Y el folio tiene layout con 2 ubicaciones configuradas
    Y ambas ubicaciones tienen validationStatus "COMPLETE"
    Cuando el agente consulta el estado del folio "FOL-TEST-003"
    Entonces el sistema retorna estado 200
    Y el campo "quoteStatus" es "CALCULATED"
    Y el campo "completionPercentage" es 100
    Y el campo "sections.calculation" es "COMPLETE"
    Y el campo "sections.layout" es "COMPLETE"
    Y el campo "sections.locations" es "COMPLETE"

  # ---------------------------------------------------------------------------
  # CRITERIO-1.4 — Error path: folio inexistente
  # ---------------------------------------------------------------------------

  @error-path @critico @smoke
  Escenario: Consulta de folio inexistente retorna 404
    Dado que el folio "FOL-INEXISTENTE" no existe en la base de datos
    Cuando el agente consulta el estado del folio "FOL-INEXISTENTE"
    Entonces el sistema retorna estado 404
    Y el cuerpo de la respuesta contiene el campo "code" con valor "FOLIO_NOT_FOUND"
    Y el cuerpo de la respuesta contiene el campo "error" con valor "Folio not found"
    Y no se retorna información de secciones

  # ---------------------------------------------------------------------------
  # CRITERIO-1.5 — Edge case: secciones con dependencias no implementadas
  # ---------------------------------------------------------------------------

  @edge-case
  Escenario: Secciones sin feature implementada retornan PENDING sin error
    Dado que existe un folio "FOL-TEST-004" con estado "IN_PROGRESS"
    Y los campos de datos del asegurado no existen en la base de datos (SPEC-006 pendiente)
    Y no existe tabla de opciones de cobertura (SPEC-007 pendiente)
    Cuando el agente consulta el estado del folio "FOL-TEST-004"
    Entonces el sistema retorna estado 200
    Y el campo "sections.generalInfo" es "PENDING"
    Y el campo "sections.coverageOptions" es "PENDING"
    Y el sistema no lanza ninguna excepción ni retorna error 500

  # ---------------------------------------------------------------------------
  # CRITERIO-1.6 — Edge case: todas las ubicaciones completas pero sin cálculo
  # ---------------------------------------------------------------------------

  @edge-case
  Escenario: Todas las ubicaciones completas pero sin calculo ejecutado
    Dado que existe un folio "FOL-TEST-005" con estado "IN_PROGRESS"
    Y el folio tiene layout con 3 ubicaciones
    Y las 3 ubicaciones tienen validationStatus "COMPLETE" y sin alertas bloqueantes
    Y el cálculo aún no se ha ejecutado
    Cuando el agente consulta el estado del folio "FOL-TEST-005"
    Entonces el sistema retorna estado 200
    Y el campo "sections.locations" es "COMPLETE"
    Y el campo "sections.calculation" es "PENDING"
    Y el campo "completionPercentage" es 40

  # ---------------------------------------------------------------------------
  # CRITERIO-1.7 — Edge case: layout con numberOfLocations pero sin locationType
  # ---------------------------------------------------------------------------

  @edge-case
  Escenario: Layout parcialmente configurado retorna IN_PROGRESS
    Dado que existe un folio "FOL-TEST-006" con estado "IN_PROGRESS"
    Y el folio tiene numberOfLocations igual a 2 pero sin locationType configurado
    Y el folio no tiene ubicaciones registradas
    Cuando el agente consulta el estado del folio "FOL-TEST-006"
    Entonces el sistema retorna estado 200
    Y el campo "sections.layout" es "IN_PROGRESS"
    Y el campo "sections.locations" es "PENDING"

  # ---------------------------------------------------------------------------
  # CRITERIO-1.8 — Edge case: folio CALCULATED fuerza 100% aunque secciones incompletas
  # ---------------------------------------------------------------------------

  @edge-case
  Escenario: Folio CALCULATED retorna 100 por ciento aunque existan ubicaciones con alertas
    Dado que existe un folio "FOL-TEST-007" con estado "CALCULATED"
    Y una de las ubicaciones del folio tiene alertas bloqueantes residuales
    Cuando el agente consulta el estado del folio "FOL-TEST-007"
    Entonces el sistema retorna estado 200
    Y el campo "completionPercentage" es 100
    Y el campo "quoteStatus" es "CALCULATED"
```

---

## Resumen de cobertura

| ID | Tipo | Criterio | Etiquetas |
|----|------|----------|-----------|
| CRITERIO-1.1 | Happy path | Layout COMPLETE + ubicación INCOMPLETE → 20% | `@happy-path @critico @smoke` |
| CRITERIO-1.2 | Happy path | Folio nuevo → 0% | `@happy-path @critico` |
| CRITERIO-1.3 | Happy path | CALCULATED → 100% | `@happy-path @critico @smoke` |
| CRITERIO-1.4 | Error path | Folio inexistente → 404 FOLIO_NOT_FOUND | `@error-path @critico @smoke` |
| CRITERIO-1.5 | Edge case | generalInfo + coverageOptions PENDING sin excepción | `@edge-case` |
| CRITERIO-1.6 | Edge case | Todas ubicaciones COMPLETE, cálculo PENDING → 40% | `@edge-case` |
| CRITERIO-1.7 | Edge case | numberOfLocations sin locationType → layout IN_PROGRESS | `@edge-case` |
| CRITERIO-1.8 | Edge case | CALCULATED fuerza 100% ignorando secciones incompletas | `@edge-case` |

**Total escenarios: 8** (3 smoke, 4 happy/error path, 4 edge cases)

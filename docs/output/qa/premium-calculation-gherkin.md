# Escenarios Gherkin — Cálculo de Prima Neta y Comercial
**Spec:** SPEC-007 | **Endpoint:** `POST /v1/quotes/{folio}/calculate`  
**Generado:** 2026-04-22 | **Autor:** QA Lead ASDD

---

## Supuestos de Tarifas (Antecedentes)

Los valores utilizados en todos los escenarios provienen del stub `GET /v1/tariffs` del core service (puerto 8081),
extendido con los factores adicionales descritos en la sección de supuestos documentados de la SPEC-007.

| Factor | Clave | Valor por defecto |
|--------|-------|-------------------|
| Tasa incendio edificios | `fireRate` | 0.0015 |
| Tasa incendio contenidos | `fireContentsRate` | 0.0012 |
| Factor extensión de cobertura | `coverageExtensionFactor` | 0.07 |
| Factor catastrófico TEV | `cattevFactor` | 0.0008 |
| Factor catastrófico FHM | `catfhmFactor` | 0.0005 |
| Factor remoción de escombros | `debrisRemovalFactor` | 0.03 |
| Factor gastos extraordinarios | `extraordinaryExpensesFactor` | 0.02 |
| Tasa pérdida de rentas | `rentalLossRate` | 0.001 |
| Tasa interrupción de negocio | `businessInterruptionRate` | 0.0015 |
| Tasa equipo electrónico | `electronicEquipmentRate` | 0.002 |
| Tasa robo | `theftRate` | 0.003 |
| Tasa dinero y valores | `cashAndValuesRate` | 0.005 |
| Tasa vidrios | `glassRate` | 0.004 |
| Tasa anuncios luminosos | `luminousSignageRate` | 0.0025 |
| Factor comercial | `commercialFactor` | 1.16 |

### Cálculo de referencia — Ubicación tipo "Bodega" con GUA-FIRE

```
insuredValue GUA-FIRE = 10,000,000

fireBuildings       = 10,000,000 × 0.0015       = 15,000.00
fireContents        = 0 (sin GUA-FIRE-CONT)      =      0.00
firePremium         = fireBuildings + fireContents = 15,000.00

coverageExtension   = 15,000.00 × 0.07           =  1,050.00
cattev              = 15,000.00 × 0.0008          =     12.00
catfhm              = 15,000.00 × 0.0005          =      7.50
debrisRemoval       = 15,000.00 × 0.03            =    450.00
extraordinaryExpenses = 15,000.00 × 0.02          =    300.00
rentalLoss          = 0 (sin GUA-RENTAL)          =      0.00
businessInterruption = 0 (sin GUA-BI)             =      0.00
electronicEquipment = 0 (sin GUA-ELEC)            =      0.00
theft               = 0 (sin GUA-THEFT)           =      0.00
cashAndValues       = 0 (sin GUA-CASH)            =      0.00
glass               = 0 (sin GUA-GLASS)           =      0.00
luminousSignage     = 0 (sin GUA-SIGN)            =      0.00

netPremium ubicación = 15,000 + 0 + 1,050 + 12 + 7.50 + 450 + 300 = 16,819.50
commercialPremium   = 16,819.50 × 1.16            = 19,510.62
```

---

## Feature

```gherkin
# language: es
Característica: Cálculo de prima neta y comercial — SPEC-007
  Como agente de seguros
  Quiero ejecutar POST /v1/quotes/{folio}/calculate
  Para obtener prima neta, comercial y desglose por ubicación
```

---

## Antecedentes

```gherkin
Antecedentes:
  Dado que existe un folio "FOL-2026-TEST" en la base de datos con version 7
  Y el folio tiene quoteStatus "IN_PROGRESS"
  Y el core service retorna tarifas válidas en GET /v1/tariffs con los siguientes factores:
    | campo                      | valor  |
    | fireRate                   | 0.0015 |
    | fireContentsRate           | 0.0012 |
    | coverageExtensionFactor    | 0.07   |
    | cattevFactor               | 0.0008 |
    | catfhmFactor               | 0.0005 |
    | debrisRemovalFactor        | 0.03   |
    | extraordinaryExpensesFactor| 0.02   |
    | rentalLossRate             | 0.001  |
    | businessInterruptionRate   | 0.0015 |
    | electronicEquipmentRate    | 0.002  |
    | theftRate                  | 0.003  |
    | cashAndValuesRate          | 0.005  |
    | glassRate                  | 0.004  |
    | luminousSignageRate        | 0.0025 |
    | commercialFactor           | 1.16   |
  Y el catálogo de garantías del core retorna en GET /v1/catalogs/guarantees:
    | code          | tarifable |
    | GUA-FIRE      | true      |
    | GUA-FIRE-CONT | true      |
    | GUA-THEFT     | true      |
    | GUA-GLASS     | true      |
    | GUA-ELEC      | true      |
    | GUA-RENTAL    | true      |
    | GUA-BI        | true      |
    | GUA-CASH      | true      |
    | GUA-SIGN      | true      |
    | GUA-NON-TAR   | false     |
```

---

## CRITERIO-1.1: Cálculo exitoso con todas las ubicaciones completas

```gherkin
Escenario: CRITERIO-1.1 — Todas las ubicaciones son calculables y el cálculo retorna desglose completo
  # Precondición: 2 ubicaciones, ambas con zipCode, fireKey y garantías tarifables
  Dado que el folio "FOL-2026-TEST" tiene exactamente 2 ubicaciones con los siguientes datos:
    | index | locationName     | zipCode | fireKey    | garantías                               |
    | 1     | Bodega Principal | 06600   | FK-INC-01  | GUA-FIRE: insuredValue=10,000,000       |
    | 2     | Oficina Norte    | 44100   | FK-INC-02  | GUA-FIRE: insuredValue=5,000,000        |
  Y ambas ubicaciones tienen validationStatus "COMPLETE"
  Cuando se envía POST /v1/quotes/FOL-2026-TEST/calculate con el body:
    """
    {
      "version": 7
    }
    """
  Entonces la respuesta HTTP es 200
  Y el campo "quoteStatus" en la respuesta es "CALCULATED"
  Y el campo "version" en la respuesta es 8
  Y el campo "calculatedAt" contiene un timestamp en formato ISO-8601 UTC
  Y "premiumsByLocation" contiene exactamente 2 entradas
  Y "premiumsByLocation[0].calculable" es true
  Y "premiumsByLocation[0].netPremium" es 16819.50
  Y "premiumsByLocation[0].commercialPremium" es 19510.62
  Y "premiumsByLocation[0].coverageBreakdown.fireBuildings" es 15000.00
  Y "premiumsByLocation[0].coverageBreakdown.coverageExtension" es 1050.00
  Y "premiumsByLocation[0].coverageBreakdown.cattev" es 12.00
  Y "premiumsByLocation[0].coverageBreakdown.catfhm" es 7.50
  Y "premiumsByLocation[0].coverageBreakdown.debrisRemoval" es 450.00
  Y "premiumsByLocation[0].coverageBreakdown.extraordinaryExpenses" es 300.00
  Y "premiumsByLocation[0].coverageBreakdown.theft" es 0.00
  Y "premiumsByLocation[0].blockingAlerts" es una lista vacía
  Y "premiumsByLocation[1].calculable" es true
  Y "premiumsByLocation[1].netPremium" es 8409.75
  Y "premiumsByLocation[1].commercialPremium" es 9755.31
  Y "premiumsByLocation[1].coverageBreakdown.fireBuildings" es 7500.00
  Y "netPremium" total en la respuesta es 25229.25
  Y "commercialPremium" total en la respuesta es 29265.93
  Y el resultado fue persistido en la tabla "calculation_results" con quote_id correspondiente a "FOL-2026-TEST"

# Notas de cálculo ubicación 2:
# fireBuildings = 5,000,000 × 0.0015 = 7,500.00
# coverageExtension = 7,500.00 × 0.07 = 525.00
# cattev = 7,500.00 × 0.0008 = 6.00
# catfhm = 7,500.00 × 0.0005 = 3.75
# debrisRemoval = 7,500.00 × 0.03 = 225.00
# extraordinaryExpenses = 7,500.00 × 0.02 = 150.00
# netPremium[2] = 7,500 + 525 + 6 + 3.75 + 225 + 150 = 8,409.75
# netPremium total = 16,819.50 + 8,409.75 = 25,229.25
# commercialPremium total = 25,229.25 × 1.16 = 29,265.93
```

---

## CRITERIO-1.2: Cálculo parcial con una ubicación incompleta

```gherkin
Escenario: CRITERIO-1.2 — Una ubicación sin zipCode es excluida del cálculo pero no bloquea las demás
  Dado que el folio "FOL-2026-TEST" tiene exactamente 2 ubicaciones con los siguientes datos:
    | index | locationName     | zipCode | fireKey   | garantías                          |
    | 1     | Bodega Central   | 06600   | FK-INC-01 | GUA-FIRE: insuredValue=10,000,000  |
    | 2     | Almacén Sur      | (vacío) | FK-INC-02 | GUA-FIRE: insuredValue=3,000,000   |
  Y la ubicación 2 tiene validationStatus "INCOMPLETE"
  Cuando se envía POST /v1/quotes/FOL-2026-TEST/calculate con el body:
    """
    {
      "version": 7
    }
    """
  Entonces la respuesta HTTP es 200
  Y el campo "quoteStatus" en la respuesta es "CALCULATED"
  Y "premiumsByLocation[0].calculable" es true
  Y "premiumsByLocation[0].netPremium" es 16819.50
  Y "premiumsByLocation[0].commercialPremium" es 19510.62
  Y "premiumsByLocation[0].blockingAlerts" es una lista vacía
  Y "premiumsByLocation[1].calculable" es false
  Y "premiumsByLocation[1].netPremium" es null
  Y "premiumsByLocation[1].commercialPremium" es null
  Y "premiumsByLocation[1].coverageBreakdown" es null
  Y "premiumsByLocation[1].blockingAlerts" contiene exactamente 1 alerta con código "MISSING_ZIP_CODE"
  Y "netPremium" total en la respuesta es 16819.50
  Y "commercialPremium" total en la respuesta es 19510.62
  Y el campo "version" en la respuesta es 8
```

---

## CRITERIO-1.3: Todas las ubicaciones son incompletas

```gherkin
Escenario: CRITERIO-1.3 — Cuando ninguna ubicación es calculable, el endpoint retorna 422 sin persistir
  Dado que el folio "FOL-2026-TEST" tiene exactamente 2 ubicaciones con los siguientes datos:
    | index | locationName   | zipCode | fireKey    | garantías                         |
    | 1     | Local A        | 06600   | (vacío)    | GUA-FIRE: insuredValue=5,000,000  |
    | 2     | Local B        | (vacío) | (vacío)    | GUA-FIRE: insuredValue=2,000,000  |
  Cuando se envía POST /v1/quotes/FOL-2026-TEST/calculate con el body:
    """
    {
      "version": 7
    }
    """
  Entonces la respuesta HTTP es 422
  Y el campo "error" en la respuesta es "No calculable locations"
  Y el campo "code" en la respuesta es "NO_CALCULABLE_LOCATIONS"
  Y el quoteStatus del folio "FOL-2026-TEST" permanece como "IN_PROGRESS" en base de datos
  Y no existe ningún registro en la tabla "calculation_results" para el folio "FOL-2026-TEST"
  Y la versión del folio "FOL-2026-TEST" sigue siendo 7
```

---

## CRITERIO-1.4: Conflicto de versión — optimistic lock

```gherkin
Escenario: CRITERIO-1.4 — La versión enviada en el request no coincide con la versión almacenada
  Dado que el folio "FOL-2026-TEST" está almacenado con version 8
  Y el folio tiene al menos 1 ubicación calculable con zipCode "06600", fireKey "FK-INC-01" y garantía GUA-FIRE con insuredValue=10,000,000
  Cuando se envía POST /v1/quotes/FOL-2026-TEST/calculate con el body:
    """
    {
      "version": 7
    }
    """
  Entonces la respuesta HTTP es 409
  Y el campo "error" en la respuesta es "Optimistic lock conflict"
  Y el campo "code" en la respuesta es "VERSION_CONFLICT"
  Y no existe ningún nuevo registro en la tabla "calculation_results" para el folio "FOL-2026-TEST"
  Y la versión del folio "FOL-2026-TEST" sigue siendo 8
```

---

## CRITERIO-1.5: Folio inexistente

```gherkin
Escenario: CRITERIO-1.5 — El folio solicitado no existe en base de datos
  Dado que el folio "FOL-9999-00001" no existe en la base de datos
  Cuando se envía POST /v1/quotes/FOL-9999-00001/calculate con el body:
    """
    {
      "version": 1
    }
    """
  Entonces la respuesta HTTP es 404
  Y el campo "error" en la respuesta es "Folio not found"
  Y el campo "code" en la respuesta es "FOLIO_NOT_FOUND"
```

---

## CRITERIO-1.6: Recálculo — resultado anterior es reemplazado

```gherkin
Escenario: CRITERIO-1.6 — Recalcular una cotización ya calculada reemplaza el resultado anterior
  Dado que el folio "FOL-2026-TEST" ya fue calculado previamente con quoteStatus "CALCULATED"
  Y la versión actual del folio es 8
  Y existe un registro previo en "calculation_results" para el folio con netPremium=16819.50
  Y el folio tiene 1 ubicación con zipCode "06600", fireKey "FK-INC-01" y garantía GUA-FIRE con insuredValue=10,000,000
  Cuando se envía POST /v1/quotes/FOL-2026-TEST/calculate con el body:
    """
    {
      "version": 8
    }
    """
  Entonces la respuesta HTTP es 200
  Y el campo "quoteStatus" en la respuesta es "CALCULATED"
  Y el campo "netPremium" en la respuesta es 16819.50
  Y el campo "version" en la respuesta es 9
  Y existe exactamente 1 registro en "calculation_results" para el folio "FOL-2026-TEST"
  Y el registro anterior fue eliminado y reemplazado por el nuevo
  Y el campo "calculatedAt" del nuevo resultado es posterior al del resultado anterior
```

---

## CRITERIO-1.7: Ubicación con garantías no tarifables

```gherkin
Escenario: CRITERIO-1.7 — Ubicación con zipCode y fireKey válidos pero sin garantías tarifables
  Dado que el folio "FOL-2026-TEST" tiene exactamente 1 ubicación con los siguientes datos:
    | index | locationName | zipCode | fireKey   | garantías                       |
    | 1     | Depósito X   | 06600   | FK-INC-01 | GUA-NON-TAR: insuredValue=8,000,000 |
  Y la garantía "GUA-NON-TAR" tiene tarifable=false según el catálogo del core
  Cuando se envía POST /v1/quotes/FOL-2026-TEST/calculate con el body:
    """
    {
      "version": 7
    }
    """
  Entonces la respuesta HTTP es 422
  Y el campo "code" en la respuesta es "NO_CALCULABLE_LOCATIONS"
  # Nota: al ser la única ubicación y no calculable, se eleva NO_CALCULABLE_LOCATIONS
  # Si hubiera otra ubicación calculable, el resultado de esta ubicación sería:
  # calculable=false con blockingAlerts[{code: "MISSING_TARIFABLE_GUARANTEE"}]

Escenario: CRITERIO-1.7b — Ubicación no tarifable en cotización mixta expone alerta correcta
  Dado que el folio "FOL-2026-TEST" tiene exactamente 2 ubicaciones con los siguientes datos:
    | index | locationName | zipCode | fireKey   | garantías                               |
    | 1     | Depósito X   | 06600   | FK-INC-01 | GUA-NON-TAR: insuredValue=8,000,000     |
    | 2     | Bodega Y     | 44100   | FK-INC-02 | GUA-FIRE: insuredValue=10,000,000       |
  Y la garantía "GUA-NON-TAR" tiene tarifable=false según el catálogo del core
  Cuando se envía POST /v1/quotes/FOL-2026-TEST/calculate con el body:
    """
    {
      "version": 7
    }
    """
  Entonces la respuesta HTTP es 200
  Y "premiumsByLocation[0].calculable" es false
  Y "premiumsByLocation[0].netPremium" es null
  Y "premiumsByLocation[0].coverageBreakdown" es null
  Y "premiumsByLocation[0].blockingAlerts" contiene exactamente 1 alerta con código "MISSING_TARIFABLE_GUARANTEE"
  Y "premiumsByLocation[1].calculable" es true
  Y "premiumsByLocation[1].netPremium" es 16819.50
  Y "netPremium" total en la respuesta es 16819.50
```

---

## Escenarios Adicionales de Borde

### BORDE-01: Recálculo con datos diferentes reemplaza el resultado anterior completamente

```gherkin
Escenario: BORDE-01 — Recálculo con insuredValue distinto produce prima diferente y elimina resultado previo
  Dado que el folio "FOL-2026-TEST" fue calculado previamente con quoteStatus "CALCULATED" y version 8
  Y el registro previo en "calculation_results" tiene netPremium=16819.50 (una ubicación con GUA-FIRE=10,000,000)
  Y la ubicación 1 fue actualizada posteriormente con GUA-FIRE insuredValue=20,000,000
  Cuando se envía POST /v1/quotes/FOL-2026-TEST/calculate con el body:
    """
    {
      "version": 8
    }
    """
  Entonces la respuesta HTTP es 200
  Y el campo "netPremium" en la respuesta es 33639.00
  # fireBuildings = 20,000,000 × 0.0015 = 30,000.00
  # coverageExtension = 30,000.00 × 0.07 = 2,100.00
  # cattev = 30,000.00 × 0.0008 = 24.00
  # catfhm = 30,000.00 × 0.0005 = 15.00
  # debrisRemoval = 30,000.00 × 0.03 = 900.00
  # extraordinaryExpenses = 30,000.00 × 0.02 = 600.00
  # netPremium = 30,000 + 2,100 + 24 + 15 + 900 + 600 = 33,639.00
  Y el campo "commercialPremium" en la respuesta es 39021.24
  Y existe exactamente 1 registro en "calculation_results" para "FOL-2026-TEST"
  Y el netPremium almacenado en "calculation_results" es 33639.00
  Y no existe ningún registro residual con netPremium=16819.50 para ese folio
  Y el campo "version" en la respuesta es 9
```

---

### BORDE-02: Ubicación con múltiples garantías del mismo tipo (GUA-FIRE duplicado)

```gherkin
Escenario: BORDE-02 — Ubicación con dos garantías GUA-FIRE acumula sus valores asegurados antes de aplicar la tasa
  Dado que el folio "FOL-2026-TEST" tiene exactamente 1 ubicación con los siguientes datos:
    | index | locationName    | zipCode | fireKey   | garantías                                                         |
    | 1     | Complejo Norte  | 06600   | FK-INC-01 | GUA-FIRE: insuredValue=6,000,000 / GUA-FIRE: insuredValue=4,000,000 |
  # La ubicación reporta dos líneas GUA-FIRE separadas (edificio A y edificio B)
  Cuando se envía POST /v1/quotes/FOL-2026-TEST/calculate con el body:
    """
    {
      "version": 7
    }
    """
  Entonces la respuesta HTTP es 200
  Y "premiumsByLocation[0].calculable" es true
  Y "premiumsByLocation[0].coverageBreakdown.fireBuildings" es 15000.00
  # sumInsuredValue(GUA-FIRE) = 6,000,000 + 4,000,000 = 10,000,000
  # fireBuildings = 10,000,000 × 0.0015 = 15,000.00
  Y "premiumsByLocation[0].netPremium" es 16819.50
  # Idéntico al cálculo de referencia: el resultado es el mismo que con una sola GUA-FIRE de 10,000,000
  Y "premiumsByLocation[0].blockingAlerts" es una lista vacía
```

---

### BORDE-03: Ubicación con insuredValue = 0 en todas las garantías tarifables

```gherkin
Escenario: BORDE-03 — Garantías con insuredValue=0 producen prima neta cero pero la ubicación sigue siendo calculable
  Dado que el folio "FOL-2026-TEST" tiene exactamente 1 ubicación con los siguientes datos:
    | index | locationName | zipCode | fireKey   | garantías                   |
    | 1     | Predio Vacío | 06600   | FK-INC-01 | GUA-FIRE: insuredValue=0    |
  Y la garantía "GUA-FIRE" tiene tarifable=true según el catálogo
  Cuando se envía POST /v1/quotes/FOL-2026-TEST/calculate con el body:
    """
    {
      "version": 7
    }
    """
  Entonces la respuesta HTTP es 200
  Y "premiumsByLocation[0].calculable" es true
  # La ubicación ES calculable: tiene zipCode, fireKey y al menos una garantía tarifable
  # El valor asegurado en cero es responsabilidad del agente, no invalida la calculabilidad
  Y "premiumsByLocation[0].coverageBreakdown.fireBuildings" es 0.00
  Y "premiumsByLocation[0].coverageBreakdown.coverageExtension" es 0.00
  Y "premiumsByLocation[0].coverageBreakdown.cattev" es 0.00
  Y "premiumsByLocation[0].coverageBreakdown.catfhm" es 0.00
  Y "premiumsByLocation[0].coverageBreakdown.debrisRemoval" es 0.00
  Y "premiumsByLocation[0].coverageBreakdown.extraordinaryExpenses" es 0.00
  Y "premiumsByLocation[0].netPremium" es 0.00
  Y "premiumsByLocation[0].commercialPremium" es 0.00
  Y "netPremium" total en la respuesta es 0.00
  Y "quoteStatus" en la respuesta es "CALCULATED"
  Y "premiumsByLocation[0].blockingAlerts" es una lista vacía
```

---

## Tabla de Datos de Prueba

| Escenario | folioNumber | version entrada | Ubicaciones | Garantías | netPremium esperado |
|-----------|-------------|-----------------|-------------|-----------|---------------------|
| CRITERIO-1.1 (happy path 2 ubicaciones) | FOL-2026-TEST | 7 | 2 (ambas calculables) | UBI-1: GUA-FIRE=10M / UBI-2: GUA-FIRE=5M | 25,229.25 |
| CRITERIO-1.2 (mixta) | FOL-2026-TEST | 7 | 2 (1 calculable, 1 sin zipCode) | UBI-1: GUA-FIRE=10M / UBI-2: GUA-FIRE=3M | 16,819.50 (solo ubi-1) |
| CRITERIO-1.3 (ninguna calculable) | FOL-2026-TEST | 7 | 2 (sin fireKey ni zipCode) | GUA-FIRE en ambas | 422 NO_CALCULABLE_LOCATIONS |
| CRITERIO-1.4 (version conflict) | FOL-2026-TEST | 7 (almacenada=8) | 1 calculable | GUA-FIRE=10M | 409 VERSION_CONFLICT |
| CRITERIO-1.5 (folio no existe) | FOL-9999-00001 | 1 | N/A | N/A | 404 FOLIO_NOT_FOUND |
| CRITERIO-1.6 (recálculo idempotente) | FOL-2026-TEST | 8 (ya calculado) | 1 calculable | GUA-FIRE=10M | 16,819.50 (reemplaza anterior) |
| CRITERIO-1.7 (sin garantía tarifable — única) | FOL-2026-TEST | 7 | 1 (GUA-NON-TAR) | GUA-NON-TAR=8M (tarifable=false) | 422 NO_CALCULABLE_LOCATIONS |
| CRITERIO-1.7b (sin garantía tarifable — mixta) | FOL-2026-TEST | 7 | 2 (1 no tar, 1 tar) | UBI-1: GUA-NON-TAR=8M / UBI-2: GUA-FIRE=10M | 16,819.50 (solo ubi-2) |
| BORDE-01 (recálculo con datos distintos) | FOL-2026-TEST | 8 (ya calculado) | 1 calculable (actualizada) | GUA-FIRE=20M | 33,639.00 (reemplaza 16,819.50) |
| BORDE-02 (GUA-FIRE duplicado) | FOL-2026-TEST | 7 | 1 calculable | GUA-FIRE=6M + GUA-FIRE=4M | 16,819.50 (suma=10M) |
| BORDE-03 (insuredValue=0) | FOL-2026-TEST | 7 | 1 calculable | GUA-FIRE=0 (tarifable=true) | 0.00 |

---

## Cobertura de Criterios

| Criterio spec | Escenario(s) cubierto(s) | Tipo |
|---------------|--------------------------|------|
| CRITERIO-1.1 | CRITERIO-1.1 | Happy path |
| CRITERIO-1.2 | CRITERIO-1.2 | Happy path / parcial |
| CRITERIO-1.3 | CRITERIO-1.3 | Error path |
| CRITERIO-1.4 | CRITERIO-1.4 | Error path |
| CRITERIO-1.5 | CRITERIO-1.5 | Error path |
| CRITERIO-1.6 | CRITERIO-1.6, BORDE-01 | Edge case / recálculo |
| CRITERIO-1.7 | CRITERIO-1.7, CRITERIO-1.7b | Edge case |
| — (borde no en spec) | BORDE-01 | Recálculo con datos distintos |
| — (borde no en spec) | BORDE-02 | GUA-FIRE duplicado |
| — (borde no en spec) | BORDE-03 | insuredValue = 0 |

**Total escenarios:** 11 (7 de criterios de aceptación + 3 de borde adicionales + 1 variante 1.7b)

---

## Observaciones para el Equipo de Desarrollo

1. **Precisión decimal:** todos los valores de prima deben usar `BigDecimal` con `RoundingMode.HALF_UP` escala 2 en el resultado final. Los datos de prueba en esta tabla asumen redondeo HALF_UP.

2. **coverageBreakdown null en no calculables:** el BORDE-03 confirma que una ubicación con insuredValue=0 sigue siendo calculable (tiene garantía tarifable); el campo `coverageBreakdown` solo es `null` cuando `calculable=false`.

3. **Semántica de BORDE-02:** `sumInsuredValueByCode` debe acumular todos los registros del mismo `code` dentro de una ubicación antes de aplicar la tasa. Los escenarios confirman este comportamiento explícitamente.

4. **Core service indisponible:** no se cubre en escenarios Gherkin de esta iteración porque no hay SLA definido que determine el comportamiento de degradación. Se escala al `/risk-identifier` como riesgo crítico.

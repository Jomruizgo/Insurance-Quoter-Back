---
id: SPEC-007
status: IMPLEMENTED
feature: premium-calculation
created: 2026-04-22
updated: 2026-04-22
author: spec-generator
version: "1.0"
related-specs:
  - SPEC-001  # folio-generator (Quote aggregate raíz)
  - SPEC-002  # folio-management (QuoteJpa con @Version, GlobalExceptionHandler)
  - SPEC-004  # location-management (LocationJpa, BlockingAlertCode, ValidationStatus)
  - SPEC-006  # coverage-options (CoverageOptionJpa, catálogo de garantías)
---

# Spec: Cálculo de Prima Neta y Comercial

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción

`Insurance-Quoter-Back` expone el endpoint `POST /v1/quotes/{folio}/calculate` que ejecuta el cálculo de prima neta y prima comercial para todas las ubicaciones calculables de una cotización. Las ubicaciones incompletas generan alertas pero no bloquean el cálculo de las demás. El resultado se persiste en una única transacción que también actualiza el estado de la cotización a `CALCULATED`. La lógica de cálculo vive íntegramente en `CalculationService` (domain service puro, sin dependencias de Spring), facilitando el 100% TDD por componente de desglose.

### Requerimiento de Negocio

> - `POST /v1/quotes/{folio}/calculate` — ejecuta el cálculo de prima para la cotización identificada por `folio`.
> - Lee la cotización completa: `Quote` + `Locations[]` + `CoverageOptions[]`.
> - Consume tarifas y factores técnicos del core service: `GET /v1/tariffs` (puerto 8081).
> - Determina si cada ubicación es calculable: `zipCode` válido + `businessLine.fireKey` presente + al menos una garantía tarifable.
> - Si una ubicación no es calculable: registra alerta `blockingAlerts`, continúa con las demás.
> - Si **ninguna** ubicación es calculable: responde HTTP 422 con código `NO_CALCULABLE_LOCATIONS`.
> - Calcula `netPremium` por ubicación con el desglose de 14 componentes especificados en `docs/api-contracts.md` sección 7.
> - `netPremium` total = suma de `netPremium` de ubicaciones calculables.
> - `commercialPremium` = `netPremium` total × `commercialFactor` (proviene de tarifas).
> - Persiste en una única transacción: `Quote.quoteStatus = CALCULATED` + `CalculationResultJpa` + `PremiumByLocationJpa[]`.
> - Versionado optimista obligatorio: la `version` del request debe coincidir con la almacenada en `QuoteJpa`.

### Historias de Usuario

#### HU-01: Calcular prima de cotización con ubicaciones mixtas

```
Como:        agente de seguros
Quiero:      ejecutar POST /v1/quotes/{folio}/calculate
Para:        obtener la prima neta y comercial de la cotización, con desglose por ubicación,
             y continuar el flujo hacia emisión del seguro

Prioridad:   Alta
Estimación:  XL
Dependencias: SPEC-002 (Quote), SPEC-004 (Locations), SPEC-006 (CoverageOptions)
Capa:        Backend
```

#### Criterios de Aceptación — HU-01

**Happy Path — todas las ubicaciones calculables**
```gherkin
CRITERIO-1.1: Cálculo exitoso con todas las ubicaciones completas
  Dado que:  existe el folio "FOL-2026-00042" con version 7
             Y tiene 2 ubicaciones, ambas con zipCode, fireKey y garantías tarifables
             Y el core service retorna tarifas válidas en GET /v1/tariffs
  Cuando:    se realiza POST /v1/quotes/FOL-2026-00042/calculate con { "version": 7 }
  Entonces:  la respuesta tiene HTTP 200
             Y "quoteStatus" es "CALCULATED"
             Y "netPremium" es la suma de las primas netas de ambas ubicaciones
             Y "commercialPremium" es netPremium × commercialFactor de las tarifas
             Y "premiumsByLocation" contiene 2 entradas, ambas con "calculable": true
             Y cada entrada incluye "coverageBreakdown" con los 14 componentes
             Y "version" se incrementó en 1
             Y "calculatedAt" es un timestamp ISO-8601 UTC
```

**Happy Path — ubicaciones mixtas (completa + incompleta)**
```gherkin
CRITERIO-1.2: Cálculo parcial con una ubicación incompleta
  Dado que:  existe el folio "FOL-2026-00042" con 2 ubicaciones
             Y la ubicación 1 tiene zipCode, fireKey y garantías tarifables
             Y la ubicación 2 NO tiene zipCode
  Cuando:    se realiza POST /v1/quotes/FOL-2026-00042/calculate con { "version": 7 }
  Entonces:  la respuesta tiene HTTP 200
             Y "premiumsByLocation[0].calculable" es true con netPremium > 0
             Y "premiumsByLocation[1].calculable" es false
             Y "premiumsByLocation[1].netPremium" es null
             Y "premiumsByLocation[1].blockingAlerts" contiene {"code": "MISSING_ZIP_CODE", ...}
             Y "netPremium" total solo considera la ubicación 1
             Y "quoteStatus" es "CALCULATED"
```

**Error Path — ninguna ubicación calculable**
```gherkin
CRITERIO-1.3: Todas las ubicaciones son incompletas
  Dado que:  existe el folio "FOL-2026-00042" con 2 ubicaciones, ninguna con fireKey
  Cuando:    se realiza POST /v1/quotes/FOL-2026-00042/calculate
  Entonces:  la respuesta tiene HTTP 422
             Y el body es {"error": "No calculable locations", "code": "NO_CALCULABLE_LOCATIONS"}
             Y "quoteStatus" permanece sin cambios (no es CALCULATED)
```

**Error Path — conflicto de versión**
```gherkin
CRITERIO-1.4: Versión del request no coincide con la almacenada
  Dado que:  la versión actual del folio "FOL-2026-00042" es 8
  Cuando:    se realiza POST /v1/quotes/FOL-2026-00042/calculate con { "version": 7 }
  Entonces:  la respuesta tiene HTTP 409
             Y el body es {"error": "Optimistic lock conflict", "code": "VERSION_CONFLICT"}
             Y no se persiste ningún resultado de cálculo
```

**Error Path — folio inexistente**
```gherkin
CRITERIO-1.5: Folio no registrado en la base de datos
  Dado que:  el folio "FOL-9999-00001" no existe
  Cuando:    se realiza POST /v1/quotes/FOL-9999-00001/calculate con { "version": 1 }
  Entonces:  la respuesta tiene HTTP 404
             Y el body es {"error": "Folio not found", "code": "FOLIO_NOT_FOUND"}
```

**Edge Case — recálculo (idempotencia)**
```gherkin
CRITERIO-1.6: Recalcular cotización ya calculada
  Dado que:  el folio "FOL-2026-00042" ya fue calculado (quoteStatus = CALCULATED)
             Y la versión actual es 8
  Cuando:    se realiza POST /v1/quotes/FOL-2026-00042/calculate con { "version": 8 }
  Entonces:  la respuesta tiene HTTP 200
             Y el resultado anterior se reemplaza por el nuevo
             Y "quoteStatus" sigue siendo "CALCULATED"
             Y "version" incrementa a 9
```

**Edge Case — ubicación sin garantía tarifable**
```gherkin
CRITERIO-1.7: Ubicación con zipCode y fireKey pero sin garantías tarifables
  Dado que:  la ubicación 1 tiene zipCode y fireKey válidos pero garantías con tarifable = false
  Cuando:    se realiza POST /v1/quotes/{folio}/calculate
  Entonces:  la ubicación 1 tiene "calculable": false
             Y "blockingAlerts" contiene {"code": "MISSING_TARIFABLE_GUARANTEE", ...}
```

### Reglas de Negocio

1. **Calculabilidad de ubicación:** una ubicación es calculable si y solo si cumple las tres condiciones: (a) `zipCode` no nulo/vacío, (b) `businessLine.fireKey` no nulo/vacío, (c) al menos una garantía con `tarifable = true` según catálogo `GET /v1/catalogs/guarantees` del core.
2. **Alertas sin bloqueo:** las ubicaciones no calculables generan `blockingAlerts` con el código correspondiente pero **no impiden** calcular las demás. El cálculo continúa.
3. **Excepción total:** si **todas** las ubicaciones son no calculables → HTTP 422 `NO_CALCULABLE_LOCATIONS`. No se persiste ningún resultado.
4. **Prima neta total:** suma exclusiva de las `netPremium` de ubicaciones calculables.
5. **Prima comercial:** `commercialPremium = netPremium × tariffs.commercialFactor`. El `commercialFactor` viene del core service `GET /v1/tariffs`. Supuesto documentado: `1.16` si el core no lo retorna.
6. **Persistencia atómica:** la operación de persistencia (actualizar `quoteStatus`, guardar `CalculationResultJpa`, guardar `PremiumByLocationJpa[]`) ocurre en **una sola transacción**. Si alguna falla, se revierte todo.
7. **Reemplazo en recálculo:** si ya existe un `CalculationResultJpa` para el folio, se elimina y reemplaza. El recálculo es idempotente y reemplaza el resultado anterior.
8. **Versionado optimista:** la `version` del request debe igualar la `version` almacenada en `QuoteJpa`; de lo contrario → HTTP 409 `VERSION_CONFLICT`. Al persistir, `@Version` de `QuoteJpa` se incrementa automáticamente por Hibernate.
9. **Estado de cotización:** al persistir exitosamente, `Quote.quoteStatus` pasa a `CALCULATED`.
10. **Lógica de cálculo en dominio:** `CalculationService` es un domain service puro (POJO, sin anotaciones Spring, sin ports). Cada componente del desglose tiene su propio método privado y su propio test unitario.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad | Almacén | Cambios | Descripción |
|---------|---------|---------|-------------|
| `CalculationResultJpa` | tabla `calculation_results` | **nueva** | Resultado de cálculo con FK a `quotes` |
| `PremiumByLocationJpa` | tabla `premiums_by_location` | **nueva** | Prima por ubicación con FK a `calculation_results` |
| `QuoteJpa` | tabla `quotes` | sin cambios estructurales | `@Version` y `quoteStatus` actualizados al calcular |

---

#### Tabla `calculation_results` (migración V8 — nueva)

| Columna | Tipo SQL | Obligatorio | Constraint | Descripción |
|---------|----------|-------------|------------|-------------|
| `id` | `BIGSERIAL` | sí | PK | Identificador interno |
| `quote_id` | `BIGINT` | sí | FK → `quotes.id` ON DELETE CASCADE, UNIQUE | Cotización propietaria (1:1) |
| `net_premium` | `DECIMAL(15,2)` | sí | NOT NULL | Prima neta total de ubicaciones calculables |
| `commercial_premium` | `DECIMAL(15,2)` | sí | NOT NULL | Prima comercial total |
| `calculated_at` | `TIMESTAMP WITH TIME ZONE` | sí | NOT NULL | Timestamp del cálculo |

---

#### Tabla `premiums_by_location` (migración V9 — nueva)

| Columna | Tipo SQL | Obligatorio | Constraint | Descripción |
|---------|----------|-------------|------------|-------------|
| `id` | `BIGSERIAL` | sí | PK | Identificador interno |
| `calculation_result_id` | `BIGINT` | sí | FK → `calculation_results.id` ON DELETE CASCADE | Resultado padre |
| `location_index` | `INTEGER` | sí | NOT NULL | Índice de la ubicación (1-based) |
| `location_name` | `VARCHAR(255)` | no | — | Nombre de la ubicación |
| `net_premium` | `DECIMAL(15,2)` | no | — | Prima neta (null si no calculable) |
| `commercial_premium` | `DECIMAL(15,2)` | no | — | Prima comercial (null si no calculable) |
| `calculable` | `BOOLEAN` | sí | NOT NULL DEFAULT FALSE | ¿Ubicación calculable? |
| `fire_buildings` | `DECIMAL(15,2)` | no | — | Incendio edificios |
| `fire_contents` | `DECIMAL(15,2)` | no | — | Incendio contenidos |
| `coverage_extension` | `DECIMAL(15,2)` | no | — | Extensión de cobertura |
| `cattev` | `DECIMAL(15,2)` | no | — | Catastrófico TEV |
| `catfhm` | `DECIMAL(15,2)` | no | — | Catastrófico FHM |
| `debris_removal` | `DECIMAL(15,2)` | no | — | Remoción de escombros |
| `extraordinary_expenses` | `DECIMAL(15,2)` | no | — | Gastos extraordinarios |
| `rental_loss` | `DECIMAL(15,2)` | no | — | Pérdida de rentas |
| `business_interruption` | `DECIMAL(15,2)` | no | — | Interrupción de negocio (BI) |
| `electronic_equipment` | `DECIMAL(15,2)` | no | — | Equipo electrónico |
| `theft` | `DECIMAL(15,2)` | no | — | Robo |
| `cash_and_values` | `DECIMAL(15,2)` | no | — | Dinero y valores |
| `glass` | `DECIMAL(15,2)` | no | — | Vidrios |
| `luminous_signage` | `DECIMAL(15,2)` | no | — | Anuncios luminosos |

Constraint adicional: `UNIQUE (calculation_result_id, location_index)` — una ubicación no se repite en el mismo resultado.

---

#### Tabla `premium_location_blocking_alerts` (migración V9 — nueva, @ElementCollection)

| Columna | Tipo SQL | Descripción |
|---------|----------|-------------|
| `premium_by_location_id` | `BIGINT` | FK → `premiums_by_location.id` ON DELETE CASCADE |
| `alert_code` | `VARCHAR(50)` | Código de alerta (ej. `MISSING_ZIP_CODE`) |
| `alert_message` | `VARCHAR(255)` | Mensaje legible |

---

#### Índices / Constraints

- `pk_calculation_results` — PRIMARY KEY en `id`
- `uq_calculation_results_quote_id` — UNIQUE en `quote_id`
- `idx_calculation_results_quote_id` — índice en `quote_id`
- `pk_premiums_by_location` — PRIMARY KEY en `id`
- `fk_pbl_calculation_result_id` — FK a `calculation_results.id`
- `uq_pbl_calc_index` — UNIQUE `(calculation_result_id, location_index)`
- `idx_pbl_calculation_result_id` — índice en `calculation_result_id`

---

#### Migraciones Flyway

```sql
-- V8__create_calculation_results_table.sql
CREATE TABLE IF NOT EXISTS calculation_results (
    id                 BIGSERIAL PRIMARY KEY,
    quote_id           BIGINT            NOT NULL UNIQUE
        REFERENCES quotes(id) ON DELETE CASCADE,
    net_premium        DECIMAL(15,2)     NOT NULL,
    commercial_premium DECIMAL(15,2)     NOT NULL,
    calculated_at      TIMESTAMPTZ       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_calculation_results_quote_id ON calculation_results(quote_id);
```

```sql
-- V9__create_premiums_by_location_table.sql
CREATE TABLE IF NOT EXISTS premiums_by_location (
    id                       BIGSERIAL PRIMARY KEY,
    calculation_result_id    BIGINT          NOT NULL
        REFERENCES calculation_results(id) ON DELETE CASCADE,
    location_index           INTEGER         NOT NULL,
    location_name            VARCHAR(255),
    net_premium              DECIMAL(15,2),
    commercial_premium       DECIMAL(15,2),
    calculable               BOOLEAN         NOT NULL DEFAULT FALSE,
    fire_buildings           DECIMAL(15,2),
    fire_contents            DECIMAL(15,2),
    coverage_extension       DECIMAL(15,2),
    cattev                   DECIMAL(15,2),
    catfhm                   DECIMAL(15,2),
    debris_removal           DECIMAL(15,2),
    extraordinary_expenses   DECIMAL(15,2),
    rental_loss              DECIMAL(15,2),
    business_interruption    DECIMAL(15,2),
    electronic_equipment     DECIMAL(15,2),
    theft                    DECIMAL(15,2),
    cash_and_values          DECIMAL(15,2),
    glass                    DECIMAL(15,2),
    luminous_signage         DECIMAL(15,2),
    CONSTRAINT uq_pbl_calc_index UNIQUE (calculation_result_id, location_index)
);

CREATE INDEX IF NOT EXISTS idx_pbl_calculation_result_id ON premiums_by_location(calculation_result_id);

CREATE TABLE IF NOT EXISTS premium_location_blocking_alerts (
    premium_by_location_id  BIGINT          NOT NULL
        REFERENCES premiums_by_location(id) ON DELETE CASCADE,
    alert_code              VARCHAR(50)     NOT NULL,
    alert_message           VARCHAR(255)    NOT NULL
);
```

---

#### Domain Models

```java
// calculation/domain/model/CoverageBreakdown.java
public record CoverageBreakdown(
    BigDecimal fireBuildings,
    BigDecimal fireContents,
    BigDecimal coverageExtension,
    BigDecimal cattev,
    BigDecimal catfhm,
    BigDecimal debrisRemoval,
    BigDecimal extraordinaryExpenses,
    BigDecimal rentalLoss,
    BigDecimal businessInterruption,
    BigDecimal electronicEquipment,
    BigDecimal theft,
    BigDecimal cashAndValues,
    BigDecimal glass,
    BigDecimal luminousSignage
) {}

// calculation/domain/model/PremiumByLocation.java
public record PremiumByLocation(
    int index,
    String locationName,
    BigDecimal netPremium,       // null si no calculable
    BigDecimal commercialPremium, // null si no calculable
    boolean calculable,
    CoverageBreakdown coverageBreakdown, // null si no calculable
    List<BlockingAlert> blockingAlerts
) {}

// calculation/domain/model/CalculationResult.java
public record CalculationResult(
    String folioNumber,
    BigDecimal netPremium,
    BigDecimal commercialPremium,
    List<PremiumByLocation> premiumsByLocation,
    Instant calculatedAt
) {}

// calculation/domain/model/Tariff.java
public record Tariff(
    BigDecimal fireRate,
    BigDecimal fireContentsRate,
    BigDecimal coverageExtensionFactor,
    BigDecimal cattevFactor,
    BigDecimal catfhmFactor,
    BigDecimal debrisRemovalFactor,
    BigDecimal extraordinaryExpensesFactor,
    BigDecimal rentalLossRate,
    BigDecimal businessInterruptionRate,
    BigDecimal electronicEquipmentRate,
    BigDecimal theftRate,
    BigDecimal cashAndValuesRate,
    BigDecimal glassRate,
    BigDecimal luminousSignageRate,
    BigDecimal commercialFactor
) {}
```

> **Nota:** `BlockingAlert` ya existe en el módulo `location`. Se reutiliza desde `com.sofka.insurancequoter.back.location.domain.model.BlockingAlert` o se mueve a un paquete `shared`.

---

### Lógica de Cálculo — CalculationService

`CalculationService` es un domain service puro (sin anotaciones Spring, sin puertos). Recibe una `Location` y un `Tariff` para calcular el desglose.

**Mapeo de garantías a componentes:**

| Componente | Código de garantía | Fórmula |
|-----------|-------------------|---------|
| `fireBuildings` | `GUA-FIRE` | `sum(insuredValue para GUA-FIRE) × fireRate` |
| `fireContents` | `GUA-FIRE-CONT` | `sum(insuredValue para GUA-FIRE-CONT) × fireContentsRate` |
| `coverageExtension` | derivado | `(fireBuildings + fireContents) × coverageExtensionFactor` |
| `cattev` | derivado | `(fireBuildings + fireContents) × cattevFactor` |
| `catfhm` | derivado | `(fireBuildings + fireContents) × catfhmFactor` |
| `debrisRemoval` | derivado | `(fireBuildings + fireContents) × debrisRemovalFactor` |
| `extraordinaryExpenses` | derivado | `(fireBuildings + fireContents) × extraordinaryExpensesFactor` |
| `rentalLoss` | `GUA-RENTAL` | `sum(insuredValue para GUA-RENTAL) × rentalLossRate` |
| `businessInterruption` | `GUA-BI` | `sum(insuredValue para GUA-BI) × businessInterruptionRate` |
| `electronicEquipment` | `GUA-ELEC` | `sum(insuredValue para GUA-ELEC) × electronicEquipmentRate` |
| `theft` | `GUA-THEFT` | `sum(insuredValue para GUA-THEFT) × theftRate` |
| `cashAndValues` | `GUA-CASH` | `sum(insuredValue para GUA-CASH) × cashAndValuesRate` |
| `glass` | `GUA-GLASS` | `sum(insuredValue para GUA-GLASS) × glassRate` |
| `luminousSignage` | `GUA-SIGN` | `sum(insuredValue para GUA-SIGN) × luminousSignageRate` |

**netPremium de ubicación** = suma de los 14 componentes anteriores (los componentes derivados usan el resultado del mismo cálculo, no los valores de guarantees).

**commercialPremium de ubicación** = `netPremium × commercialFactor`

**Supuestos documentados** (datos no entregados en el reto, resueltos explícitamente):
- Los rates para `fireContentsRate`, `coverageExtensionFactor`, `debrisRemovalFactor`, `extraordinaryExpensesFactor`, `rentalLossRate`, `businessInterruptionRate`, `cashAndValuesRate`, `glassRate`, `luminousSignageRate` y `commercialFactor` se añaden al stub de `GET /v1/tariffs` del core service. Valores por defecto documentados en `Insurance-Quoter-Core/src/main/resources/fixtures/tariffs.json`.
- El desglose de `cattev` y `catfhm` usa factores planos de las tarifas (no por zona). Se asume que `cattevFactor` y `catfhmFactor` son factores globales; una extensión futura podría multiplicarlos por un factor de zona procedente de `catalogoCpZonas`.

```java
// calculation/domain/service/CalculationService.java
public class CalculationService {

    public boolean isCalculable(Location location, Set<String> tarifableCodes) { ... }

    public List<BlockingAlert> getBlockingAlerts(Location location, Set<String> tarifableCodes) { ... }

    public PremiumByLocation calculateLocation(Location location, Tariff tariff, Set<String> tarifableCodes) { ... }

    // Métodos privados — uno por componente
    private BigDecimal calculateFireBuildings(Location location, Tariff tariff) { ... }
    private BigDecimal calculateFireContents(Location location, Tariff tariff) { ... }
    private BigDecimal calculateCoverageExtension(BigDecimal firePremium, Tariff tariff) { ... }
    private BigDecimal calculateCattev(BigDecimal firePremium, Tariff tariff) { ... }
    private BigDecimal calculateCatfhm(BigDecimal firePremium, Tariff tariff) { ... }
    private BigDecimal calculateDebrisRemoval(BigDecimal firePremium, Tariff tariff) { ... }
    private BigDecimal calculateExtraordinaryExpenses(BigDecimal firePremium, Tariff tariff) { ... }
    private BigDecimal calculateRentalLoss(Location location, Tariff tariff) { ... }
    private BigDecimal calculateBusinessInterruption(Location location, Tariff tariff) { ... }
    private BigDecimal calculateElectronicEquipment(Location location, Tariff tariff) { ... }
    private BigDecimal calculateTheft(Location location, Tariff tariff) { ... }
    private BigDecimal calculateCashAndValues(Location location, Tariff tariff) { ... }
    private BigDecimal calculateGlass(Location location, Tariff tariff) { ... }
    private BigDecimal calculateLuminousSignage(Location location, Tariff tariff) { ... }

    private BigDecimal sumInsuredValueByCode(Location location, String guaranteeCode) { ... }
}
```

---

### API Endpoints

#### POST /v1/quotes/{folio}/calculate

- **Descripción:** Ejecuta cálculo de prima para todas las ubicaciones calculables; persiste resultado.
- **Auth requerida:** no
- **Path param:** `folio` — número de folio (ej. `FOL-2026-00042`)
- **Request Body:**
  ```json
  {
    "version": 7
  }
  ```
- **Response 200:**
  ```json
  {
    "folioNumber": "FOL-2026-00042",
    "quoteStatus": "CALCULATED",
    "netPremium": 48500.00,
    "commercialPremium": 56260.00,
    "premiumsByLocation": [
      {
        "index": 1,
        "locationName": "Bodega Principal",
        "netPremium": 48500.00,
        "commercialPremium": 56260.00,
        "calculable": true,
        "coverageBreakdown": {
          "fireBuildings": 20000.00,
          "fireContents": 15000.00,
          "coverageExtension": 3500.00,
          "cattev": 4000.00,
          "catfhm": 2500.00,
          "debrisRemoval": 1500.00,
          "extraordinaryExpenses": 1000.00,
          "rentalLoss": 0.00,
          "businessInterruption": 0.00,
          "electronicEquipment": 500.00,
          "theft": 0.00,
          "cashAndValues": 0.00,
          "glass": 0.00,
          "luminousSignage": 0.00
        },
        "blockingAlerts": []
      },
      {
        "index": 2,
        "locationName": "Oficina Sur",
        "netPremium": null,
        "commercialPremium": null,
        "calculable": false,
        "coverageBreakdown": null,
        "blockingAlerts": [
          { "code": "MISSING_ZIP_CODE", "message": "Código postal requerido" }
        ]
      }
    ],
    "calculatedAt": "2026-04-22T16:00:00Z",
    "version": 8
  }
  ```
- **Response 404:** `{"error": "Folio not found", "code": "FOLIO_NOT_FOUND"}`
- **Response 409:** `{"error": "Optimistic lock conflict", "code": "VERSION_CONFLICT"}`
- **Response 422 — todas incompletas:** `{"error": "No calculable locations", "code": "NO_CALCULABLE_LOCATIONS"}`

---

### Arquitectura Hexagonal — Descomposición de Componentes

#### Bounded context: `calculation`

```
com.sofka.insurancequoter.back.calculation/
├── domain/
│   ├── model/
│   │   ├── CalculationResult.java           ← Record: folioNumber, netPremium, commercialPremium, premiumsByLocation, calculatedAt
│   │   ├── PremiumByLocation.java           ← Record: index, locationName, netPremium, commercialPremium, calculable, coverageBreakdown, blockingAlerts
│   │   ├── CoverageBreakdown.java           ← Record: 14 componentes BigDecimal
│   │   └── Tariff.java                      ← Record: todos los rates y factores técnicos
│   ├── service/
│   │   └── CalculationService.java          ← Domain service puro: isCalculable, calculateLocation, 14 métodos privados
│   └── port/
│       ├── in/
│       │   └── CalculatePremiumUseCase.java ← Input Port: CalculationResult calculate(CalculatePremiumCommand)
│       └── out/
│           ├── QuoteCalculationReader.java  ← Output Port: QuoteCalculationSnapshot getSnapshot(String folioNumber)
│           ├── CalculationResultRepository.java ← Output Port: void persist(String folioNumber, CalculationResult result)
│           └── TariffClient.java           ← Output Port: Tariff fetchTariffs()
├── application/
│   └── usecase/
│       ├── CalculatePremiumUseCaseImpl.java ← Orquesta: leer snapshot → validar versión → fetchTariffs → calcular → persistir
│       ├── command/
│       │   └── CalculatePremiumCommand.java ← Record: folioNumber, version
│       ├── dto/
│       │   └── QuoteCalculationSnapshot.java ← Record: folioNumber, version, locations, coverageOptions
│       └── exception/
│           └── NoCalculableLocationsException.java
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   └── rest/
    │   │       ├── CalculationController.java      ← implements CalculationApi
    │   │       ├── swaggerdocs/
    │   │       │   └── CalculationApi.java         ← @Tag "Calculation", @Operation POST /calculate
    │   │       ├── dto/
    │   │       │   ├── request/
    │   │       │   │   └── CalculatePremiumRequest.java   ← { version: Long @NotNull }
    │   │       │   └── response/
    │   │       │       ├── CalculationResponse.java       ← full response
    │   │       │       ├── PremiumByLocationResponse.java
    │   │       │       └── CoverageBreakdownResponse.java
    │   │       └── mapper/
    │   │           └── CalculationRestMapper.java
    │   └── out/
    │       ├── persistence/
    │       │   ├── adapter/
    │       │   │   └── CalculationResultJpaAdapter.java   ← implementa CalculationResultRepository + QuoteCalculationReader
    │       │   ├── repositories/
    │       │   │   ├── CalculationResultJpaRepository.java ← JpaRepository<CalculationResultJpa, Long>
    │       │   │   └── PremiumByLocationJpaRepository.java ← JpaRepository<PremiumByLocationJpa, Long>
    │       │   ├── entities/
    │       │   │   ├── CalculationResultJpa.java          ← @Entity tabla calculation_results; @OneToOne QuoteJpa
    │       │   │   └── PremiumByLocationJpa.java          ← @Entity tabla premiums_by_location; @ManyToOne CalculationResultJpa; @ElementCollection blockingAlerts
    │       │   └── mappers/
    │       │       └── CalculationPersistenceMapper.java
    │       └── http/
    │           ├── adapter/
    │           │   └── TariffClientAdapter.java           ← implementa TariffClient; GET /v1/tariffs al core
    │           └── dto/
    │               └── TariffResponse.java                ← deserialization del core
    └── config/
        └── CalculationConfig.java                        ← @Bean wiring use case + RestClient
```

---

#### Contratos de interfaces clave

```java
// domain/port/in/CalculatePremiumUseCase.java
public interface CalculatePremiumUseCase {
    CalculationResult calculate(CalculatePremiumCommand command);
}

// domain/port/out/QuoteCalculationReader.java
public interface QuoteCalculationReader {
    QuoteCalculationSnapshot getSnapshot(String folioNumber); // lanza FolioNotFoundException si no existe
}

// domain/port/out/CalculationResultRepository.java
public interface CalculationResultRepository {
    void persist(String folioNumber, CalculationResult result); // actualiza quoteStatus + upsert entidades
}

// domain/port/out/TariffClient.java
public interface TariffClient {
    Tariff fetchTariffs(); // llama GET /v1/tariffs al core; lanza CoreServiceException si falla
}

// application/usecase/dto/QuoteCalculationSnapshot.java
public record QuoteCalculationSnapshot(
    String folioNumber,
    long version,
    List<Location> locations,     // domain model de location context
    List<CoverageOption> coverageOptions  // domain model de coverage context
) {}
```

---

#### Flujo de llamada — POST /v1/quotes/{folio}/calculate

```
CalculationController.calculate(folio, request)
  → CalculatePremiumUseCaseImpl.calculate(command)
      1. QuoteCalculationReader.getSnapshot(folio)           ← 404 si no existe
      2. if (snapshot.version() != command.version())
            throw VersionConflictException                   ← 409
      3. TariffClient.fetchTariffs()                         ← CoreServiceException si falla
      4. GuaranteeCatalogClient.fetchGuarantees()            ← obtener tarifable codes (reutilizar de coverage context)
      5. Para cada location en snapshot.locations():
         a. CalculationService.isCalculable(location, tarifableCodes)
         b. Si no calculable: getBlockingAlerts() → PremiumByLocation(calculable=false)
         c. Si calculable: CalculationService.calculateLocation(location, tariff, tarifableCodes)
      6. Si todos los premiumsByLocation tienen calculable=false:
            throw NoCalculableLocationsException             ← 422
      7. netPremium = sum(pbl.netPremium() where calculable=true)
      8. commercialPremium = netPremium × tariff.commercialFactor()
      9. CalculationResultRepository.persist(folio, result) ← @Transactional en adapter
     10. return CalculationResult
  ← ResponseEntity<CalculationResponse>(200)
```

---

#### Manejo de excepciones

| Excepción | HTTP | Código |
|-----------|------|--------|
| `FolioNotFoundException` | 404 | `FOLIO_NOT_FOUND` |
| `VersionConflictException` | 409 | `VERSION_CONFLICT` |
| `NoCalculableLocationsException` | 422 | `NO_CALCULABLE_LOCATIONS` |
| `CoreServiceException` | 502 | `CORE_SERVICE_UNAVAILABLE` |

> `GlobalExceptionHandler` (existente de SPEC-002) debe agregar handler para `NoCalculableLocationsException` → 422 y `CoreServiceException` → 502.

---

#### Persistencia atómica — CalculationResultJpaAdapter.persist()

```
@Transactional
persist(folioNumber, result):
  1. quoteJpa = quoteJpaRepository.findByFolioNumber(folioNumber)
  2. quoteJpa.setQuoteStatus(CALCULATED)
     quoteJpa.setUpdatedAt(Instant.now())         ← dispara @Version increment
  3. calculationResultJpaRepository.deleteByQuoteId(quoteJpa.getId())   ← borra anterior si existe
  4. calculationResultJpa = mapper.toJpa(result, quoteJpa)
  5. saved = calculationResultJpaRepository.save(calculationResultJpa)
  6. premiumByLocationJpaList = result.premiumsByLocation().map(pbl → mapper.toJpa(pbl, saved))
  7. premiumByLocationJpaRepository.saveAll(premiumByLocationJpaList)
```

### Notas de Implementación

- **Cross-context read en `CalculationResultJpaAdapter`:** para implementar `QuoteCalculationReader`, el adapter en `calculation/infrastructure` puede inyectar `QuoteJpaRepository`, `LocationJpaRepository` y `CoverageOptionJpaRepository` (de otros contextos). Esto es válido en la capa infrastructure — los bounded contexts solo son estrictos en domain/application.
- **`CalculationService` como POJO:** no usar `@Service` en `CalculationService`. Es instanciado directamente desde `CalculatePremiumUseCaseImpl` o inyectado vía `@Bean` en `CalculationConfig`. Esto lo hace testeable sin contexto Spring.
- **`GuaranteeCatalogClient` reutilizado:** el output port `GuaranteeCatalogClient` ya existe en el contexto `coverage`. Para no duplicarlo, se puede reusar desde `coverage/domain/port/out/GuaranteeCatalogClient.java` referenciando el puerto directamente. Si el proyecto sigue bounded contexts estrictos, duplicar la interfaz en `calculation/domain/port/out/` con el mismo contrato.
- **Precisión decimal:** todos los cálculos de prima usan `BigDecimal` con `RoundingMode.HALF_UP` y escala 2 al final de cada operación intermedia. Prohibido usar `double` o `float`.
- **Escala intermedia:** durante el cálculo intermedio mantener escala 8 para acumular precisión; redondear a 2 decimales solo en el resultado final de cada componente.
- **Ubicaciones con `validationStatus = INCOMPLETE`:** el cálculo no rechaza ubicaciones basándose en `validationStatus`. La lógica de calculabilidad es independiente del estado de validación y se recalcula en tiempo de ejecución según las reglas de negocio 1.
- **Código `MISSING_TARIFABLE_GUARANTEE`:** agregar este nuevo código al enum `BlockingAlertCode` del contexto `location` (o a un enum propio en `calculation/domain/model/`).

---

## 3. LISTA DE TAREAS

> Checklist accionable. Marcar cada ítem (`[x]`) al completarlo.
> El Orchestrator monitorea este checklist para determinar el progreso.

### Backend

#### Base de Datos

- [ ] Crear migración `V8__create_calculation_results_table.sql` — tabla `calculation_results` con columnas, UNIQUE en `quote_id`, FK a `quotes`, índice `idx_calculation_results_quote_id`
- [ ] Crear migración `V9__create_premiums_by_location_table.sql` — tabla `premiums_by_location` con 14 columnas de desglose, UNIQUE `(calculation_result_id, location_index)`, FK a `calculation_results`; tabla `premium_location_blocking_alerts` con FK a `premiums_by_location`

#### Dominio — Modelos

- [ ] Crear record `CoverageBreakdown` en `calculation/domain/model/` — 14 campos `BigDecimal`
- [ ] Crear record `PremiumByLocation` en `calculation/domain/model/` — index, locationName, netPremium, commercialPremium, calculable, coverageBreakdown, blockingAlerts
- [ ] Crear record `CalculationResult` en `calculation/domain/model/` — folioNumber, netPremium, commercialPremium, premiumsByLocation, calculatedAt
- [ ] Crear record `Tariff` en `calculation/domain/model/` — 15 campos BigDecimal (rates + commercialFactor)
- [ ] Agregar `MISSING_TARIFABLE_GUARANTEE` al enum `BlockingAlertCode` en `location/domain/model/`

#### Dominio — Puertos

- [ ] Crear Input Port `CalculatePremiumUseCase` en `calculation/domain/port/in/`
- [ ] Crear Output Port `QuoteCalculationReader` en `calculation/domain/port/out/`
- [ ] Crear Output Port `CalculationResultRepository` en `calculation/domain/port/out/`
- [ ] Crear Output Port `TariffClient` en `calculation/domain/port/out/`

#### Dominio — Service

- [ ] Crear `CalculationService` en `calculation/domain/service/` — POJO puro sin anotaciones Spring
  - [ ] Método `isCalculable(Location, Set<String>)` — valida zipCode + fireKey + garantía tarifable
  - [ ] Método `getBlockingAlerts(Location, Set<String>)` — retorna lista de alertas
  - [ ] Método privado `calculateFireBuildings(Location, Tariff)` — GUA-FIRE × fireRate
  - [ ] Método privado `calculateFireContents(Location, Tariff)` — GUA-FIRE-CONT × fireContentsRate
  - [ ] Método privado `calculateCoverageExtension(BigDecimal firePremium, Tariff)` — firePremium × coverageExtensionFactor
  - [ ] Método privado `calculateCattev(BigDecimal firePremium, Tariff)` — firePremium × cattevFactor
  - [ ] Método privado `calculateCatfhm(BigDecimal firePremium, Tariff)` — firePremium × catfhmFactor
  - [ ] Método privado `calculateDebrisRemoval(BigDecimal firePremium, Tariff)` — firePremium × debrisRemovalFactor
  - [ ] Método privado `calculateExtraordinaryExpenses(BigDecimal firePremium, Tariff)` — firePremium × extraordinaryExpensesFactor
  - [ ] Método privado `calculateRentalLoss(Location, Tariff)` — GUA-RENTAL × rentalLossRate
  - [ ] Método privado `calculateBusinessInterruption(Location, Tariff)` — GUA-BI × businessInterruptionRate
  - [ ] Método privado `calculateElectronicEquipment(Location, Tariff)` — GUA-ELEC × electronicEquipmentRate
  - [ ] Método privado `calculateTheft(Location, Tariff)` — GUA-THEFT × theftRate
  - [ ] Método privado `calculateCashAndValues(Location, Tariff)` — GUA-CASH × cashAndValuesRate
  - [ ] Método privado `calculateGlass(Location, Tariff)` — GUA-GLASS × glassRate
  - [ ] Método privado `calculateLuminousSignage(Location, Tariff)` — GUA-SIGN × luminousSignageRate
  - [ ] Método público `calculateLocation(Location, Tariff, Set<String>)` — orquesta todos los componentes → `PremiumByLocation`
  - [ ] Método privado `sumInsuredValueByCode(Location, String)` — helper para filtrar garantías por código

#### Aplicación

- [ ] Crear record `CalculatePremiumCommand` en `calculation/application/usecase/command/` — folioNumber, version
- [ ] Crear record `QuoteCalculationSnapshot` en `calculation/application/usecase/dto/` — folioNumber, version, locations, coverageOptions
- [ ] Crear `NoCalculableLocationsException` en `calculation/application/usecase/exception/`
- [ ] Crear `CalculatePremiumUseCaseImpl` en `calculation/application/usecase/` — orquesta: getSnapshot → validar versión → fetchTariffs → fetchGuarantees → calcular por ubicación → validar que hay calculables → consolidar → persistir

#### Infraestructura — Entidades JPA

- [ ] Crear `CalculationResultJpa` en `calculation/infrastructure/adapter/out/persistence/entities/` — `@Entity`, `@OneToOne(QuoteJpa)`, campos según diseño
- [ ] Crear `PremiumByLocationJpa` en `calculation/infrastructure/adapter/out/persistence/entities/` — `@Entity`, `@ManyToOne(CalculationResultJpa)`, 14 campos DECIMAL, `@ElementCollection` para blockingAlerts en tabla `premium_location_blocking_alerts`

#### Infraestructura — Repositorios JPA

- [ ] Crear `CalculationResultJpaRepository` en `calculation/infrastructure/adapter/out/persistence/repositories/` — extiende `JpaRepository<CalculationResultJpa, Long>`; método `deleteByQuoteId(Long quoteId)`
- [ ] Crear `PremiumByLocationJpaRepository` en `calculation/infrastructure/adapter/out/persistence/repositories/` — extiende `JpaRepository<PremiumByLocationJpa, Long>`

#### Infraestructura — Persistence Adapter

- [ ] Crear `CalculationPersistenceMapper` en `calculation/infrastructure/adapter/out/persistence/mappers/`
- [ ] Crear `CalculationResultJpaAdapter` en `calculation/infrastructure/adapter/out/persistence/adapter/` — implementa `CalculationResultRepository` + `QuoteCalculationReader`; método `persist` con `@Transactional`; inyecta `QuoteJpaRepository`, `LocationJpaRepository`, `CoverageOptionJpaRepository`, `CalculationResultJpaRepository`, `PremiumByLocationJpaRepository`

#### Infraestructura — HTTP Client

- [ ] Crear `TariffResponse` en `calculation/infrastructure/adapter/out/http/dto/` — deserialización del core
- [ ] Crear `TariffClientAdapter` en `calculation/infrastructure/adapter/out/http/adapter/` — implementa `TariffClient`; llama `GET /v1/tariffs` al core; lanza `CoreServiceException` si falla

#### Infraestructura — REST

- [ ] Crear `CalculatePremiumRequest` en `calculation/infrastructure/adapter/in/rest/dto/request/` — `{ version: Long @NotNull }`
- [ ] Crear `CoverageBreakdownResponse` en `calculation/infrastructure/adapter/in/rest/dto/response/`
- [ ] Crear `PremiumByLocationResponse` en `calculation/infrastructure/adapter/in/rest/dto/response/`
- [ ] Crear `CalculationResponse` en `calculation/infrastructure/adapter/in/rest/dto/response/`
- [ ] Crear `CalculationRestMapper` en `calculation/infrastructure/adapter/in/rest/mapper/`
- [ ] Crear Swagger interface `CalculationApi` en `calculation/infrastructure/adapter/in/rest/swaggerdocs/` — `@Tag "Calculation"`, `@Operation` para POST /calculate con todos los `@ApiResponse`
- [ ] Crear `CalculationController` en `calculation/infrastructure/adapter/in/rest/` — implementa `CalculationApi`; inyecta `CalculatePremiumUseCase` por constructor
- [ ] Actualizar `GlobalExceptionHandler` — agregar handlers para `NoCalculableLocationsException` → 422 `NO_CALCULABLE_LOCATIONS` y `CoreServiceException` → 502 `CORE_SERVICE_UNAVAILABLE`

#### Infraestructura — Core Service Stub (Insurance-Quoter-Core)

- [ ] Extender `GET /v1/tariffs` del core stub para retornar los 15 campos de `Tariff` (documentar valores por defecto en `fixtures/tariffs.json`)

#### Configuración

- [ ] Crear `CalculationConfig` en `calculation/infrastructure/config/` — `@Bean` wiring de `CalculatePremiumUseCaseImpl`, `CalculationService`, `CalculationResultJpaAdapter`, `TariffClientAdapter`

---

### Tests Backend (TDD — escribir el test ANTES de la implementación)

#### CalculationServiceTest (domain service puro — sin Spring)

- [ ] `isCalculable_returnsTrue_whenZipCodeFireKeyAndTarifableGuaranteePresent`
- [ ] `isCalculable_returnsFalse_whenZipCodeMissing`
- [ ] `isCalculable_returnsFalse_whenFireKeyMissing`
- [ ] `isCalculable_returnsFalse_whenNoTarifableGuarantee`
- [ ] `getBlockingAlerts_returnsMissingZipCode_whenZipCodeIsNull`
- [ ] `getBlockingAlerts_returnsMissingFireKey_whenFireKeyIsNull`
- [ ] `getBlockingAlerts_returnsMissingTarifableGuarantee_whenNoTarifableGuarantee`
- [ ] `calculateFireBuildings_returnsInsuredValueTimesRate`
- [ ] `calculateFireBuildings_returnsZero_whenNoGUAFIREGuarantee`
- [ ] `calculateFireContents_returnsInsuredValueTimesRate`
- [ ] `calculateFireContents_returnsZero_whenNoGUAFIRECONTGuarantee`
- [ ] `calculateCoverageExtension_returnsFirePremiumTimesFactor`
- [ ] `calculateCattev_returnsFirePremiumTimesFactor`
- [ ] `calculateCatfhm_returnsFirePremiumTimesFactor`
- [ ] `calculateDebrisRemoval_returnsFirePremiumTimesFactor`
- [ ] `calculateExtraordinaryExpenses_returnsFirePremiumTimesFactor`
- [ ] `calculateRentalLoss_returnsInsuredValueTimesRate`
- [ ] `calculateRentalLoss_returnsZero_whenNoGUARENTALGuarantee`
- [ ] `calculateBusinessInterruption_returnsInsuredValueTimesRate`
- [ ] `calculateElectronicEquipment_returnsInsuredValueTimesRate`
- [ ] `calculateTheft_returnsInsuredValueTimesRate`
- [ ] `calculateCashAndValues_returnsInsuredValueTimesRate`
- [ ] `calculateGlass_returnsInsuredValueTimesRate`
- [ ] `calculateLuminousSignage_returnsInsuredValueTimesRate`
- [ ] `calculateLocation_returnsPremiumByLocation_withAllComponents`
- [ ] `calculateLocation_returnsSumOfComponents_asNetPremium`
- [ ] `calculateLocation_appliesCommercialFactor_toNetPremium`
- [ ] `sumInsuredValueByCode_returnsSum_ofMatchingGuarantees`
- [ ] `sumInsuredValueByCode_returnsZero_whenNoMatchingGuarantee`

#### CalculatePremiumUseCaseImplTest (con Mockito)

- [ ] `calculate_throwsFolioNotFoundException_whenFolioNotFound`
- [ ] `calculate_throwsVersionConflictException_whenVersionMismatch`
- [ ] `calculate_throwsNoCalculableLocationsException_whenAllLocationsIncomplete`
- [ ] `calculate_returnsTotalNetPremium_asSum_ofCalculableLocations`
- [ ] `calculate_setsQuoteStatusToCalculated_onSuccess`
- [ ] `calculate_persistsResult_withCorrectPremiums`
- [ ] `calculate_includesBlockingAlerts_forNonCalculableLocations`
- [ ] `calculate_succeeds_withOnlyOneCalculableLocation`
- [ ] `calculate_callsTariffClient_once`
- [ ] `calculate_isIdempotent_onRecalculation` (recalcular reemplaza resultado anterior)

#### CalculationResultJpaAdapterTest (con Mockito + slice test)

- [ ] `getSnapshot_returnsSnapshot_withLocationsAndCoverageOptions`
- [ ] `getSnapshot_throwsFolioNotFoundException_whenFolioNotFound`
- [ ] `persist_savesCalculationResult_andUpdatesQuoteStatus`
- [ ] `persist_deletesExistingResult_beforeSavingNew` (recálculo)
- [ ] `persist_savesAllPremiumByLocations`
- [ ] `persist_savesBlockingAlerts_forNonCalculableLocations`

#### TariffClientAdapterTest (WireMock)

- [ ] `fetchTariffs_returnsAllFields_onSuccess`
- [ ] `fetchTariffs_throwsCoreServiceException_onCoreError5xx`
- [ ] `fetchTariffs_throwsCoreServiceException_onCoreTimeout`

#### CalculationControllerTest (MockMvc)

- [ ] `calculate_returns200_withFullBreakdown_onSuccess`
- [ ] `calculate_returns200_withNullPremiums_forNonCalculableLocation`
- [ ] `calculate_returns404_whenFolioNotFound`
- [ ] `calculate_returns409_onVersionConflict`
- [ ] `calculate_returns422_whenNoCalculableLocations`
- [ ] `calculate_returns422_whenVersionIsNull` (Bean Validation)
- [ ] `calculate_returns502_whenCoreServiceUnavailable`

#### Tests de Integración

- [ ] `PremiumCalculationIntegrationTest` — `@SpringBootTest` + Testcontainers PostgreSQL + WireMock core: flujo completo con 1 ubicación calculable → HTTP 200 + datos persistidos verificados en DB
- [ ] `PremiumCalculationIntegrationTest` — 2 ubicaciones, 1 calculable + 1 incompleta → HTTP 200 + netPremium solo de la completa + blockingAlert persistida
- [ ] `PremiumCalculationIntegrationTest` — todas incompletas → HTTP 422 NO_CALCULABLE_LOCATIONS + nada persistido
- [ ] `PremiumCalculationIntegrationTest` — version conflict → HTTP 409 + nada persistido
- [ ] `PremiumCalculationIntegrationTest` — recálculo → HTTP 200 + resultado anterior reemplazado en DB

### QA

- [ ] Ejecutar skill `/gherkin-case-generator` → escenarios para CRITERIO-1.1…1.7
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD (transacción compleja, cross-context read, cálculo actuarial, core unavailable, recálculo idempotente)
- [ ] Validar manualmente con Bruno/Postman: POST /v1/quotes/{folio}/calculate
- [ ] Verificar que `coverageBreakdown` es null en ubicaciones no calculables
- [ ] Verificar que recálculo reemplaza el resultado anterior en DB
- [ ] Verificar comportamiento cuando el core service de tarifas no está disponible
- [ ] Verificar que la transacción revierte si falla el guardado de `PremiumByLocationJpa`
- [ ] Verificar precisión decimal en los 14 componentes del desglose
- [ ] Actualizar estado spec: `status: IMPLEMENTED`

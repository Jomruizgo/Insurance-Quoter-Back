# Matriz de Riesgos — SPEC-007 Premium Calculation

**Generado:** 2026-04-22
**Analista QA:** risk-identifier (ASDD)
**Spec de referencia:** `.claude/specs/premium-calculation.spec.md`
**Commit de código analizado:** rama activa (Insurance-Quoter-Back)

---

## Resumen

- **Total de riesgos identificados:** 15
- **Alto (A) — obligatorio mitigar antes de producción:** 6
- **Significativo (S) — recomendado mitigar:** 6
- **Deseable (D) — opcional, mejora la calidad:** 3

---

## Matriz detallada

| ID | Área | Descripción | Impacto | Probabilidad | ASD | Mitigación |
|----|------|-------------|---------|--------------|-----|------------|
| RISK-001 | Cálculo | Escala intermedia fija en RESULT_SCALE=2 para componentes intermedios (fireBuildings, fireContents); la suma de 14 valores de escala 2 puede acumular error de redondeo acumulativo | Prima neta incorrecta por ±0.01–0.14 unidades dependiendo del número de ubicaciones; impacto directo en primas emitidas | Media | **A** | Mantener escala 8 en todos los cálculos intermedios y redondear a 2 solo en `netPremium` final. Actualmente cada `calculateXxx` privado aplica `setScale(RESULT_SCALE, ROUNDING)` antes de ser sumado, violando la nota de implementación de la spec que exige escala 8 en intermedios. Agregar test de precisión con valores decimales que producen diferencias visibles al cambiar el orden de redondeo. |
| RISK-002 | Cálculo | `firePremium` calculado como `fireBuildings.add(fireContents)` ya con escala 2 se usa como base de 5 factores derivados; si los dos componentes base son cero (ninguna garantía GUA-FIRE ni GUA-FIRE-CONT), todos los derivados son cero y el cálculo puede ser aceptado como calculable si existe otra garantía tarifable | Ubicación con valor de garantía exclusivamente en GUA-THEFT puede tener `coverageExtension`, `cattev`, `catfhm`, `debrisRemoval`, `extraordinaryExpenses` todos en cero sin ninguna advertencia; posible subdeclaración de prima | Baja | **S** | Documentar explícitamente en la spec y en tests que esta combinación es intencional. Agregar test de contrato: `calculateLocation_derivedComponentsAreZero_whenNeitherGUAFIREnorGUAFIRECONT`. |
| RISK-003 | Cálculo | No existe protección ante `insuredValue = null` en `Guarantee`; `sumInsuredValueByCode` hace `g.insuredValue()` sin null-check antes del `reduce`; si el mapper de persistencia produce un `Guarantee` con `insuredValue = null`, se lanza `NullPointerException` en el stream | NPE no controlada en `CalculationService`; la excepción escala hasta el controlador y produce HTTP 500 en lugar de 422 con alerta | Media | **A** | Agregar null-check en `sumInsuredValueByCode`: `filter(g -> g.insuredValue() != null)`. Agregar test: `sumInsuredValueByCode_treatsNullInsuredValueAsZero`. |
| RISK-004 | Cálculo | Overflow silencioso en `BigDecimal` no es posible, pero la precisión de la columna `DECIMAL(15,2)` limita a 999,999,999,999,999.99; carteras con `insuredValue` muy alto (ej. 10^14 por ubicación) pueden producir un resultado que supera la precisión del tipo SQL y falla con error de truncamiento en PostgreSQL | Error de inserción en DB con mensaje críptico de PostgreSQL; transacción revierte sin mensaje de negocio útil al cliente | Baja | **S** | Agregar validación de rango en el request de ubicación (`insuredValue <= 9,999,999,999.99` como límite razonable de negocio). Documentar el límite en la spec. |
| RISK-005 | Integración | `TariffClientAdapter.fetchTariffs()` no tiene timeout configurado explícitamente en el `RestClient`; si el core service `GET /v1/tariffs` no responde, el hilo HTTP puede bloquearse indefinidamente | Bajo carga concurrente, agotamiento del pool de threads de Tomcat; el servicio de cotización se vuelve completamente inresponsivo | Alta | **A** | Configurar `connectTimeout` y `readTimeout` en el `RestClient` del `CalculationConfig`. Valor sugerido: connect=2s, read=5s. Agregar test WireMock: `fetchTariffs_throwsCoreServiceException_onCoreTimeout`. Los tests de `TariffClientAdapterTest` aún no existen. |
| RISK-006 | Integración | Cuando `TariffData` del core retorna campos nulos (ej. `commercialFactor = null`), el mapeo en `TariffClientAdapter` construye un record `Tariff` con campos null; en `CalculationService`, la operación `netPremium.multiply(tariff.commercialFactor())` lanza NPE | NPE no controlada que resulta en HTTP 500; la prima no se calcula y el cliente no recibe mensaje de negocio | Alta | **A** | Agregar validación explícita de campos críticos del `Tariff` en `TariffClientAdapter` antes de construir el record (o en el constructor del record `Tariff` con `Objects.requireNonNull`). Agregar test: `fetchTariffs_throwsCoreServiceException_whenCommercialFactorIsNull`. |
| RISK-007 | Integración | El catálogo de garantías (`GuaranteeCatalogClient.fetchGuarantees()`) puede retornar lista vacía si el core no tiene datos; `tarifableCodes` queda vacío y ninguna ubicación pasa la verificación `hasTarifableGuarantee`; todas las ubicaciones generan `MISSING_TARIFABLE_GUARANTEE` y se lanza `NoCalculableLocationsException` | HTTP 422 con código `NO_CALCULABLE_LOCATIONS` cuando el error real es una falla del core; el agente no puede distinguir si el problema es de datos o de infraestructura | Media | **S** | Diferenciar respuesta cuando el catálogo está vacío por falla del core vs. cuando las ubicaciones genuinamente no tienen garantías tarifables. Agregar test en `CalculatePremiumUseCaseImplTest`: `calculate_throws422_whenCatalogIsEmpty`. |
| RISK-008 | Concurrencia | Dos requests simultáneos `POST /v1/quotes/{folio}/calculate` para el mismo folio pueden ejecutar el bloque `deleteByQuoteId + save` en paralelo; ambos leen el mismo `quoteJpa`, ambos actualizan `quoteStatus`, y ambos intentan hacer INSERT en `calculation_results`; el segundo INSERT falla con violación de UNIQUE en `quote_id` | Error de BD `org.postgresql.util.PSQLException: duplicate key value violates unique constraint`; la transacción revierte limpiamente pero el cliente recibe HTTP 500 en lugar de 409 o 200 | Media | **A** | El `@Version` de `QuoteJpa` mitiga parcialmente: el segundo save del quote fallará con `OptimisticLockException`, que sí produce HTTP 409. Verificar que `GlobalExceptionHandler` mapea `OptimisticLockException` a 409. Agregar test de integración con dos hilos concurrentes. |
| RISK-009 | Concurrencia | `getSnapshot` y `persist` son dos operaciones separadas sin un lock de lectura; en el intervalo entre `getSnapshot` (que lee `version`) y el `persist` (que escribe el quote), otro proceso puede modificar el quote e incrementar `version`; el check `snapshot.version() != command.version()` solo valida contra la versión en el momento del `getSnapshot`, no al momento del flush | Resultado de cálculo persistido con datos de un snapshot potencialmente desactualizado; la versión devuelta al cliente puede ser incorrecta | Baja | **S** | La protección de Hibernate `@Version` en `QuoteJpa` al hacer `save(quoteJpa)` en `persist` lanzará `OptimisticLockException` si la versión cambió entre la lectura y el save. Esta protección ya existe y es suficiente. Documentar explícitamente en el adaptador que `@Version` es la segunda barrera. Agregar test de integración que lo verifica. |
| RISK-010 | Persistencia | `deleteByQuoteId` usa `@Modifying` con JPQL; Hibernate puede no haber vaciado el contexto de persistencia antes de ejecutar el DELETE, dejando entidades `CalculationResultJpa` en caché que se re-insertarían al final de la transacción | Violación de constraint UNIQUE en `quote_id`; la transacción entera revierte incluyendo la actualización del `quoteStatus` | Media | **A** | Agregar `@Modifying(clearAutomatically = true)` en el método `deleteByQuoteId` del repositorio. Agregar test de repositorio que verifica el recálculo (delete + insert) en un `@DataJpaTest`. Los tests `CalculationResultJpaAdapterTest` aún no existen. |
| RISK-011 | Persistencia | `@ElementCollection` de `PremiumByLocationJpa.blockingAlerts` usa `FetchType.EAGER`; al cargar una lista de `PremiumByLocationJpa` con `premiumByLocationJpaRepository.saveAll(...)`, Hibernate ejecuta un SELECT adicional por cada elemento de la colección (N+1) | Con N ubicaciones, se ejecutan N queries extras para leer alertas al cargar; en cotizaciones con muchas ubicaciones y recálculos frecuentes, degrada el rendimiento y puede saturar el pool de conexiones | Media | **S** | Evaluar si el `FetchType.EAGER` es necesario en el path de escritura. En el flujo actual de `persist` solo se hace `saveAll`, no se leen las alertas, por lo que el EAGER no aplica al escribir. El riesgo real es en el path de lectura (`getSnapshot` no lee alertas, pero un futuro endpoint de consulta del resultado sí lo haría). Documentar y agregar `@BatchSize` en la colección como mitigación preventiva. |
| RISK-012 | Persistencia | La migración V9 no crea índice en `premium_location_blocking_alerts(premium_by_location_id)`; con cotizaciones de muchas ubicaciones y muchas alertas, las consultas de JOIN para cargar alertas hacen full scan | Degradación de rendimiento en consultas de resultados de cálculo con muchas alertas | Baja | **D** | Agregar `CREATE INDEX IF NOT EXISTS idx_plba_premium_by_location_id ON premium_location_blocking_alerts(premium_by_location_id)` en migración V9 o en una V10 correctiva. |
| RISK-013 | Datos | `location.guarantees()` puede ser `null` (no lista vacía) si el mapper de persistencia `LocationPersistenceMapper` retorna `null` en lugar de `List.of()` para ubicaciones sin garantías; `hasTarifableGuarantee` tiene el guard `if (location.guarantees() == null) return false`, pero `getBlockingAlerts` llama a `hasTarifableGuarantee` que sí tiene el guard; sin embargo, si en un futuro refactor se elimina ese guard, el riesgo se activa | NPE en `hasTarifableGuarantee` propagada como HTTP 500 | Baja | **D** | Hacer que `Location` (domain record) rechace null en `guarantees` desde la construcción (validar en el mapper que nunca sea null). Agregar test de contrato en `LocationPersistenceMapper`. |
| RISK-014 | Datos | El campo `version` en `CalculatePremiumRequest` es de tipo `Long @NotNull`; no existe validación de rango mínimo; un cliente puede enviar `version: -1` o `version: Long.MAX_VALUE`; la comparación `snapshot.version() != command.version()` funcionará correctamente, pero `Long.MAX_VALUE` en un escenario de overflow de `@Version` de Hibernate podría producir comportamiento inesperado | Conflicto de versión falso positivo si la `@Version` de Hibernate llega a un valor muy alto (extremadamente improbable en producción, pero posible en entornos de testing con recálculos masivos) | Baja | **D** | Agregar validación `@Min(0)` en `CalculatePremiumRequest.version`. Agregar test: `calculate_returns422_whenVersionIsNegative`. |
| RISK-015 | Cobertura de pruebas | Los tests `CalculationResultJpaAdapterTest` y `TariffClientAdapterTest` no existen en el código actual; la lógica de persistencia atómica (delete+save), la lectura cross-context del snapshot y el cliente HTTP al core no tienen cobertura de tests | Regresiones en la capa de infraestructura sin detección automática; el DoD de cobertura >= 80% no se cumple para estas clases | Alta | **A** | Crear `CalculationResultJpaAdapterTest` con `@DataJpaTest` + Testcontainers cubriendo los 6 escenarios definidos en la spec (sección lista de tareas). Crear `TariffClientAdapterTest` con WireMock cubriendo los 3 escenarios. Estos tests son bloqueantes para producción. |

---

## Riesgos A (obligatorio mitigar antes de producción)

### RISK-001 — Redondeo prematuro en componentes intermedios

**Hallazgo en código real.** En `CalculationService.java`, cada método privado (`calculateFireBuildings`, `calculateFireContents`, etc.) aplica `setScale(RESULT_SCALE, ROUNDING)` donde `RESULT_SCALE = 2`. Esto contradice la nota de implementación de la spec:

> "Escala intermedia: durante el cálculo intermedio mantener escala 8 para acumular precisión; redondear a 2 decimales solo en el resultado final de cada componente."

La suma de 14 valores ya redondeados a 2 decimales introduce un error acumulado de hasta 14 × 0.005 = 0.07 unidades en `netPremium`. Para primas de seguros, cualquier diferencia con respecto a lo calculado manualmente por el actuario puede tener consecuencias regulatorias.

**Mitigación requerida:**
1. Cambiar `INTERMEDIATE_SCALE = 8` y aplicarlo en los métodos privados.
2. Aplicar `setScale(RESULT_SCALE, ROUNDING)` solo en `calculateLocation`, al asignar cada componente al `CoverageBreakdown`.
3. Agregar test parametrizado con valores que exhiban la diferencia de resultado al cambiar la escala intermedia.

---

### RISK-003 — NullPointerException en `insuredValue` nulo de Guarantee

**Hallazgo en código real.** `sumInsuredValueByCode` en `CalculationService`:

```java
return location.guarantees().stream()
    .filter(g -> code.equals(g.code()))
    .map(Guarantee::insuredValue)          // puede retornar null
    .reduce(BigDecimal.ZERO, BigDecimal::add); // NPE en add(null)
```

No existe null-check sobre `Guarantee.insuredValue()`. El origen del riesgo es el mapper `LocationPersistenceMapper::toDomain` que reconstruye `Guarantee` desde la entidad JPA, donde la columna `insured_value` puede ser null si fue guardada así.

**Mitigación requerida:**
1. En `sumInsuredValueByCode`: `.filter(g -> g.insuredValue() != null)` antes del map.
2. Alternativamente, en el constructor/factory de `Guarantee` en domain: rechazar `insuredValue = null`.
3. Test: `sumInsuredValueByCode_treatsNullInsuredValueAsZero`.

---

### RISK-005 — Sin timeout en RestClient del core service

**Hallazgo en código real.** En `CalculationConfig.java`:

```java
RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
```

No se configura `HttpClient` subyacente con timeout. Un `GET /v1/tariffs` que no responde mantiene el hilo de Tomcat bloqueado indefinidamente.

**Mitigación requerida:**
1. Construir el `RestClient` con un `HttpComponentsClientHttpRequestFactory` o `JdkClientHttpRequestFactory` que tenga `connectTimeout(2, SECONDS)` y `readTimeout(5, SECONDS)`.
2. Crear `TariffClientAdapterTest` con WireMock que simule un delay > 5s y verifique que se lanza `CoreServiceException`.

---

### RISK-006 — Campos nulos en Tariff producen NPE silenciosa

**Hallazgo en código real.** `TariffClientAdapter` construye el record `Tariff` directamente desde `TariffData` sin validar que ningún campo sea null. Si el core retorna `{"tariffs": {"fireRate": null, ...}}`, Java deserializa `null` en el `BigDecimal fireRate` del record. La primera multiplicación `sumInsuredValueByCode(...).multiply(tariff.fireRate())` lanza NPE que se convierte en HTTP 500.

**Mitigación requerida:**
1. Agregar validación inmediatamente después de construir `Tariff` en el adapter:
   ```java
   Objects.requireNonNull(data.fireRate(), "fireRate must not be null in tariff response");
   // ... para cada campo crítico
   ```
2. O bien, usar un factory method en el record `Tariff` que valide todos los campos.
3. Test: `fetchTariffs_throwsCoreServiceException_whenAnyRateIsNull`.

---

### RISK-008 — Race condition en recálculo concurrente

**Análisis.** El flujo de `persist` ejecuta: `save(quoteJpa)` → `deleteByQuoteId` → `save(calculationResult)`. Si dos threads ejecutan este flujo concurrentemente para el mismo folio:

- Thread A y B leen el mismo `quoteJpa` con `version = 7`.
- Thread A hace `save(quoteJpa)` exitosamente; Hibernate incrementa `version` a 8.
- Thread B intenta `save(quoteJpa)` con `version = 7` → `OptimisticLockException`.

El `@Version` de Hibernate actúa como barrera correcta. Sin embargo, si `GlobalExceptionHandler` no mapea `OptimisticLockException` (o `ObjectOptimisticLockingFailureException` de Spring) a HTTP 409, el cliente recibirá HTTP 500.

**Mitigación requerida:**
1. Verificar que `GlobalExceptionHandler` incluye handler para `ObjectOptimisticLockingFailureException` → HTTP 409 `VERSION_CONFLICT`.
2. Agregar test de integración con `CompletableFuture` y dos threads concurrentes sobre el mismo folio.

---

### RISK-010 — `@Modifying` sin `clearAutomatically` puede dejar caché sucia

**Hallazgo en código real.** `CalculationResultJpaRepository`:

```java
@Modifying
@Query("DELETE FROM CalculationResultJpa c WHERE c.quote.id = :quoteId")
void deleteByQuoteId(@Param("quoteId") Long quoteId);
```

Sin `clearAutomatically = true`, Hibernate no expulsa las entidades del primer nivel de caché después del bulk DELETE. Si Hibernate tiene en caché una `CalculationResultJpa` del folio (por ejemplo, de la lectura en `getSnapshot`), puede re-insertarla al hacer flush al final de la transacción, violando el UNIQUE en `quote_id`.

**Mitigación requerida:**
1. Cambiar a `@Modifying(clearAutomatically = true)`.
2. Crear `CalculationResultJpaAdapterTest` con `@DataJpaTest` que ejecute el ciclo delete+insert dos veces (recálculo) y verifique que no hay excepción de constraint.

---

### RISK-015 — Tests de infraestructura ausentes

**Hallazgo.** Los archivos `CalculationResultJpaAdapterTest` y `TariffClientAdapterTest` no existen. La spec lista explícitamente 9 casos de prueba para el adaptador JPA y 3 para el cliente HTTP. Sin ellos:

- La lógica de `persist` (delete + insert en transacción) no tiene cobertura.
- El comportamiento ante errores del core (timeout, 5xx) no está verificado.
- El DoD de cobertura >= 80% no se cumple para las clases de infraestructura.

**Mitigación requerida:**
1. Crear `CalculationResultJpaAdapterTest` con `@DataJpaTest` + Testcontainers PostgreSQL.
2. Crear `TariffClientAdapterTest` con `@WireMockTest` cubriendo: success, 5xx del core, timeout.
3. Ambos tests deben estar marcados `@Tag("integration")` y ejecutarse en el pipeline de CI.

---

## Conclusión

Los riesgos más críticos antes de llevar SPEC-007 a producción son tres: la ausencia total de tests de infraestructura (RISK-015) que deja sin cobertura la transacción de persistencia y el cliente HTTP; el redondeo prematuro en componentes intermedios (RISK-001) que puede generar primas con error acumulado inconsistente con el cálculo actuarial; y la falta de timeout en el `RestClient` del core (RISK-005) que expone al servicio a un bloqueo total bajo alta concurrencia. Se recomienda bloquear el merge a `develop` hasta resolver RISK-001, RISK-003, RISK-005, RISK-006, RISK-010 y RISK-015, y establecer los tests de RISK-015 como quality gate en el pipeline antes del primer despliegue en staging.

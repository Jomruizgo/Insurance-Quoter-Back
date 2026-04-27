---
id: SPEC-008
status: IMPLEMENTED
feature: observability
created: 2026-04-27
updated: 2026-04-27
author: spec-generator
version: "1.0"
related-specs: []
---

# Spec: Observabilidad Completa — Logs, Métricas y Trazas

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción

Agregar los tres pilares de observabilidad al microservicio `Insurance-Quoter-Back`: logs estructurados con correlación por `traceId`/`spanId`, métricas de JVM + HTTP + negocio (cotizaciones, cálculo de prima, aceptaciones) exportadas a Prometheus, y trazas distribuidas HTTP+DB+use cases exportadas a Jaeger vía OTLP. El stack de visualización local queda levantado mediante Docker Compose (Jaeger + Prometheus + Grafana).

Esta funcionalidad es puramente de infraestructura: no modifica el dominio, no crea tablas nuevas y no expone endpoints de negocio nuevos.

### Requerimiento de Negocio

> Añadir observabilidad completa al backend para poder diagnosticar problemas en producción,
> monitorear el comportamiento en tiempo real y entender cómo se usan las funcionalidades de
> negocio (cotizaciones por tipo de negocio, errores de cálculo, latencias por endpoint).
> Debe ser posible correlacionar un log con la traza completa de la request que lo generó.

### Historias de Usuario

#### HU-01: Correlación de logs con trazas

```
Como:        desarrollador diagnosticando un error en producción
Quiero:      ver el traceId y spanId en cada línea de log
Para:        buscar todos los logs de una request fallida específica y reconstruir su historia completa

Prioridad:   Alta
Estimación:  XS
Dependencias: Ninguna
Capa:        Backend
```

#### Criterios de Aceptación — HU-01

**Happy Path**
```gherkin
CRITERIO-1.1: traceId aparece en cada log de una request
  Dado que:  el servicio está levantado con observabilidad habilitada
  Cuando:    llega una request POST /v1/quotes
  Entonces:  cada línea de log generada durante esa request contiene un campo traceId no vacío
             Y cada línea de log contiene un campo spanId no vacío
             Y el traceId es el mismo en todos los logs de esa request
```

**Edge Case**
```gherkin
CRITERIO-1.2: logs sin request activa no rompen el formato
  Dado que:  el servicio arranca y ejecuta tareas de inicialización (Flyway, Spring context)
  Cuando:    se imprime un log fuera del contexto de una request HTTP
  Entonces:  el log se imprime sin traceId (campo vacío o ausente) sin lanzar excepción
```

---

#### HU-02: Métricas de infraestructura y negocio

```
Como:        operador monitoreando el servicio en producción
Quiero:      ver dashboards con métricas de JVM, HTTP, DB pool y negocio en Grafana
Para:        detectar cuellos de botella, degradaciones y anomalías antes de que afecten usuarios

Prioridad:   Alta
Estimación:  S
Dependencias: HU-01
Capa:        Backend
```

#### Criterios de Aceptación — HU-02

**Happy Path**
```gherkin
CRITERIO-2.1: endpoint de métricas Prometheus disponible
  Dado que:  el servicio está levantado
  Cuando:    se hace GET /actuator/prometheus
  Entonces:  la respuesta es HTTP 200
             Y el Content-Type es text/plain
             Y el cuerpo contiene métricas jvm_memory_used_bytes
             Y el cuerpo contiene métricas http_server_requests_seconds
             Y el cuerpo contiene métricas hikaricp_connections_active
```

```gherkin
CRITERIO-2.2: métricas de negocio se registran al crear cotización
  Dado que:  el servicio está levantado con métricas habilitadas
  Cuando:    se ejecuta CreateFolioUseCaseImpl.createFolio() exitosamente
  Entonces:  el counter quotes_created_total se incrementa en 1
             Y el counter contiene el tag businessType con el valor de la cotización
```

```gherkin
CRITERIO-2.3: métricas de cálculo se registran al calcular prima
  Dado que:  el servicio está levantado con métricas habilitadas
  Cuando:    se ejecuta CalculatePremiumUseCaseImpl.calculatePremium() exitosamente
  Entonces:  el counter premium_calculated_total se incrementa en 1
             Y el timer premium_calculate_duration_seconds registra la duración
             Y ambos contienen el tag riskClassification con el valor calculado
```

```gherkin
CRITERIO-2.4: errores de cálculo se cuentan por tipo
  Dado que:  el servicio está levantado con métricas habilitadas
  Cuando:    CalculatePremiumUseCaseImpl lanza NoCalculableLocationsException
  Entonces:  el counter calculation_errors_total se incrementa
             Y el counter contiene el tag errorType = "NoCalculableLocations"
```

---

#### HU-03: Trazas distribuidas HTTP + DB + use cases

```
Como:        desarrollador investigando por qué un endpoint es lento
Quiero:      ver en Jaeger el waterfall completo: HTTP request → use case → queries SQL
Para:        identificar exactamente qué operación consume el tiempo y optimizarla

Prioridad:   Alta
Estimación:  S
Dependencias: HU-01
Capa:        Backend
```

#### Criterios de Aceptación — HU-03

**Happy Path**
```gherkin
CRITERIO-3.1: traza completa de request POST /v1/quotes visible en Jaeger
  Dado que:  el servicio está levantado con tracing habilitado y conectado a Jaeger
  Cuando:    se ejecuta POST /v1/quotes exitosamente
  Entonces:  en Jaeger aparece una traza del servicio "insurance-quoter-back"
             Y la traza tiene un span raíz de tipo http.server.request con ruta /v1/quotes
             Y la traza tiene spans hijos para CreateFolioUseCaseImpl.createFolio
             Y la traza tiene spans hijos para cada query SQL ejecutada
             Y la duración total coincide (±5%) con el tiempo de respuesta HTTP
```

```gherkin
CRITERIO-3.2: traza propagada a llamada HTTP a Insurance-Quoter-Core
  Dado que:  el servicio está configurado con context propagation
  Cuando:    CreateFolioUseCaseImpl llama a CoreServiceClient
  Entonces:  el header traceparent se propaga en la request saliente a Core
             Y el span de la llamada HTTP saliente es hijo del span del use case
```

```gherkin
CRITERIO-3.3: span de use case registrado con @Observed
  Dado que:  ObservedAspect está activo en el contexto Spring
  Cuando:    se invoca cualquier use case anotado con @Observed
  Entonces:  aparece un span hijo con el nombre definido en la anotación
             Y el span incluye el tag del bounded context correspondiente
```

---

#### HU-04: Stack de observabilidad local en Docker Compose

```
Como:        desarrollador ejecutando el proyecto localmente
Quiero:      levantar Jaeger, Prometheus y Grafana con un solo docker compose up
Para:        explorar trazas y métricas sin infraestructura externa

Prioridad:   Media
Estimación:  S
Dependencias: HU-02, HU-03
Capa:        Backend
```

#### Criterios de Aceptación — HU-04

**Happy Path**
```gherkin
CRITERIO-4.1: servicios de observabilidad accesibles tras compose up
  Dado que:  se ejecuta docker compose up en Insurance-Quoter-Back/
  Cuando:    los contenedores terminan de inicializarse
  Entonces:  Jaeger UI responde en http://localhost:16686
             Y Prometheus responde en http://localhost:9090
             Y Grafana responde en http://localhost:3000
             Y Prometheus tiene el target "insurance-quoter-back" en estado UP
```

---

### Reglas de Negocio

1. El endpoint `/actuator/prometheus` es público en entorno local (no requiere auth). En producción debe securizarse — fuera del alcance de esta spec.
2. Los tags de métricas de negocio usan **cardinalidad baja**: `businessType` y `riskClassification` son enumeraciones finitas. Prohibido usar `folioNumber` o `quoteId` como tag (causa explosión de cardinalidad).
3. El sampling de trazas es `1.0` (100%) en entornos `local` y `test`. En `prod` se debe bajar a `0.1` vía variable de entorno `TRACING_SAMPLING_PROBABILITY`.
4. El `ObservabilityConfig` vive en `infrastructure/config/` — es el único lugar donde se referencian `ObservedAspect`, `MeterRegistry` y `ObservationRegistry`.
5. Las anotaciones `@Observed` van en los métodos públicos de los use case implementations (`application/usecase/`). El dominio no puede importar dependencias de Micrometer.

---

## 2. DISEÑO

### Modelos de Datos

No se requieren cambios de base de datos. Observabilidad es puramente infraestructura.

### Métricas de Negocio Definidas

| Nombre de métrica | Tipo | Tags | Dónde se registra |
|------------------|------|------|--------------------|
| `quotes.created` | Counter | `businessType` | `CreateFolioUseCaseImpl` |
| `premium.calculated` | Counter | `riskClassification`, `businessType` | `CalculatePremiumUseCaseImpl` |
| `premium.calculate.duration` | Timer | `riskClassification` | `CalculatePremiumUseCaseImpl` |
| `quote.accepted` | Counter | — | `AcceptQuoteUseCaseImpl` |
| `calculation.errors` | Counter | `errorType` | `CalculatePremiumUseCaseImpl` (catch) |
| `coverage.options.saved` | Counter | — | `SaveCoverageOptionsUseCaseImpl` |

### Use Cases instrumentados con `@Observed`

| Use Case | Nombre de observación | Bounded Context |
|----------|----------------------|-----------------|
| `CreateFolioUseCaseImpl.createFolio()` | `folio.create` | folio |
| `GetQuoteStateUseCaseImpl.getQuoteState()` | `folio.state.get` | folio |
| `ListFoliosUseCaseImpl.listFolios()` | `folio.list` | folio |
| `CalculatePremiumUseCaseImpl.calculatePremium()` | `premium.calculate` | calculation |
| `AcceptQuoteUseCaseImpl.acceptQuote()` | `quote.accept` | calculation |
| `GetCalculationResultUseCaseImpl.getCalculationResult()` | `calculation.result.get` | calculation |
| `SaveCoverageOptionsUseCaseImpl.saveCoverageOptions()` | `coverage.options.save` | coverage |
| `GetCoverageOptionsUseCaseImpl.getCoverageOptions()` | `coverage.options.get` | coverage |

### API Endpoints (Actuator)

No son endpoints de negocio. Se exponen vía Spring Boot Actuator:

#### GET /actuator/health
- Devuelve estado de salud del servicio y sus dependencias (DB)
- **Response 200**: `{ "status": "UP", "components": { "db": { "status": "UP" } } }`

#### GET /actuator/prometheus
- Devuelve métricas en formato Prometheus text exposition format
- **Response 200**: texto plano con todas las métricas registradas

#### GET /actuator/metrics
- Lista nombres de métricas disponibles
- **Response 200**: JSON con array de metric names

### Dependencias Nuevas (build.gradle.kts)

```kotlin
// Actuator + management endpoints
implementation("org.springframework.boot:spring-boot-starter-actuator")

// Micrometer Tracing — abstracción sobre OTel
implementation("io.micrometer:micrometer-tracing-bridge-otel")

// OpenTelemetry OTLP exporter — envía trazas a Jaeger
implementation("io.opentelemetry:opentelemetry-exporter-otlp")

// Prometheus registry — exporta métricas al formato Prometheus
implementation("io.micrometer:micrometer-registry-prometheus")
```

Las versiones las gestiona el BOM de Spring Boot 4 — no se pinean manualmente.

### Configuración application.properties (adiciones)

```properties
# Nombre del servicio (aparece en Jaeger y métricas)
spring.application.name=insurance-quoter-back

# Actuator: exponer health, prometheus, metrics
management.endpoints.web.exposure.include=health,prometheus,metrics
management.endpoint.health.show-details=always

# Tracing: 100% sampling en local (override vía env var en prod)
management.tracing.sampling.probability=${TRACING_SAMPLING_PROBABILITY:1.0}

# OTLP: enviar trazas a Jaeger local (override vía env var)
management.otlp.tracing.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4317}

# Logging: incluir traceId y spanId en cada línea de log
logging.pattern.level=%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]
```

### Nuevos Archivos de Infraestructura

#### `infrastructure/config/ObservabilityConfig.java`

Responsabilidades:
- Registrar el bean `ObservedAspect` (habilita `@Observed` en AOP)
- Definir los `MeterBinder` beans para métricas de negocio custom (opcional, si no se inyectan directamente en los use cases)

```java
// Ejemplo de contenido
@Configuration
public class ObservabilityConfig {

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry registry) {
        return new ObservedAspect(registry);
    }
}
```

#### `compose.yaml` (adiciones)

```yaml
  jaeger:
    image: jaegertracing/all-in-one:1.57
    ports:
      - "16686:16686"   # UI
      - "4317:4317"     # OTLP gRPC
      - "4318:4318"     # OTLP HTTP
    environment:
      - COLLECTOR_OTLP_ENABLED=true

  prometheus:
    image: prom/prometheus:v2.51.0
    ports:
      - "9090:9090"
    volumes:
      - ./observability/prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana:10.4.2
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - ./observability/grafana/datasources:/etc/grafana/provisioning/datasources
      - ./observability/grafana/dashboards:/etc/grafana/provisioning/dashboards
```

#### `observability/prometheus.yml`

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: insurance-quoter-back
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['host.docker.internal:8080']
```

#### `observability/grafana/datasources/datasource.yml`

```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    url: http://prometheus:9090
    isDefault: true
  - name: Jaeger
    type: jaeger
    url: http://jaeger:16686
```

### Arquitectura y Dependencias

- Sin cambios al dominio ni a la capa de aplicación más allá de las anotaciones `@Observed`
- `ObservabilityConfig` es el único punto de acoplamiento a Micrometer en `infrastructure/config/`
- Inyección de `MeterRegistry` en use cases via constructor (cumple regla de no `@Autowired`)
- `ObservedAspect` intercepta métodos `@Observed` en el proxy Spring — los use cases deben estar como beans Spring (ya lo están con `@Service`)

### Notas de Implementación

> - `micrometer-tracing-bridge-otel` actúa como puente: el código de la app usa la API de Micrometer Observation; el bridge traduce a OpenTelemetry SDK en runtime. Esto evita acoplamiento directo al SDK de OTel en código de aplicación.
> - Spring Boot 4 autoconfigura `ObservationRegistry` y el bridge OTel cuando ambas dependencias están en classpath. Solo se necesita el bean `ObservedAspect` manual.
> - El tag `businessType` en métricas proviene del campo `Quote.businessType` del dominio. El use case ya lo tiene disponible en el command.
> - Para el timer `premium.calculate.duration`, usar `Observation.createNotStarted()` o inyectar `Timer.Sample` — más preciso que `@Observed` solo (que registra duración pero no la expone como Timer separado).
> - Jaeger `all-in-one` solo para desarrollo local. No usar en producción — usar Jaeger distribuido o Grafana Tempo.

---

## 3. LISTA DE TAREAS

> Checklist accionable para el agente backend. Marcar cada ítem (`[x]`) al completarlo.

### Backend

#### Dependencias y configuración base
- [ ] Agregar 4 dependencias a `build.gradle.kts` (actuator, micrometer-tracing-bridge-otel, opentelemetry-exporter-otlp, micrometer-registry-prometheus)
- [ ] Agregar bloque de observabilidad a `application.properties` (5 properties: spring.application.name, management.endpoints, tracing.sampling, otlp.endpoint, logging.pattern.level)
- [ ] Crear `infrastructure/config/ObservabilityConfig.java` con bean `ObservedAspect`

#### Pilar 1 — Logs correlacionados
- [ ] Verificar que `logging.pattern.level` incluye `%X{traceId:-}` y `%X{spanId:-}` en `application.properties`
- [ ] Crear `logback-spring.xml` en `src/main/resources/` con patrón JSON estructurado que incluya traceId/spanId

#### Pilar 2 — Métricas
- [ ] Inyectar `MeterRegistry` en `CreateFolioUseCaseImpl` y registrar counter `quotes.created` con tag `businessType`
- [ ] Inyectar `MeterRegistry` en `CalculatePremiumUseCaseImpl` y registrar counter `premium.calculated` + timer `premium.calculate.duration` con tags `riskClassification` y `businessType`
- [ ] Registrar counter `calculation.errors` en bloque catch de `CalculatePremiumUseCaseImpl` con tag `errorType`
- [ ] Inyectar `MeterRegistry` en `AcceptQuoteUseCaseImpl` y registrar counter `quote.accepted`
- [ ] Inyectar `MeterRegistry` en `SaveCoverageOptionsUseCaseImpl` y registrar counter `coverage.options.saved`

#### Pilar 3 — Trazas distribuidas
- [ ] Agregar `@Observed(name = "folio.create")` a `CreateFolioUseCaseImpl.createFolio()`
- [ ] Agregar `@Observed(name = "folio.state.get")` a `GetQuoteStateUseCaseImpl.getQuoteState()`
- [ ] Agregar `@Observed(name = "folio.list")` a `ListFoliosUseCaseImpl.listFolios()`
- [ ] Agregar `@Observed(name = "premium.calculate")` a `CalculatePremiumUseCaseImpl.calculatePremium()`
- [ ] Agregar `@Observed(name = "quote.accept")` a `AcceptQuoteUseCaseImpl.acceptQuote()`
- [ ] Agregar `@Observed(name = "calculation.result.get")` a `GetCalculationResultUseCaseImpl.getCalculationResult()`
- [ ] Agregar `@Observed(name = "coverage.options.save")` a `SaveCoverageOptionsUseCaseImpl.saveCoverageOptions()`
- [ ] Agregar `@Observed(name = "coverage.options.get")` a `GetCoverageOptionsUseCaseImpl.getCoverageOptions()`

#### Docker Compose (stack de observabilidad local)
- [ ] Agregar servicios `jaeger`, `prometheus`, `grafana` a `compose.yaml`
- [ ] Crear `observability/prometheus.yml` con scrape config para `insurance-quoter-back`
- [ ] Crear `observability/grafana/datasources/datasource.yml` con datasources Prometheus y Jaeger

#### Tests Backend
- [ ] `ObservabilityConfigTest` — verifica que bean `ObservedAspect` existe en contexto Spring (`@SpringBootTest`)
- [ ] `QuoteMetricsTest` — verifica que `quotes.created` counter se incrementa al ejecutar `CreateFolioUseCaseImpl` (test unitario con `SimpleMeterRegistry`)
- [ ] `PremiumMetricsTest` — verifica que `premium.calculated` counter y `premium.calculate.duration` timer se registran al ejecutar `CalculatePremiumUseCaseImpl`
- [ ] `CalculationErrorMetricsTest` — verifica que `calculation.errors` counter se incrementa cuando se lanza `NoCalculableLocationsException`
- [ ] `ActuatorPrometheusIT` — integración: GET /actuator/prometheus retorna 200 y contiene `jvm_memory_used_bytes` (usa `@SpringBootTest` + Testcontainers)
- [ ] `TracingMdcIT` — integración: verifica que una request a `POST /v1/quotes` genera logs con traceId no vacío en MDC

### QA
- [ ] Ejecutar skill `/gherkin-case-generator` → criterios CRITERIO-1.1, 2.1, 2.2, 3.1
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD de riesgos (baja complejidad de negocio, media de infraestructura)
- [ ] Levantar stack con `docker compose up` y verificar manualmente:
  - Jaeger UI en `http://localhost:16686` muestra traces de `insurance-quoter-back`
  - Prometheus en `http://localhost:9090` tiene target UP
  - Grafana en `http://localhost:3000` muestra datasources conectados
- [ ] Verificar que métricas custom aparecen en `/actuator/prometheus` tras ejecutar operaciones de negocio
- [ ] Actualizar estado spec: `status: IMPLEMENTED`

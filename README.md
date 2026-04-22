# Insurance-Quoter-Back — plataforma-danos-back

Backend del cotizador de seguros de daños. Microservicio responsable de la gestión de cotizaciones, ubicaciones, coberturas y cálculo de primas.

## Stack

| Tecnología | Versión |
|-----------|---------|
| Java | 21 |
| Spring Boot | 4.0.5 |
| Spring Data JPA + Hibernate | 7.x |
| PostgreSQL | 16 |
| Flyway | 11.x |
| Lombok | — |
| Springdoc OpenAPI | 2.8.x |

Arquitectura: **Hexagonal (Ports & Adapters)** con bounded contexts `folio`, `location`, `coverage`, `calculation`.

## Requisitos previos

- Java 21+
- Docker (para PostgreSQL vía Spring Boot Docker Compose)
- Variables de entorno: `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`

El servicio también consume el **core service** (`plataforma-core-ohs`) en `http://localhost:8081`. Debe estar levantado para que los endpoints de folios y cálculo funcionen.

## Ejecutar en local

```bash
# Desde Insurance-Quoter-Back/
DB_NAME=insurance_quoter_db DB_USERNAME=quoter DB_PASSWORD=quoter123 \
  ./gradlew bootRun
```

Spring Boot Docker Compose levanta automáticamente el contenedor de PostgreSQL al arrancar y lo detiene al parar.

El backend queda disponible en `http://localhost:8080`.

## Ejecutar tests

```bash
./gradlew test
```

Los tests de integración usan Testcontainers (levanta PostgreSQL efímero). WireMock stubea el core service en tests de HTTP client.

## API

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Endpoints disponibles

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/v1/folios` | Crear o recuperar folio de cotización |
| `GET` | `/v1/quotes/{folio}/state` | Estado y datos de la cotización |
| `GET` | `/v1/quotes/{folio}/locations` | Listar ubicaciones |
| `PUT` | `/v1/quotes/{folio}/locations` | Reemplazar lista de ubicaciones |
| `PATCH` | `/v1/quotes/{folio}/locations/{index}` | Actualizar campos de una ubicación |
| `GET` | `/v1/quotes/{folio}/locations/summary` | Resumen de completitud de ubicaciones |
| `GET` | `/v1/quotes/{folio}/locations/layout` | Configuración de layout |
| `PUT` | `/v1/quotes/{folio}/locations/layout` | Actualizar configuración de layout |
| `GET` | `/v1/quotes/{folio}/coverage-options` | Obtener opciones de cobertura |
| `PUT` | `/v1/quotes/{folio}/coverage-options` | Reemplazar opciones de cobertura |
| `POST` | `/v1/quotes/{folio}/calculate` | Calcular prima neta y comercial |

### Cálculo de prima (`POST /v1/quotes/{folio}/calculate`)

Requiere body `{"version": <long>}` para control de concurrencia optimista.

Devuelve el desglose de 14 componentes por ubicación:

| Componente | Código garantía |
|-----------|----------------|
| Incendio edificios | `GUA-FIRE` |
| Contenidos incendio | `GUA-FIRE-CONT` |
| Extensión de cobertura | — (derivado) |
| CAT-TEV | — (derivado) |
| CAT-FHM | — (derivado) |
| Remoción de escombros | — (derivado) |
| Gastos extraordinarios | — (derivado) |
| Pérdida de rentas | `GUA-RENTAL` |
| Interrupción de negocio | `GUA-BI` |
| Equipo electrónico | `GUA-ELEC` |
| Robo | `GUA-THEFT` |
| Dinero y valores | `GUA-CASH` |
| Vidrios | `GUA-GLASS` |
| Anuncios luminosos | `GUA-SIGN` |

## Base de datos

PostgreSQL en puerto `5432`, base de datos `insurance_quoter_db`.

Migraciones con Flyway (classpath `db/migration`):

| Versión | Descripción |
|---------|------------|
| V1 | Tabla `quotes` |
| V2 | Constraint folio único activo |
| V3 | Columnas de layout en quotes |
| V4 | Tabla `locations` |
| V5 | Columnas de detalle en locations |
| V6 | Tabla `location_blocking_alerts` |
| V7 | Tabla `coverage_options` |
| V8 | Tabla `calculation_results` |
| V9 | Tablas `premiums_by_location` y `premium_location_blocking_alerts` |

## Estructura de paquetes

```
com.sofka.insurancequoter.back/
├── folio/          ← Generación y consulta de folios
├── location/       ← Gestión de ubicaciones y layout
├── coverage/       ← Opciones de cobertura y catálogo de garantías
└── calculation/    ← Cálculo de prima neta y comercial
```

Cada bounded context sigue la estructura hexagonal:

```
<context>/
├── domain/
│   ├── model/          ← Entities y Value Objects (POJOs, sin Spring/JPA)
│   ├── service/        ← Domain Services (lógica pura)
│   └── port/
│       ├── in/         ← Input Ports (interfaces de casos de uso)
│       └── out/        ← Output Ports (repositorios y clients externos)
├── application/
│   └── usecase/        ← Implementaciones de casos de uso
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/    ← Spring MVC Controllers + Swagger interfaces
    │   └── out/
    │       ├── persistence/  ← JPA Adapters + Entities
    │       └── http/         ← HTTP Client Adapters (RestClient)
    └── config/         ← Wiring de beans y configuración
```

## Variables de entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | Host de PostgreSQL |
| `DB_PORT` | `5432` | Puerto de PostgreSQL |
| `DB_NAME` | — | Nombre de la base de datos |
| `DB_USERNAME` | — | Usuario de PostgreSQL |
| `DB_PASSWORD` | — | Contraseña de PostgreSQL |
| `core.service.base-url` | `http://localhost:8081` | URL base del core service |
| `core.service.connect-timeout-ms` | `5000` | Timeout de conexión al core (ms) |
| `core.service.read-timeout-ms` | `10000` | Timeout de lectura al core (ms) |

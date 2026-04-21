---
id: SPEC-001
status: IMPLEMENTED
feature: folio-generator
created: 2026-04-21
updated: 2026-04-21
approved: 2026-04-21
author: spec-generator
version: "1.0"
related-specs: []
---

# Spec: Generación de Números de Folio Secuenciales

> **Estado:** `DRAFT` → aprobar con `status: APPROVED` antes de iniciar implementación.
> **Ciclo de vida:** DRAFT → APPROVED → IN_PROGRESS → IMPLEMENTED → DEPRECATED

---

## 1. REQUERIMIENTOS

### Descripción

El microservicio `Insurance-Quoter-Core` expone un endpoint `GET /v1/folios` que genera y retorna un número de folio único, secuencial y globalmente consistente bajo concurrencia. El folio sigue el patrón `FOL-<año>-<secuencia 5 dígitos>` y utiliza una secuencia nativa de PostgreSQL para garantizar unicidad sin condiciones de carrera.

### Requerimiento de Negocio

> Generación de números de folio secuenciales en el microservicio core.
> Endpoint `GET /v1/folios`. La secuencia es global, se incrementa con cada llamada y debe ser
> consistente bajo concurrencia usando una PostgreSQL SEQUENCE. No se almacena el folio en tabla;
> solo se persiste la secuencia en la base de datos del motor.

### Historias de Usuario

#### HU-01: Generar número de folio secuencial

```
Como:        sistema cliente (Insurance-Quoter-Back u otro consumidor)
Quiero:      llamar GET /v1/folios y recibir un número de folio único y secuencial
Para:        asociarlo a una nueva cotización garantizando unicidad global y orden temporal

Prioridad:   Alta
Estimación:  S
Dependencias: Ninguna
Capa:        Backend
```

#### Criterios de Aceptación — HU-01

**Happy Path**
```gherkin
CRITERIO-1.1: Generación exitosa de folio
  Dado que:  el servicio Insurance-Quoter-Core está disponible y la secuencia folio_sequence existe en la DB
  Cuando:    se realiza GET /v1/folios
  Entonces:  la respuesta tiene HTTP 200
             Y el cuerpo contiene "folioNumber" con formato "FOL-<año_actual>-<secuencia_5_dígitos>"
             Y el cuerpo contiene "generatedAt" con un timestamp ISO-8601 en UTC del momento de generación
             Y el valor de secuencia es mayor en 1 al folio generado en la llamada anterior
```

**Consistencia bajo concurrencia**
```gherkin
CRITERIO-1.2: Unicidad garantizada bajo llamadas concurrentes
  Dado que:  se realizan N llamadas concurrentes a GET /v1/folios
  Cuando:    todas las solicitudes se procesan simultáneamente
  Entonces:  cada respuesta contiene un "folioNumber" distinto
             Y no existen números de secuencia duplicados en el conjunto de respuestas
             Y todos los folios pertenecen al año de la solicitud
```

**Formato del folio**
```gherkin
CRITERIO-1.3: Validación del patrón FOL-<año>-<5 dígitos con padding>
  Dado que:  el siguiente valor de la secuencia es 43
  Cuando:    se genera el folio en el año 2026
  Entonces:  "folioNumber" es exactamente "FOL-2026-00043"
             Y la longitud total de la cadena es 14 caracteres
```

**Rollover de año**
```gherkin
CRITERIO-1.4: El año en el folio refleja el año UTC del momento de generación
  Dado que:  se genera un folio el 31-dic a las 23:59 UTC y otro el 01-ene al 00:01 UTC
  Cuando:    se comparan los dos folios
  Entonces:  el segundo folio contiene el año nuevo
             Y la secuencia continúa incrementándose (no se reinicia por año)
```

### Reglas de Negocio

1. **Patrón de folio:** `FOL-<YYYY>-<NNNNN>` donde `YYYY` es el año UTC actual y `NNNNN` es la secuencia global con padding cero a 5 dígitos (máximo representable: 99 999 por año sin overflow de dígitos). **Supuesto de negocio:** el volumen anual de cotizaciones no superará 99 999. Si en el futuro el negocio requiere mayor capacidad, se tratará como una nueva feature que amplíe el patrón — no como corrección de esta spec.
2. **Secuencia global:** la secuencia no se reinicia por año, por suscriptor ni por ningún otro criterio. Es una única secuencia creciente en toda la vida del sistema.
3. **Concurrencia:** la unicidad está garantizada por el motor de PostgreSQL mediante `nextval('folio_sequence')` — no se usan locks de aplicación ni UUIDs.
4. **Sin almacenamiento del folio:** el microservicio core no persiste el folio en ninguna tabla propia. Solo llama `nextval`. La asociación cotización–folio ocurre en `Insurance-Quoter-Back`.
5. **Timestamp UTC:** `generatedAt` se genera en la capa de aplicación con `Instant.now()` justo después de obtener el número de secuencia.

---

## 2. DISEÑO

### Modelos de Datos

#### Entidades afectadas

| Entidad / Objeto | Almacén | Cambios | Descripción |
|-----------------|---------|---------|-------------|
| `folio_sequence` | PostgreSQL SEQUENCE en `insurance_core_db` | nueva | Secuencia nativa que garantiza `nextval` atómico |
| `Folio` (domain model) | — (no persiste) | nueva | Value Object con `folioNumber` y `generatedAt` |

#### Diseño de la SEQUENCE

```sql
-- migration: V1__create_folio_sequence.sql
CREATE SEQUENCE IF NOT EXISTS folio_sequence
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;
```

> **No se crea tabla `folios`.** Solo se crea la secuencia.

#### Campos del Value Object `Folio`

| Campo | Tipo Java | Descripción |
|-------|-----------|-------------|
| `folioNumber` | `String` | Folio formateado: `FOL-2026-00043` |
| `generatedAt` | `Instant` | Timestamp UTC de generación |

### API Endpoints

#### GET /v1/folios

- **Descripción:** Genera y retorna el siguiente número de folio disponible
- **Auth requerida:** no (servicio interno, llamado solo por otros microservicios de la plataforma)
- **Request Body:** ninguno
- **Response 200:**
  ```json
  {
    "folioNumber": "FOL-2026-00043",
    "generatedAt": "2026-04-20T14:30:00Z"
  }
  ```
- **Response 503:** fallo de conectividad con la base de datos (propagado por Spring)

> **Nota:** El método HTTP es `GET` según el contrato definido en `docs/api-contracts.md` sección 8, aunque semánticamente produce un efecto de lado (incremento de secuencia). Se respeta el contrato acordado.

### Arquitectura Hexagonal — Descomposición de Componentes

#### Contexto: `folio`

```
com.sofka.insurancequoter.core.folio/
├── domain/
│   ├── model/
│   │   └── Folio.java                                  ← Value Object (folioNumber, generatedAt)
│   └── port/
│       ├── in/
│       │   └── GenerateFolioUseCase.java                ← Input Port: Folio generate()
│       └── out/
│           └── FolioSequencePort.java                   ← Output Port: String nextFolioNumber()
├── application/
│   └── usecase/
│       └── GenerateFolioUseCaseImpl.java                ← Orquesta FolioSequencePort; genera Folio
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   └── rest/
    │   │       ├── FolioController.java                 ← GET /v1/folios → GenerateFolioUseCase
    │   │       ├── swaggerdocs/
    │   │       │   └── FolioApi.java                    ← @Tag, @Operation, @ApiResponse
    │   │       ├── dto/
    │   │       │   └── FolioResponse.java               ← { folioNumber, generatedAt }
    │   │       └── mapper/
    │   │           └── FolioRestMapper.java             ← Folio (domain) → FolioResponse (DTO)
    │   └── out/
    │       └── persistence/
    │           ├── adapter/
    │           │   └── FolioSequenceJpaAdapter.java     ← implementa FolioSequencePort; inyecta FolioRepository
    │           ├── repositories/
    │           │   └── FolioRepository.java             ← extiende JpaRepository<FolioSequenceEntity, Long>; método nextValue()
    │           ├── entities/
    │           │   └── FolioSequenceEntity.java         ← @Entity tabla folio_sequence_ctrl (1 fila dummy) para llamar nextval
    │           └── mappers/
    │               └── (vacío — no hay entidad de dominio que mapear desde persistence)
    └── config/
        └── FolioConfig.java                             ← @Bean wiring GenerateFolioUseCaseImpl + Clock
```

#### Contratos de interfaces clave

```java
// domain/port/in/GenerateFolioUseCase.java
public interface GenerateFolioUseCase {
    Folio generate();
}

// domain/port/out/FolioSequencePort.java
public interface FolioSequencePort {
    String nextFolioNumber();
}
```

#### Flujo de llamada

```
FolioController.getFolio()
  → GenerateFolioUseCase.generate()
    → FolioSequencePort.nextFolioNumber()          // llama nextval en DB
      ← "FOL-2026-00043"
    ← Folio("FOL-2026-00043", Instant.now())
  ← FolioResponse("FOL-2026-00043", "2026-04-20T14:30:00Z")
← HTTP 200
```

#### Decisión de diseño: dónde vive el formato del folio

La responsabilidad de formatear `FOL-<año>-<NNNNN>` reside en `FolioSequenceJpaAdapter` (infrastructure), que obtiene el valor crudo de `nextval` vía `FolioRepository` y aplica el patrón usando el año UTC del reloj del sistema. Esto es consistente con la firma `nextFolioNumber(): String` del output port. Si el patrón cambiara, se modifica solo el adapter sin tocar el dominio.

> **Alternativa considerada y descartada:** que el output port devuelva `Long` y el use case formatee. Requeriría que el use case conozca el patrón de formato (lógica de infraestructura), violando la separación de capas en este contexto concreto.

#### Decisión de diseño: FolioSequenceEntity como tabla auxiliar

Para mantener el patrón de 4 subcarpetas en persistence (adapter / repositories / entities / mappers) y usar `JpaRepository` de forma consistente, se introduce una tabla auxiliar de control `folio_sequence_ctrl` con una única fila. Esta tabla no almacena folios; su único propósito es anclar la entidad JPA que permite ejecutar el `@Query(nativeQuery = true)` con `SELECT nextval('folio_sequence')`.

```sql
-- migration: V1__create_folio_sequence.sql
CREATE SEQUENCE IF NOT EXISTS folio_sequence START WITH 1 INCREMENT BY 1 NO CYCLE;

CREATE TABLE IF NOT EXISTS folio_sequence_ctrl (id BIGINT PRIMARY KEY DEFAULT 1);
INSERT INTO folio_sequence_ctrl VALUES (1) ON CONFLICT DO NOTHING;
```

```java
// entities/FolioSequenceEntity.java
@Entity
@Table(name = "folio_sequence_ctrl")
public class FolioSequenceEntity {
    @Id
    private Long id;
}

// repositories/FolioRepository.java
public interface FolioRepository extends JpaRepository<FolioSequenceEntity, Long> {
    @Query(value = "SELECT nextval('folio_sequence')", nativeQuery = true)
    Long nextValue();
}

// adapter/FolioSequenceJpaAdapter.java  (implementa FolioSequencePort)
@Component
@RequiredArgsConstructor
public class FolioSequenceJpaAdapter implements FolioSequencePort {
    private final FolioRepository folioRepository;
    private final Clock clock;

    @Override
    public String nextFolioNumber() {
        long seq = folioRepository.nextValue();
        int year = LocalDate.now(clock).getYear();
        return String.format("FOL-%d-%05d", year, seq);
    }
}
```

#### Mapper REST: Folio → FolioResponse

```java
// rest/mapper/FolioRestMapper.java
@Component
public class FolioRestMapper {
    public FolioResponse toResponse(Folio folio) {
        return new FolioResponse(folio.folioNumber(), folio.generatedAt());
    }
}
```

### Notas de Implementación

- `FolioRepository.nextValue()` usa `@Query(nativeQuery = true)` sobre `FolioSequenceEntity` para llamar `nextval` sin lógica JDBC directa.
- `generatedAt` se genera con `Instant.now(Clock.systemUTC())` en `GenerateFolioUseCaseImpl` — el `Clock` se inyecta como `@Bean` para facilitar los tests.
- `FolioRestMapper` inyectado en `FolioController` para desacoplar el DTO del domain model.
- Las migraciones Flyway deben estar en `src/main/resources/db/migration/` con el prefijo `V1__`.
- La secuencia empieza en 1 y no tiene `MAXVALUE` ni `CYCLE` — es responsabilidad de operaciones monitorear si supera 99 999 (límite del padding de 5 dígitos).
- La carpeta `mappers/` en persistence queda vacía para este feature (no hay conversión entity↔domain ya que `FolioSequenceEntity` no mapea a un domain model).

---

## 3. LISTA DE TAREAS

> Checklist accionable para el agente backend. Marcar cada ítem (`[x]`) al completarlo.

### Backend

#### Base de Datos
- [ ] Crear migración Flyway `V1__create_folio_sequence.sql`:
  - `CREATE SEQUENCE folio_sequence`
  - `CREATE TABLE folio_sequence_ctrl (id BIGINT PRIMARY KEY DEFAULT 1)`
  - `INSERT INTO folio_sequence_ctrl VALUES (1) ON CONFLICT DO NOTHING`
- [ ] Configurar Flyway en `application.properties` apuntando a `insurance_core_db`

#### Dominio
- [ ] Crear Value Object `Folio` en `folio/domain/model/Folio.java` — campos: `folioNumber` (`String`), `generatedAt` (`Instant`)
- [ ] Crear Input Port `GenerateFolioUseCase` en `folio/domain/port/in/` — método `Folio generate()`
- [ ] Crear Output Port `FolioSequencePort` en `folio/domain/port/out/` — método `String nextFolioNumber()`

#### Aplicación
- [ ] Crear `GenerateFolioUseCaseImpl` en `folio/application/usecase/` — inyecta `FolioSequencePort` y `Clock`; llama `nextFolioNumber()` y construye `Folio` con `Instant.now(clock)`

#### Infraestructura — Persistencia
- [ ] Crear `FolioSequenceEntity` en `folio/infrastructure/adapter/out/persistence/entities/` — `@Entity`, `@Table(name = "folio_sequence_ctrl")`, campo `id` (`Long`)
- [ ] Crear `FolioRepository` en `folio/infrastructure/adapter/out/persistence/repositories/` — extiende `JpaRepository<FolioSequenceEntity, Long>`; método `@Query(nativeQuery = true) Long nextValue()`
- [ ] Crear `FolioSequenceJpaAdapter` en `folio/infrastructure/adapter/out/persistence/adapter/` — implementa `FolioSequencePort`; inyecta `FolioRepository` y `Clock`; formatea `FOL-<año>-<5dígitos>`

#### Infraestructura — REST
- [ ] Crear DTO `FolioResponse` en `folio/infrastructure/adapter/in/rest/dto/` — campos: `folioNumber` (`String`), `generatedAt` (`Instant`)
- [ ] Crear `FolioRestMapper` en `folio/infrastructure/adapter/in/rest/mapper/` — método `FolioResponse toResponse(Folio folio)`
- [ ] Crear Swagger interface `FolioApi` en `folio/infrastructure/adapter/in/rest/swaggerdocs/` — `@GetMapping("/v1/folios")`, `@Tag`, `@Operation`, `@ApiResponse(200)`
- [ ] Crear `FolioController` en `folio/infrastructure/adapter/in/rest/` — implementa `FolioApi`; inyecta `GenerateFolioUseCase` y `FolioRestMapper`; delega y mapea

#### Configuración
- [ ] Crear `FolioConfig` en `folio/infrastructure/config/` — `@Bean` para `GenerateFolioUseCaseImpl` + `Clock.systemUTC()`

#### Tests Backend (TDD — escribir antes de la implementación)
- [ ] `GenerateFolioUseCaseImplTest` — happy path: verifica que el folio retornado tiene el formato correcto y `generatedAt` no es nulo
- [ ] `GenerateFolioUseCaseImplTest` — verifica que se delega a `FolioSequencePort.nextFolioNumber()` exactamente una vez
- [ ] `FolioSequenceJpaAdapterTest` — verifica que el folio formateado sigue el patrón `FOL-<año>-<5dígitos>` dado un valor de secuencia mock
- [ ] `FolioRestMapperTest` — verifica que `toResponse(Folio)` mapea correctamente `folioNumber` y `generatedAt`
- [ ] `FolioControllerTest` — verifica que `GET /v1/folios` retorna HTTP 200 con `folioNumber` y `generatedAt`
- [ ] `FolioControllerTest` — verifica que el controlador delega al use case y usa el mapper (sin lógica propia)
- [ ] Test de integración `FolioGenerationIntegrationTest` — con `@SpringBootTest` + Testcontainers PostgreSQL: dos llamadas consecutivas producen folios con secuencias `N` y `N+1`
- [ ] Test de concurrencia `FolioSequenceConcurrencyTest` — 20 threads concurrentes: sin duplicados en el conjunto de folios generados

### QA
- [ ] Ejecutar skill `/gherkin-case-generator` → escenarios para CRITERIO-1.1, 1.2, 1.3, 1.4
- [ ] Ejecutar skill `/risk-identifier` → clasificación ASD de riesgos (concurrencia, DB unavailable)
- [ ] Validar manualmente con Bruno/Postman: `GET http://localhost:8081/v1/folios`
- [ ] Verificar que dos llamadas seguidas producen folios con secuencias consecutivas
- [ ] Actualizar estado spec: `status: IMPLEMENTED`

---
description: Reglas de backend para este proyecto (Java 21 + Spring Boot 4 + PostgreSQL + Arquitectura Hexagonal). Se aplica automáticamente a archivos de ambos microservicios backend.
paths:
  - "Insurance-Quoter-Back/**"
  - "Insurance-Quoter-Core/**"
  - "plataforma-danos-back/**"
  - "plataforma-core-ohs/**"
---

# Reglas de Backend — Java 21 + Spring Boot 4 + Arquitectura Hexagonal

## Microservicios

| Proyecto | Nombre lógico | Puerto | Base de datos | Responsabilidad |
|----------|--------------|--------|---------------|-----------------|
| `Insurance-Quoter-Back/` | `plataforma-danos-back` | 8080 | `insurance_quoter_db` (5432) | Cotizaciones, ubicaciones, coberturas, cálculo de prima |
| `Insurance-Quoter-Core/` | `plataforma-core-ohs` | 8081 | `insurance_core_db` (5433) | Catálogos, tarifas, agentes, suscriptores, folios, CP |

## Stack aprobado

- **Java 21** + **Spring Boot 4** (MVC REST, no reactivo)
- **Spring Data JPA** + **Hibernate** — acceso a datos
- **PostgreSQL** — base de datos relacional principal
- **Lombok** — reducción de boilerplate

**Prohibido:** WebFlux/Reactor, MongoDB, MyBatis, JDBC Template directo salvo casos justificados.

---

## Idioma del código

| Artefacto | Idioma |
|-----------|--------|
| Clases, métodos, variables, constantes, paquetes | **Inglés** |
| Tablas y columnas de base de datos | **Inglés** |
| Comentarios de código (`//`) | **Inglés** |
| Mensajes de error en API responses | **Inglés** |
| Documentación (specs, README, markdown) | **Español** |

Mapeo de términos de negocio → ver `CLAUDE.md` (diccionario de dominio).

---

## Arquitectura Hexagonal (Ports & Adapters)

```
Infrastructure (adapters) → Application (use cases) → Domain (model + ports)
```

La regla de dependencia es estricta: **domain no depende de nada**; application depende de domain; infrastructure depende de application y domain.

### Estructura de paquetes por bounded context

```
com.sofka.insurancequoter.<context>/
├── domain/
│   ├── model/               ← Entities, Value Objects, Aggregates (sin anotaciones Spring/JPA)
│   ├── service/             ← Domain Services (lógica de negocio pura)
│   └── port/
│       ├── in/              ← Input Ports: interfaces de casos de uso (ej. CreateFolioUseCase)
│       └── out/             ← Output Ports: interfaces de repositorio y servicios externos
│                               (ej. QuoteRepository, CoreServiceClient)
├── application/
│   └── usecase/             ← Implementaciones de Input Ports (orquestan domain + output ports)
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   └── rest/        ← Spring MVC Controllers (llaman Input Ports via DI)
    │   └── out/
    │       ├── persistence/ ← JPA Adapters: implementan Output Ports de repositorio
    │       └── http/        ← HTTP Client Adapters: implementan Output Ports de servicios externos
    └── config/              ← Beans de Spring, configuración, wiring de puertos y adaptadores
```

### Ejemplo de wiring

```java
// domain/port/in/CreateFolioUseCase.java
public interface CreateFolioUseCase {
    FolioResponse createFolio(CreateFolioCommand command);
}

// domain/port/out/QuoteRepository.java
public interface QuoteRepository {
    Optional<Quote> findByFolioNumber(String folioNumber);
    Quote save(Quote quote);
}

// application/usecase/CreateFolioUseCaseImpl.java
@Service
@RequiredArgsConstructor
public class CreateFolioUseCaseImpl implements CreateFolioUseCase {
    private final QuoteRepository quoteRepository;       // output port
    private final CoreServiceClient coreServiceClient;  // output port
    // lógica de negocio aquí
}

// infrastructure/adapter/in/rest/FolioController.java
@RestController
@RequiredArgsConstructor
public class FolioController {
    private final CreateFolioUseCase createFolioUseCase; // input port
    // solo parsea HTTP y delega
}

// infrastructure/adapter/out/persistence/QuoteJpaAdapter.java
@Component
@RequiredArgsConstructor
public class QuoteJpaAdapter implements QuoteRepository { // implementa output port
    private final QuoteJpaRepository jpaRepository;
}
```

### Capas y responsabilidades

| Capa | Paquete | Responsabilidad | Prohibido |
|------|---------|-----------------|-----------|
| `domain/model/` | `*.domain.model` | Entities, VOs, reglas de negocio puras | Anotaciones JPA/Spring, HTTP |
| `domain/port/in/` | `*.domain.port.in` | Interfaces de casos de uso | Implementación |
| `domain/port/out/` | `*.domain.port.out` | Interfaces de repos y clients externos | Implementación |
| `application/usecase/` | `*.application.usecase` | Orquesta domain + output ports | Queries JPA, HTTP directo |
| `infrastructure/adapter/in/rest/` | `*.infrastructure.adapter.in.rest` | Parsear HTTP, llamar input port | Lógica de negocio |
| `infrastructure/adapter/out/persistence/` | `*.infrastructure.adapter.out.persistence` | Implementar output ports con JPA | Lógica de negocio |
| `infrastructure/adapter/out/http/` | `*.infrastructure.adapter.out.http` | Implementar output ports con RestClient | Lógica de negocio |

---

## Convenciones de Código

- Clases en `PascalCase`; métodos y variables en `camelCase`
- Paquetes en `lowercase`: `com.sofka.insurancequoter.<context>.<layer>`
- Entidades JPA solo en `infrastructure/adapter/out/persistence/` — los domain models son POJOs
- API versionada: `/v1/...`
- Formato de error: `{ "error": "<message>", "code": "<ERROR_CODE>" }`
- Configuración desde `application.properties` — cero credenciales hardcodeadas
- Versionado optimista: `@Version` en la entidad JPA que mapea al aggregate raíz

## Nomenclatura de Archivos

| Artefacto | Convención | Ejemplo |
|-----------|-----------|---------|
| Input Port | `<Action>UseCase.java` | `CreateFolioUseCase.java` |
| Use Case Impl | `<Action>UseCaseImpl.java` | `CreateFolioUseCaseImpl.java` |
| Output Port | `<Entity>Repository.java` / `<Service>Client.java` | `QuoteRepository.java` |
| JPA Adapter | `<Entity>JpaAdapter.java` | `QuoteJpaAdapter.java` |
| JPA Entity | `<Entity>Jpa.java` | `QuoteJpa.java` |
| REST Controller | `<Context>Controller.java` | `FolioController.java` |
| DTO Request | `<Action>Request.java` | `CreateFolioRequest.java` |
| DTO Response | `<Context>Response.java` | `FolioResponse.java` |
| Test | `<Clase>Test.java` | `CreateFolioUseCaseImplTest.java` |

## Anti-patrones Prohibidos

- Anotaciones JPA en domain models (van solo en las entidades JPA de persistence adapter)
- Lógica de negocio en controllers o adapters de persistencia
- Domain models expuestos directamente en responses API (siempre mapear a DTO)
- Inyección de `JpaRepository` directamente en use cases (siempre via output port)
- `@Transactional` en controllers (solo en use cases o adapters de persistencia)
- Consultas N+1 sin `JOIN FETCH` o `@EntityGraph`
- Credenciales hardcodeadas

## Lineamientos completos

`.claude/docs/lineamientos/dev-guidelines.md` — Clean Code, SOLID, API REST, Seguridad, Observabilidad.

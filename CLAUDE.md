# Arquitectura (ejemplo equivalente)

Este documento muestra un ejemplo de arquitectura hexagonal equivalente a la que aplicaremos en el proyecto. Es únicamente un ejemplo ilustrativo: los paquetes, nombres de clases, rutas HTTP y números de puerto pueden diferir en nuestro caso.

Ejemplo de estructura:

```text
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

Notas y recomendaciones
- Equivalencia: mantener la misma separación de responsabilidades (domain / application / infrastructure). Los nombres y rutas pueden cambiar, pero las responsabilidades y las interfaces (puertos) deben conservarse.
- Puertos: definir interfaces en `domain.port` (input/out) para facilitar pruebas y desacoplar implementaciones.
- Adaptadores: colocar controladores REST y DTOs en `in` y persistencia en `out`. La lógica de negocio vive en `application.usecase`.
- Configuración: usar clases `@Configuration` (p. ej. `FolioConfig.java`) para efectuar wiring y exponer `Clock` o beans de infraestructura.
- Adaptaciones del proyecto: en este repositorio sustituiremos nombres de paquetes, endpoints y puertos por los que correspondan (p. ej. `/api/v1/folios` versus `/v1/folios`). Este archivo es una guía conceptual, no un mapeo literal.

Si quieres, hago commit y push del archivo ahora, y/o lo adapto a nombres concretos del proyecto.

## Agentes

Los agentes custom del proyecto viven en `.claude/agents/`. Siempre usar estos agentes — **nunca** los de `.github/`, que son para GitHub Copilot.

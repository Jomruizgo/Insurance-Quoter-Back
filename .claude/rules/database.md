---
description: Reglas de acceso a datos para este proyecto (PostgreSQL + Spring Data JPA + Hibernate + Lombok).
paths:
  - "**/domain/**"
  - "**/entity/**"
  - "**/entities/**"
  - "**/repository/**"
  - "**/repositories/**"
  - "**/migration/**"
  - "Insurance-Quoter-Back/src/main/resources/db/**"
---

# Reglas de Base de Datos — PostgreSQL + Spring Data JPA

## Stack aprobado

- **PostgreSQL** — base de datos relacional principal
- **Spring Data JPA** + **Hibernate** — ORM y acceso a datos
- **Lombok** — `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` en entidades
- **Spring Boot Docker Compose** — levanta PostgreSQL automáticamente en desarrollo (`compose.yaml`)

**Prohibido:** MongoDB, Motor, PyMongo, SQLite, acceso JDBC directo salvo casos justificados con comentario.

## Convenciones de Entidades JPA

```java
@Entity
@Table(name = "cotizaciones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_folio", nullable = false, unique = true)
    private String numeroFolio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cotizacion", nullable = false)
    private EstadoCotizacion estadoCotizacion;

    @Version
    private Long version;   // versionado optimista obligatorio en cotizaciones

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

## Convenciones de Nomenclatura

- Tablas en `snake_case_plural`: `cotizaciones`, `ubicaciones`, `coberturas`
- Columnas en `snake_case`: `numero_folio`, `estado_cotizacion`
- Entidades Java en `PascalCase`: `Cotizacion`, `Ubicacion`
- Campos Java en `camelCase`: `numeroFolio`, `estadoCotizacion`

## Tablas principales del dominio

| Entidad | Tabla | Relación clave |
|---------|-------|----------------|
| `Cotizacion` | `cotizaciones` | Aggregate raíz |
| `Ubicacion` | `ubicaciones` | `@ManyToOne` → `Cotizacion` |
| `Cobertura` | `coberturas` | `@ManyToOne` → `Cotizacion` |
| `ResultadoCalculo` | `resultados_calculo` | `@OneToOne` → `Cotizacion` |
| `PrimaPorUbicacion` | `primas_por_ubicacion` | `@ManyToOne` → `ResultadoCalculo` |

## Catálogos de referencia (read-only o stubs)

Según el reto, el servicio core provee catálogos. Si se implementan localmente:

| Colección lógica | Tabla sugerida |
|-----------------|---------------|
| Parámetros de cálculo | `parametros_calculo` |
| Tarifas incendio | `tarifas_incendio` |
| Tarifas CAT | `tarifas_cat` |
| Factores equipo electrónico | `factores_equipo_electronico` |
| Catálogo CP + zonas | `catalogo_cp_zonas` |
| Dim zona TEV | `dim_zona_tev` |
| Dim zona FHM | `dim_zona_fhm` |

## Patrón de Repositorio (obligatorio)

```java
// repository/CotizacionRepository.java
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    Optional<Cotizacion> findByNumeroFolio(String numeroFolio);

    @Query("SELECT c FROM Cotizacion c JOIN FETCH c.ubicaciones WHERE c.numeroFolio = :folio")
    Optional<Cotizacion> findByNumeroFolioWithUbicaciones(@Param("folio") String folio);
}
```

## Reglas de Diseño

- **Versionado optimista obligatorio** en `Cotizacion` y cualquier entidad con escrituras concurrentes (`@Version`)
- **Timestamps UTC** — `@CreationTimestamp` / `@UpdateTimestamp`, nunca calculados en cliente
- **Índices justificados** — solo crear índice con un caso de uso documentado
- **Sin datos sensibles en texto plano** — nunca almacenar contraseñas sin hash
- **Repositorio como única puerta de acceso** — services no usan `EntityManager` directamente salvo queries complejas
- **JOIN FETCH** para relaciones cargadas en el mismo query — evitar N+1
- **Paginación** con `Pageable` de Spring Data para listados

## Anti-patrones Prohibidos

- Entidades JPA en responses de API (siempre mapear a DTO)
- Lógica de negocio en repositorios
- `EntityManager` directo en services sin justificación
- Queries N+1 (iterar llamadas a DB en un bucle sin `JOIN FETCH`)
- `@Transactional` en controllers (solo en services)
- Columnas sin `nullable = false` cuando el campo es requerido
- Estado de conexión global mutable

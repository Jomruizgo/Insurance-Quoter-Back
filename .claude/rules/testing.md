---
description: Principios y convenciones de testing con TDD. Backend con JUnit 5 + Spring Boot Test. Frontend con Jasmine + Karma (solo lógica, sin UI). Automatización con Serenity BDD + Screenplay.
paths:
  - "**/test/**"
  - "**/*.spec.ts"
  - "**/*Test.java"
  - "**/*Spec.java"
  - "Auto_Api_Screenplay/**"
  - "Auto_Front_Screenplay/**"
---

# Reglas de Testing — TDD

## Metodología: TDD obligatorio

```
RED   → escribir el test que falla (define el comportamiento esperado)
GREEN → implementar el mínimo código para que pase
REFACTOR → limpiar sin romper tests
```

**Regla de oro: no escribir código de producción sin un test en rojo que lo justifique.**

El ciclo TDD aplica a:
- ✅ **Backend**: toda lógica en services, repositories, cálculos de prima
- ✅ **Frontend**: services, guards, pipes, resolvers — cualquier lógica pura
- ❌ **Frontend UI**: componentes y templates no se testean — cambio frecuente, bajo ROI

## Stack de Testing por Capa

| Capa | Framework | Ubicación |
|------|-----------|-----------|
| Backend unitario | **JUnit 5** + **Mockito** + **Spring Boot Test** | `Insurance-Quoter-Back/src/test/` |
| Backend integración | **@SpringBootTest** + **@DataJpaTest** + **Testcontainers** (opcional) | `Insurance-Quoter-Back/src/test/` |
| Frontend unitario | **Jasmine** + **Karma** (Angular default) | `Insurance-Quoter-Front/src/` |
| API automatizado | **Serenity BDD** + **Screenplay** + **REST Assured** | `Auto_Api_Screenplay/` |
| UI E2E automatizado | **Serenity BDD** + **Screenplay** + **Selenium/Playwright** | `Auto_Front_Screenplay/` |

## Comandos de ejecución

```bash
# Backend — tests unitarios e integración
cd Insurance-Quoter-Back
./gradlew test

# Frontend — tests unitarios
cd Insurance-Quoter-Front
ng test

# Automatización API
cd Auto_Api_Screenplay
./gradlew test

# Automatización UI
cd Auto_Front_Screenplay
./gradlew test
```

## Principios Universales (independiente del framework)

### Estructura AAA / Given-When-Then obligatoria

```java
// GIVEN — preparar datos y contexto
// WHEN  — ejecutar la acción bajo prueba
// THEN  — verificar el resultado esperado
```

### Pirámide de Testing

| Nivel | % recomendado | Qué cubre |
|-------|--------------|-----------|
| **Unitarios** | ~70% | Lógica de negocio aislada con mocks |
| **Integración** | ~20% | Flujos entre capas, endpoints HTTP, JPA |
| **E2E / Serenity** | ~10% | Flujos críticos de usuario |

### Reglas de Oro del Testing

- **Independencia** — cada test se puede ejecutar solo, en cualquier orden
- **Aislamiento** — mockear dependencias externas (servicios externos, tiempo)
- **Determinismo** — sin `Thread.sleep()`, sin dependencia de fechas reales, sin datos de producción
- **Cobertura mínima ≥ 80%** en lógica de negocio (quality gate bloqueante)
- **Nombres descriptivos**:
  - Java: `should<Resultado>_when<Escenario>()` o `<método>_<escenario>_<resultado>()`
  - TypeScript: `'should <resultado> when <escenario>'`
- **Un assert lógico por test** — si necesitas varios, separar en tests distintos

### Por cada unidad cubrir

- ✅ Happy path — datos válidos, flujo exitoso
- ❌ Error path — excepción esperada, respuesta de error
- 🔲 Edge case — vacío, duplicado, límites, permisos

## Convenciones Backend (JUnit 5 + Mockito)

```java
@ExtendWith(MockitoExtension.class)
class CotizacionServiceTest {

    @Mock
    private CotizacionRepository cotizacionRepository;

    @InjectMocks
    private CotizacionServiceImpl cotizacionService;

    @Test
    void shouldCrearFolio_whenDatosValidos_thenRetornaFolioGenerado() {
        // GIVEN
        when(cotizacionRepository.save(any())).thenReturn(cotizacionMock());

        // WHEN
        var resultado = cotizacionService.crearFolio();

        // THEN
        assertThat(resultado.getNumeroFolio()).isNotBlank();
        verify(cotizacionRepository).save(any(Cotizacion.class));
    }
}
```

- Usar `@DataJpaTest` para tests de repositorios (base de datos en memoria H2 o Testcontainers)
- Usar `@WebMvcTest` para tests de controllers (sin levantar contexto completo)
- Usar `@SpringBootTest` solo para tests de integración end-to-end del backend

## Convenciones Frontend (Jasmine + Angular Testing)

**Alcance**: SOLO services, guards, pipes y resolvers. Los componentes y templates NO se testean.

```typescript
// cotizacion.service.spec.ts  ← TDD: este archivo se crea ANTES del service
describe('CotizacionService', () => {
  let service: CotizacionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CotizacionService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(CotizacionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should crear folio and return folio number', () => {
    // GIVEN
    const mockResponse: FolioResponse = { numeroFolio: 'FOL-001' };

    // WHEN
    service.crearFolio().subscribe(res => {
      // THEN
      expect(res.numeroFolio).toBe('FOL-001');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/v1/folios`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  afterEach(() => httpMock.verify());
});
```

**Lo que NO se escribe en el proyecto:**
```typescript
// ❌ NO — test de componente / template
describe('CotizacionFormComponent', () => {
  // No crear tests de componentes, fixtures, renders ni detectChanges
});
```

## Flujos críticos para Serenity BDD (mínimo 3)

Según el reto, los flujos automatizados mínimos justificados son:

1. **Creación y consulta de folio** — POST /v1/folios + GET /v1/quotes/{folio}/state
2. **Registro y edición de ubicación** — PUT /v1/quotes/{folio}/locations + PATCH /v1/quotes/{folio}/locations/{índice}
3. **Cálculo de prima** — POST /v1/quotes/{folio}/calculate + validar prima neta/comercial
4. **Manejo de ubicación incompleta** — calcular con ubicación sin CP válido → esperar alerta, no error fatal

## Anti-patrones Prohibidos

- Tests que dependen del orden de ejecución
- Llamadas reales a servicios externos sin stub/mock
- `Thread.sleep()` / `setTimeout` arbitrarios en tests
- Lógica condicional dentro de un test (if/else)
- Datos de producción real en fixtures
- `@SpringBootTest` donde `@WebMvcTest` o `@DataJpaTest` son suficientes

## Estrategia de Regresión

- **Smoke suite** (`@Tag("smoke")`): happy paths críticos → corre en cada PR
- **Regresión completa** (`@Tag("regression")`): todo → corre nightly o pre-release
- Cobertura reportada con Jacoco (backend) + Istanbul/Karma coverage (frontend)

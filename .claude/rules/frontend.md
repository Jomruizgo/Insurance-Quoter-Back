---
description: Reglas de frontend para este proyecto (Angular 19 + TypeScript + RxJS). Se aplica automáticamente a archivos frontend.
paths:
  - "Insurance-Quoter-Front/**"
  - "cotizador-danos-web/**"
---

# Reglas de Frontend — Angular 19 + TypeScript

## Stack aprobado

- **Angular 19** (última versión estable) con **TypeScript 5.x**
- **Angular Router** — rutas de la SPA
- **Angular HttpClient** — llamadas HTTP al backend
- **RxJS** — manejo reactivo de datos y observables
- **Angular Forms** — `ReactiveFormsModule` para formularios complejos
- **Standalone Components** — arquitectura sin NgModules (Angular 19 default)

**Prohibido:** React, Vue, jQuery, fetch directo en componentes, llamadas HTTP fuera de services, NgModules salvo integración con librerías legacy.

## Idioma

| Artefacto | Idioma |
|-----------|--------|
| Clases, métodos, variables, interfaces, archivos | **Inglés** |
| Comentarios de código (`//`) | **Inglés** |
| Documentación (specs, README, markdown) | **Español** |
| Textos de la interfaz de usuario (labels, botones, mensajes, placeholders) | **Español** |

Los textos que el usuario ve en pantalla van en español (ej. `"Crear cotización"`, `"Código postal inválido"`). Los términos internacionales comunes se mantienen en inglés (ej. "online", "dashboard", "folio").

## Metodología: TDD para lógica

Se aplica **TDD (Test-Driven Development)** en la capa de lógica del frontend:

```
1. Escribir el test del service / guard / pipe (RED)
2. Implementar el mínimo código que lo haga pasar (GREEN)
3. Refactorizar manteniendo tests en verde (REFACTOR)
```

**Qué se testea con TDD:**
- ✅ Services (`*.service.ts`) — llamadas HTTP, transformaciones, lógica de negocio
- ✅ Guards (`*.guard.ts`) — lógica de navegación y control de acceso
- ✅ Pipes (`*.pipe.ts`) — transformaciones de datos
- ✅ Resolvers y funciones utilitarias puras

**Qué NO se testea:**
- ❌ Componentes y templates (HTML/SCSS) — no se prueban interacciones de UI
- ❌ Páginas (route components) — no se prueban renders ni bindings

La razón es maximizar ROI: la lógica de negocio en services es estable y crítica; los templates cambian con frecuencia y su valor de test es bajo.

## Arquitectura por Capas

```
services → components/smart → components/dumb → pages (route components)
```

| Capa | Carpeta | Responsabilidad | Prohibido |
|------|---------|----------------|-----------|
| `pages/` / route components | `app/<feature>/` | Layout, composición de componentes, usa servicios vía DI | Lógica de negocio, llamadas HTTP directas |
| `components/` | `shared/components/` o `<feature>/components/` | Render UI, recibir `@Input()`, emitir `@Output()` | Estado global, llamadas HTTP |
| `services/` | `<feature>/services/` o `core/services/` | Llamadas HTTP (HttpClient), transformación de datos | Estado de UI, lógica de render |
| `models/` | `shared/models/` o `<feature>/models/` | Interfaces y tipos TypeScript del dominio | Lógica de negocio, HTTP |

## Convenciones Obligatorias

- **Tipado estricto**: `strict: true` en `tsconfig.json` — cero `any` sin justificación
- **Inyección de dependencias**: siempre con `inject()` o constructor DI — sin instanciación manual de servicios
- **HttpClient**: SIEMPRE en services, NUNCA en componentes directamente
- **Observables**: retornar `Observable<T>` desde services; suscribir en componentes con `async pipe` preferentemente
- **Variables de entorno**: usar `environment.ts` / `environment.prod.ts` para URLs y configuración
- **API base URL**: `environment.apiUrl` — nunca hardcodear URLs

## Llamadas a la API (patrón obligatorio)

```typescript
// services/cotizacion.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CotizacionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1`;

  crearFolio(): Observable<FolioResponse> {
    return this.http.post<FolioResponse>(`${this.baseUrl}/folios`, {});
  }
}
```

## Rutas del cotizador

```
/cotizador                           → selección / creación de folio
/quotes/:folio/general-info          → datos generales
/quotes/:folio/locations             → ubicaciones
/quotes/:folio/technical-info        → info técnica y coberturas
/quotes/:folio/terms-and-conditions  → cálculo y resultados
```

## Nomenclatura de Archivos

| Artefacto | Convención | Ejemplo |
|-----------|-----------|---------|
| Componente | `<feature>.component.ts` + `.html` + `.scss` | `cotizacion-form.component.ts` |
| Service | `<feature>.service.ts` | `cotizacion.service.ts` |
| Model/Interface | `<feature>.model.ts` | `cotizacion.model.ts` |
| Guard | `<feature>.guard.ts` | `folio-activo.guard.ts` |
| Module/Route | `<feature>.routes.ts` | `cotizador.routes.ts` |
| Test | `<clase>.spec.ts` | `cotizacion.service.spec.ts` |

- `kebab-case` para nombres de archivo
- `PascalCase` para clases (`CotizacionFormComponent`)
- `camelCase` para variables, métodos y propiedades

## Estructura de Referencia

```
Insurance-Quoter-Front/src/
├── app/
│   ├── core/
│   │   ├── services/          ← servicios singleton (http, auth)
│   │   └── models/            ← interfaces de dominio compartidas
│   ├── shared/
│   │   └── components/        ← componentes reutilizables
│   └── features/
│       ├── cotizador/         ← feature principal
│       │   ├── pages/
│       │   ├── components/
│       │   ├── services/
│       │   └── cotizador.routes.ts
│       └── ...
├── environments/
│   ├── environment.ts
│   └── environment.prod.ts
└── main.ts
```

## Anti-patrones Prohibidos

- `HttpClient` directamente en componentes (siempre en services)
- Hardcodear URLs de API (usar `environment.apiUrl`)
- Subscribir observables sin `async pipe` o sin `takeUntilDestroyed()`
- Usar `any` sin comentario justificando la excepción
- Lógica de negocio en componentes (va en services)
- Módulos de Angular si el proyecto usa standalone components (preferir standalone)

## Lineamientos completos

`.claude/docs/lineamientos/dev-guidelines.md` — Clean Code, SOLID, API REST, Seguridad, Observabilidad.

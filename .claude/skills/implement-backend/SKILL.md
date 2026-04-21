---
name: implement-backend
description: Implementa un feature completo en el backend con TDD. Requiere spec con status APPROVED en .claude/specs/.
argument-hint: "<nombre-feature>"
---

# Implement Backend

## Prerequisitos
1. Leer spec: `.claude/specs/<feature>.spec.md` — secciones 2 y 3 (diseño + lista de tareas)
2. Leer stack y arquitectura: `.claude/rules/backend.md`
3. Verificar autenticación `gh auth status` — se necesita para cerrar issues

## Metodología: TDD obligatorio

**Cada clase de lógica se implementa en este ciclo, sin excepciones:**

```
1. RED   — escribir el test unitario (falla porque la clase aún no existe)
2. GREEN — escribir el mínimo código que hace pasar el test
3. REFACTOR — limpiar sin romper el test
```

**Orden de implementación por capa:**

```
migración DB → dominio (model + ports) → application (use case) → persistence adapter → REST adapter → config
```

Para cada capa:
1. Escribir primero el test (`src/test/java/...`)
2. Luego crear la clase de producción (`src/main/java/...`)
3. Verificar que el test pasa (`./gradlew test --tests <NombreTest>`)
4. Cerrar el GitHub Issue correspondiente con `gh issue close <N> --comment "implemented"`

## Cierre de issues

Cada tarea de la sección `## 3. LISTA DE TAREAS` de la spec tiene un GitHub Issue asociado.
Al completar cada tarea (test verde + código limpio), cerrar el issue correspondiente:

```bash
gh issue close <N> --comment "implemented"
```

Al final del feature, todos los issues de implementación y tests deben estar cerrados.

## Restricciones
- Solo `src/` del proyecto backend asignado. No tocar frontend.
- Test unitario **siempre antes** del código de producción — nunca al revés.
- No delegar los tests a otro agente; son parte del ciclo TDD de este skill.
- `@Autowired` prohibido — inyección por constructor siempre.
- Swagger (`@Tag`, `@Operation`) solo en interfaces `swaggerdocs/*Api.java`, nunca en controllers.
- Domain models sin anotaciones JPA ni Spring.

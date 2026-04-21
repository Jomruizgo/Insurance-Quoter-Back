---
name: tasks-to-issues
description: Convierte la LISTA DE TAREAS de una spec ASDD aprobada en GitHub Issues reales (gh CLI). Un issue por tarea, agrupados por capa arquitectónica. Solo backend — sin distinción front/back.
argument-hint: "<nombre-feature> (ej: folio-generator)"
---

# Tasks to Issues

Lee la spec indicada en `$ARGUMENTS`, extrae cada ítem de `## 3. LISTA DE TAREAS` y crea un GitHub Issue real por tarea usando `gh issue create`.

## Prerequisitos

- La spec `.claude/specs/<feature>.spec.md` debe tener `status: APPROVED`, `IN_PROGRESS` o `IMPLEMENTED`
- `gh auth status` debe estar autenticado
- El remoto Git debe ser GitHub

## Comportamiento

Delega al agente `tasks-to-issues` pasándole el argumento recibido. El agente:

1. Verifica la spec y el estado de autenticación de `gh`
2. Extrae **todas** las tareas sin filtrar por front/back 
3. Asigna labels por capa: `database`, `domain`, `application`, `infrastructure`, `persistence`, `rest`, `config`, `testing`, `qa`
4. Crea los issues en orden de dependencias (DB → dominio → aplicación → infra → config → tests → QA)
5. Reporta las URLs de todos los issues creados

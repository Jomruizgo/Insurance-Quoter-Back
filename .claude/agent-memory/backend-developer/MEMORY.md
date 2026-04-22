# Agent Memory — backend-developer

- [folio-generator feature — implementation state](project_folio_feature.md) — SPEC-001 implementado, compila. Estructura de paquetes, decisiones de diseño y archivos clave.
- [Insurance-Quoter-Core — setup y configuración base](project_core_setup.md) — Paquete base, puerto, DB, dependencias reales del proyecto core.
- [location-layout feature — implementation state](project_location_layout_feature.md) — SPEC-003 implementado. Decisiones de diseño: optimistic lock manual, sin @Transactional en integration test, GlobalExceptionHandler ampliado.
- [location-management feature — implementation state](project_location_management_feature.md) — SPEC-004 implementado, issues #82-#118 cerrados. Input ports con records internos, AttributeConverter para JSONB, @WebMvcTest no existe en Spring Boot 4.

## Reto Técnico

### Objetivo

Construir una solución funcional para un *cotizador de daños* que permita capturar un folio, registrar información general, administrar ubicaciones de riesgo, calcular la prima neta/comercial y mostrar el resultado en una interfaz web.

El reto debe evaluar capacidades de:
- Diseño y construcción de backend
- Construcción de frontend
- Integración entre servicios
- Modelado de datos
- Manejo de reglas de negocio
- Calidad de código
- Pruebas unitarias y automatizadas
- Documentación técnica y operativa

---

### Contexto del negocio

La solución representa un cotizador de seguros de daños compuesto por tres bloques:
- *cotizador-danos-web:* SPA para captura y consulta
- *plataforma-danos-back:* backend principal que administra la cotización
- *plataforma-core-ohs:* servicio de referencia con catálogos, tarifas, agentes, códigos postales y folios

*El flujo esperado es:*
1. El usuario crea o recupera un folio.
2. Captura datos generales de la cotización.
3. Configura el layout y registra una o múltiples ubicaciones.
4. El backend consulta catálogos y tarifas técnicas.
5. El backend calcula la prima por ubicación y la prima total.
6. El frontend presenta alertas, estados y desglose financiero.

---

### Alcance funcional obligatorio

#### Backend

Implementar un backend que:
- Cree folios con idempotencia
- Consulte y guarde datos generales de una cotización
- Consulte y guarde la configuración del layout de ubicaciones
- Registre, consulte y edite ubicaciones
- Consulte el estado de la cotización
- Consulte y guarde opciones de cobertura
- Ejecute el cálculo de prima neta y prima comercial
- Persista el resultado financiero sin sobrescribir otras secciones de la cotización
- Maneje versionado optimista en operaciones de edición

*Endpoints mínimos esperados:*
- POST /v1/folios
- GET /v1/quotes/{folio}/general-info
- PUT /v1/quotes/{folio}/general-info
- GET /v1/quotes/{folio}/locations/layout
- PUT /v1/quotes/{folio}/locations/layout
- GET /v1/quotes/{folio}/locations
- PUT /v1/quotes/{folio}/locations
- PATCH /v1/quotes/{folio}/locations/{índice}
- GET /v1/quotes/{folio}/locations/summary
- GET /v1/quotes/{folio}/state
- GET /v1/quotes/{folio}/coverage-options
- PUT /v1/quotes/{folio}/coverage-options
- POST /v1/quotes/{folio}/calculate

#### Frontend

Implementar una SPA que permita:
- Crear o abrir un folio
- Capturar datos generales
- Consultar suscriptores, agentes, giros y códigos postales
- Capturar una o varias ubicaciones
- Editar una ubicación puntual
- Visualizar el progreso y estado del folio
- Configurar opciones de cobertura
- Ejecutar el cálculo
- Mostrar la prima neta, la prima comercial y el desglose por ubicación
- Mostrar alertas de ubicaciones incompletas sin bloquear completamente el folio

*Rutas funcionales mínimas sugeridas:*
- /cotizador
- /quotes/{folio}/general-info
- /quotes/{folio}/locations
- /quotes/{folio}/technical-info
- /quotes/{folio}/terms-and-conditions

---

### Reglas de negocio obligatorias

- La cotización se identifica por numeroFolio
- El backend debe persistir la cotización como agregado principal
- Las escrituras deben hacerse por actualización parcial
- Al editar secciones funcionales, debe incrementarse la versión
- Debe actualizarse fechaUltimaActualizacion
- El cálculo debe guardar primaNeta, primaComercial y primasPorUbicacion en una misma operación lógica
- Si una ubicación está incompleta, genera alerta, pero no debe impedir calcular las demás
- Una ubicación no debe calcularse si no tiene código postal válido, giro.claveIncendio o garantías tarifables

---

### Dominio mínimo esperado

*Cotización* debe contemplar como mínimo:
numeroFolio, estadoCotizacion, datosAsegurado, datosConduccion.codigoAgente, clasificacionRiesgo, tipoNegocio, configuracionLayout, opcionesCobertura, ubicaciones[], primaNeta, primaComercial, primasPorUbicacion[], version, metadatos

*Ubicación* debe incluir al menos:
índice, nombreUbicacion, direccion, codigoPostal, estado, municipio, colonia, ciudad, tipoConstructivo, nivel, anioConstruccion, giro, giro.claveIncendio, garantías[], zonaCatastrofica, alertasBloqueantes, estadoValidacion

---

### Integración con servicios de referencia

El backend debe consumir o simular las siguientes capacidades del servicio core:
- Catálogo de suscriptores
- Consulta de agente por clave
- Consulta de giros
- Validación y consulta de código postal
- Generación secuencial de folio
- Consulta de catálogos de clasificación de riesgo y garantías
- Consulta de tarifas y factores técnicos

*Endpoints de referencia del servicio core:*
- GET /v1/subscribers
- GET /v1/agents
- GET /v1/business-lines
- GET /v1/zip-codes/{zipCode}
- POST /v1/zip-codes/validate
- GET /v1/folios
- GET /v1/catalogs/risk-classification
- GET /v1/catalogs/guarantees
- GET|PUT /v1/tariffs

Si no se implementa un servicio real adicional, se acepta un stub, mock server o fixtures versionados siempre que el contrato quede documentado.

---

### Cálculo técnico mínimo

El cálculo de prima debe:
1. Leer la cotización completa por folio.
2. Leer parámetros globales de cálculo.
3. Resolver datos técnicos requeridos por ubicación.
4. Determinar si cada ubicación es calculable o incompleta.
5. Calcular prima por ubicación.
6. Consolidar prima neta total.
7. Derivar prima comercial total.
8. Persistir el resultado financiero.

*Componentes técnicos que el reto debe contemplar en el desglose:*
- Incendio edificios
- Incendio contenidos
- Extensión de cobertura
- CATTEV
- CATFHM
- Remoción de escombros
- Gastos extraordinarios
- Pérdida de rentas
- BI
- Equipo electrónico
- Robo
- Dinero y valores
- Vidrios
- Anuncios luminosos

No es obligatorio replicar exactamente una fórmula actuarial real sino fue entregada, pero sí debe existir una lógica consistente, trazable y documentada.

---

### Datos técnicos y colecciones de referencia

- cotizaciones_danos
- parametros_calculo
- tarifas_incendio
- tarifas_cat
- tarifa_fhm
- factores_equipo_electronico
- catalogo_cp_zonas
- dim_zona_tev
- dim_zona_fhm

---

### Requerimientos de pruebas

*Pruebas unitarias* — cobertura mínima del 80%:
- Casos de uso del backend
- Validaciones de negocio
- Cálculo de prima
- Repositorios o adaptadores críticos con mocks
- Componentes o hooks clave del frontend
- Transformaciones o mapeos relevantes

*Pruebas automatizadas* — mínimo 3 flujos críticos justificados:
- Endpoints principales del backend
- Flujo de creación y actualización de folio
- Captura y edición de ubicaciones
- Ejecución del cálculo
- Manejo de ubicaciones incompletas
- Flujo principal del frontend

Se acepta: pruebas de integración backend, contract tests, pruebas end to end, o combinación de las anteriores.

---

### Documentación requerida

- Descripción de arquitectura
- Decisiones técnicas relevantes
- Instrucciones de instalación y ejecución
- Variables de entorno necesarias
- Contratos API
- Modelo de datos principal
- Explicación de la lógica de cálculo implementada
- Estrategia de pruebas
- Supuestos y limitaciones

---

### Entregables esperados

*Condición obligatoria: Uso obligatorio de la metodología ASSD.*

El participante debe entregar:
- Todos los Specs generados de la metodología ASSD
- Un video en YouTube de *máximo 10 minutos en modo oculto* (modo privado causa descalificación)
- Repositorio de código fuente — GitLab Sofka
- Pruebas unitarias
- Pruebas automatizadas
- Archivo README.md principal
- Colección de requests o documentación equivalente de APIs
- Scripts de arranque local
- Fixtures, mocks o semillas de datos

*Opcional:*
- docker-compose.yml
- Pipeline CI
- Colección Postman o Bruno
- Cobertura de pruebas

---

### Criterios de evaluación

Se evaluará:
- Claridad del modelado del dominio
- Separación entre capas y responsabilidades
- Calidad del código
- Consistencia de APIs y manejo de errores
- Experiencia de usuario en frontend
- Cobertura y calidad de pruebas
- Argumentación de los flujos automatizados
- Trazabilidad del cálculo
- Calidad de la documentación
- Facilidad de ejecución local

---

### Restricciones y supuestos sugeridos

- Pueden usar stubs o mocks para integraciones externas no previstas
- Pueden simplificar autenticación si no forma parte del objetivo
- Deben priorizar claridad y trazabilidad sobre complejidad innecesaria
- Cualquier fórmula simplificada debe estar documentada
- Cualquier dato no entregado debe resolverse con supuestos explícitos

---

### Escenario de aceptación sugerido

La solución debe permitir demostrar este escenario mínimo:
1. Crear un folio nuevo.
2. Capturar datos generales.
3. Definir layout de ubicaciones.
4. Registrar al menos dos ubicaciones.
5. Dejar una ubicación completa y una incompleta.
6. Configurar opciones de cobertura.
7. Ejecutar el cálculo.
8. Ver prima calculada para la ubicación válida.
9. Ver alerta para la ubicación incompleta.
10. Consultar el estado final del folio.

---

### Resultado esperado del reto

Al finalizar, el participante debe entregar una solución ejecutable y documentada que demuestre capacidad para construir un flujo integral de cotización con backend, frontend, persistencia, integración, pruebas y documentación técnica
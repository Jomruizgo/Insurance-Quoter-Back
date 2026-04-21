# Contratos de API — Insurance Quoter

Documento de referencia para el equipo de frontend y backend. Define los contratos HTTP que ambos lados deben respetar sin excepción.

**Convenciones:**
- Todos los campos en `camelCase` en JSON
- Timestamps en ISO 8601 UTC: `"2026-04-20T14:30:00Z"`
- Versionado optimista: toda operación de escritura recibe y retorna `version`
- Errores siempre con la forma `{ "error": "<message>", "code": "<ERROR_CODE>" }`
- Base URL backend: `http://localhost:8080`

---

## Índice

1. [Folios](#1-folios)
2. [Datos Generales](#2-datos-generales)
3. [Layout de Ubicaciones](#3-layout-de-ubicaciones)
4. [Ubicaciones](#4-ubicaciones)
5. [Estado de Cotización](#5-estado-de-cotización)
6. [Opciones de Cobertura](#6-opciones-de-cobertura)
7. [Cálculo de Prima](#7-cálculo-de-prima)
8. [Servicio Core (stubs / catálogos)](#8-servicio-core-stubs--catálogos)
9. [Códigos de error globales](#9-códigos-de-error-globales)

---

## 1. Folios

### POST /v1/folios
Crea un nuevo folio con idempotencia. Si ya existe un folio para los mismos parámetros sin cotización iniciada, retorna el existente.

**Request body:**
```json
{
  "subscriberId": "SUB-001",
  "agentCode": "AGT-123"
}
```

**Response 201 — Created:**
```json
{
  "folioNumber": "FOL-2026-00042",
  "quoteStatus": "CREATED",
  "underwritingData": {
    "subscriberId": "SUB-001",
    "agentCode": "AGT-123"
  },
  "createdAt": "2026-04-20T14:30:00Z",
  "version": 1
}
```

**Response 200 — Already exists (idempotent):**
```json
{
  "folioNumber": "FOL-2026-00042",
  "quoteStatus": "CREATED",
  "underwritingData": {
    "subscriberId": "SUB-001",
    "agentCode": "AGT-123"
  },
  "createdAt": "2026-04-20T14:00:00Z",
  "version": 1
}
```

**Response 400:** `{ "error": "Invalid subscriber or agent", "code": "INVALID_REFERENCE" }`  
**Response 422:** `{ "error": "Validation failed", "code": "VALIDATION_ERROR", "fields": [...] }`

---

## 2. Datos Generales

### GET /v1/quotes/{folio}/general-info
Obtiene los datos generales de la cotización.

**Path param:** `folio` — número de folio (ej. `FOL-2026-00042`)

**Response 200:**
```json
{
  "folioNumber": "FOL-2026-00042",
  "quoteStatus": "IN_PROGRESS",
  "insuredData": {
    "name": "Empresa Ejemplo SA de CV",
    "rfc": "EEJ900101ABC",
    "email": "contacto@empresa.com",
    "phone": "5512345678"
  },
  "underwritingData": {
    "agentCode": "AGT-123",
    "subscriberId": "SUB-001",
    "riskClassification": "STANDARD",
    "businessType": "COMMERCIAL"
  },
  "updatedAt": "2026-04-20T15:00:00Z",
  "version": 2
}
```

**Response 404:** `{ "error": "Folio not found", "code": "FOLIO_NOT_FOUND" }`

---

### PUT /v1/quotes/{folio}/general-info
Actualiza los datos generales. Requiere `version` para control de concurrencia.

**Request body:**
```json
{
  "insuredData": {
    "name": "Empresa Ejemplo SA de CV",
    "rfc": "EEJ900101ABC",
    "email": "contacto@empresa.com",
    "phone": "5512345678"
  },
  "underwritingData": {
    "agentCode": "AGT-123",
    "subscriberId": "SUB-001",
    "riskClassification": "STANDARD",
    "businessType": "COMMERCIAL"
  },
  "version": 2
}
```

**Response 200:**
```json
{
  "folioNumber": "FOL-2026-00042",
  "quoteStatus": "IN_PROGRESS",
  "insuredData": { ... },
  "underwritingData": { ... },
  "updatedAt": "2026-04-20T15:10:00Z",
  "version": 3
}
```

**Response 409:** `{ "error": "Optimistic lock conflict", "code": "VERSION_CONFLICT" }`  
**Response 404:** `{ "error": "Folio not found", "code": "FOLIO_NOT_FOUND" }`  
**Response 422:** `{ "error": "Validation failed", "code": "VALIDATION_ERROR", "fields": [...] }`

---

## 3. Layout de Ubicaciones

### GET /v1/quotes/{folio}/locations/layout
Obtiene la configuración de layout de ubicaciones.

**Response 200:**
```json
{
  "folioNumber": "FOL-2026-00042",
  "layoutConfiguration": {
    "numberOfLocations": 3,
    "locationType": "MULTIPLE"
  },
  "version": 3
}
```

---

### PUT /v1/quotes/{folio}/locations/layout
Define cuántas y qué tipo de ubicaciones tendrá la cotización.

**Request body:**
```json
{
  "layoutConfiguration": {
    "numberOfLocations": 3,
    "locationType": "MULTIPLE"
  },
  "version": 3
}
```

**Response 200:**
```json
{
  "folioNumber": "FOL-2026-00042",
  "layoutConfiguration": {
    "numberOfLocations": 3,
    "locationType": "MULTIPLE"
  },
  "updatedAt": "2026-04-20T15:20:00Z",
  "version": 4
}
```

**Response 409:** `{ "error": "Optimistic lock conflict", "code": "VERSION_CONFLICT" }`

---

## 4. Ubicaciones

### GET /v1/quotes/{folio}/locations
Lista todas las ubicaciones de la cotización.

**Response 200:**
```json
{
  "folioNumber": "FOL-2026-00042",
  "locations": [
    {
      "index": 1,
      "locationName": "Bodega Principal",
      "address": "Av. Insurgentes 1000",
      "zipCode": "06600",
      "state": "Ciudad de México",
      "municipality": "Cuauhtémoc",
      "neighborhood": "Juárez",
      "city": "Ciudad de México",
      "constructionType": "MASONRY",
      "level": 2,
      "constructionYear": 1995,
      "businessLine": {
        "code": "BL-001",
        "fireKey": "FK-INC-01",
        "description": "Bodega de mercancías"
      },
      "guarantees": [
        { "code": "GUA-FIRE", "insuredValue": 5000000 },
        { "code": "GUA-THEFT", "insuredValue": 500000 }
      ],
      "catastrophicZone": "ZONE_A",
      "validationStatus": "COMPLETE",
      "blockingAlerts": []
    }
  ],
  "version": 4
}
```

---

### PUT /v1/quotes/{folio}/locations
Reemplaza la lista completa de ubicaciones.

**Request body:**
```json
{
  "locations": [
    {
      "index": 1,
      "locationName": "Bodega Principal",
      "address": "Av. Insurgentes 1000",
      "zipCode": "06600",
      "constructionType": "MASONRY",
      "level": 2,
      "constructionYear": 1995,
      "businessLine": {
        "code": "BL-001",
        "fireKey": "FK-INC-01"
      },
      "guarantees": [
        { "code": "GUA-FIRE", "insuredValue": 5000000 }
      ]
    }
  ],
  "version": 4
}
```

**Response 200:**
```json
{
  "folioNumber": "FOL-2026-00042",
  "locations": [ ... ],
  "updatedAt": "2026-04-20T15:30:00Z",
  "version": 5
}
```

---

### PATCH /v1/quotes/{folio}/locations/{index}
Actualiza parcialmente una ubicación específica. Solo se aplican los campos enviados.

**Path param:** `index` — número entero del índice de la ubicación (1-based)

**Request body** (campos opcionales — solo los que cambian):
```json
{
  "locationName": "Bodega Norte",
  "zipCode": "44100",
  "guarantees": [
    { "code": "GUA-FIRE", "insuredValue": 7000000 }
  ],
  "version": 5
}
```

**Response 200:**
```json
{
  "folioNumber": "FOL-2026-00042",
  "location": {
    "index": 1,
    "locationName": "Bodega Norte",
    "zipCode": "44100",
    "validationStatus": "COMPLETE",
    "blockingAlerts": [],
    ...
  },
  "updatedAt": "2026-04-20T15:35:00Z",
  "version": 6
}
```

**Response 404:** `{ "error": "Location index not found", "code": "LOCATION_NOT_FOUND" }`  
**Response 409:** `{ "error": "Optimistic lock conflict", "code": "VERSION_CONFLICT" }`

---

### GET /v1/quotes/{folio}/locations/summary
Resumen de validación de todas las ubicaciones (para mostrar alertas sin cargar detalle completo).

**Response 200:**
```json
{
  "folioNumber": "FOL-2026-00042",
  "totalLocations": 3,
  "completeLocations": 2,
  "incompleteLocations": 1,
  "locations": [
    {
      "index": 1,
      "locationName": "Bodega Principal",
      "validationStatus": "COMPLETE",
      "blockingAlerts": []
    },
    {
      "index": 2,
      "locationName": "Oficina Sur",
      "validationStatus": "INCOMPLETE",
      "blockingAlerts": [
        { "code": "MISSING_ZIP_CODE", "message": "Código postal requerido" },
        { "code": "MISSING_FIRE_KEY", "message": "Clave incendio requerida" }
      ]
    }
  ]
}
```

---

## 5. Estado de Cotización

### GET /v1/quotes/{folio}/state
Retorna el estado actual y el progreso de completitud del folio.

**Response 200:**
```json
{
  "folioNumber": "FOL-2026-00042",
  "quoteStatus": "IN_PROGRESS",
  "completionPercentage": 75,
  "sections": {
    "generalInfo": "COMPLETE",
    "layout": "COMPLETE",
    "locations": "INCOMPLETE",
    "coverageOptions": "PENDING",
    "calculation": "PENDING"
  },
  "version": 6,
  "updatedAt": "2026-04-20T15:35:00Z"
}
```

**Valores de `quoteStatus`:** `CREATED` | `IN_PROGRESS` | `CALCULATED` | `ISSUED`  
**Valores de sección:** `PENDING` | `IN_PROGRESS` | `COMPLETE` | `INCOMPLETE`

---

## 6. Opciones de Cobertura

### GET /v1/quotes/{folio}/coverage-options
Obtiene las opciones de cobertura configuradas para la cotización.

**Response 200:**
```json
{
  "folioNumber": "FOL-2026-00042",
  "coverageOptions": [
    {
      "code": "COV-FIRE",
      "description": "Incendio y riesgos adicionales",
      "selected": true,
      "deductiblePercentage": 2.0,
      "coinsurancePercentage": 80.0
    },
    {
      "code": "COV-THEFT",
      "description": "Robo con violencia",
      "selected": false,
      "deductiblePercentage": 5.0,
      "coinsurancePercentage": 100.0
    }
  ],
  "version": 6
}
```

---

### PUT /v1/quotes/{folio}/coverage-options
Actualiza las opciones de cobertura seleccionadas.

**Request body:**
```json
{
  "coverageOptions": [
    {
      "code": "COV-FIRE",
      "selected": true,
      "deductiblePercentage": 2.0,
      "coinsurancePercentage": 80.0
    },
    {
      "code": "COV-THEFT",
      "selected": true,
      "deductiblePercentage": 5.0,
      "coinsurancePercentage": 100.0
    }
  ],
  "version": 6
}
```

**Response 200:**
```json
{
  "folioNumber": "FOL-2026-00042",
  "coverageOptions": [ ... ],
  "updatedAt": "2026-04-20T15:45:00Z",
  "version": 7
}
```

---

## 7. Cálculo de Prima

### POST /v1/quotes/{folio}/calculate
Ejecuta el cálculo de prima neta y prima comercial para todas las ubicaciones calculables.

Las ubicaciones sin `zipCode` válido, sin `businessLine.fireKey` o sin garantías tarifables generan una alerta pero **no bloquean** el cálculo de las demás.

**Request body:**
```json
{
  "version": 7
}
```

**Response 200:**
```json
{
  "folioNumber": "FOL-2026-00042",
  "quoteStatus": "CALCULATED",
  "netPremium": 48500.00,
  "commercialPremium": 56260.00,
  "premiumsByLocation": [
    {
      "index": 1,
      "locationName": "Bodega Principal",
      "netPremium": 48500.00,
      "commercialPremium": 56260.00,
      "calculable": true,
      "coverageBreakdown": {
        "fireBuildings": 20000.00,
        "fireContents": 15000.00,
        "coverageExtension": 3500.00,
        "cattev": 4000.00,
        "catfhm": 2500.00,
        "debrisRemoval": 1500.00,
        "extraordinaryExpenses": 1000.00,
        "rentalLoss": 0.00,
        "businessInterruption": 0.00,
        "electronicEquipment": 500.00,
        "theft": 0.00,
        "cashAndValues": 0.00,
        "glass": 0.00,
        "luminousSignage": 0.00
      },
      "blockingAlerts": []
    },
    {
      "index": 2,
      "locationName": "Oficina Sur",
      "netPremium": null,
      "commercialPremium": null,
      "calculable": false,
      "blockingAlerts": [
        { "code": "MISSING_ZIP_CODE", "message": "Código postal requerido" }
      ]
    }
  ],
  "calculatedAt": "2026-04-20T16:00:00Z",
  "version": 8
}
```

**Response 409:** `{ "error": "Optimistic lock conflict", "code": "VERSION_CONFLICT" }`  
**Response 422:** `{ "error": "No calculable locations", "code": "NO_CALCULABLE_LOCATIONS" }` — cuando **todas** las ubicaciones son incompletas.

---

## 8. Servicio Core (stubs / catálogos)

Estos endpoints los consume el backend. Si no se implementa un servicio externo real, se acepta un stub integrado.

### GET /v1/subscribers
```json
{
  "subscribers": [
    { "id": "SUB-001", "name": "Seguros Sofka" },
    { "id": "SUB-002", "name": "Aseguradora Norte" }
  ]
}
```

### GET /v1/agents
```json
{
  "agents": [
    { "code": "AGT-123", "name": "Juan Pérez", "subscriberId": "SUB-001" }
  ]
}
```

### GET /v1/business-lines
```json
{
  "businessLines": [
    { "code": "BL-001", "description": "Bodega de mercancías", "fireKey": "FK-INC-01" },
    { "code": "BL-002", "description": "Oficina administrativa", "fireKey": "FK-INC-02" }
  ]
}
```

### GET /v1/zip-codes/{zipCode}
```json
{
  "zipCode": "06600",
  "state": "Ciudad de México",
  "municipality": "Cuauhtémoc",
  "city": "Ciudad de México",
  "neighborhoods": ["Juárez", "Tabacalera"],
  "catastrophicZone": "ZONE_A",
  "tevZone": "TEV-1",
  "fhmZone": "FHM-2",
  "valid": true
}
```
**Response 404:** `{ "error": "Zip code not found", "code": "ZIP_CODE_NOT_FOUND" }`

### POST /v1/zip-codes/validate
**Request:** `{ "zipCode": "06600" }`  
**Response 200:** `{ "valid": true, "zipCode": "06600" }`

### GET /v1/folios
Genera el siguiente número de folio secuencial.
```json
{
  "folioNumber": "FOL-2026-00043",
  "generatedAt": "2026-04-20T14:30:00Z"
}
```

### GET /v1/catalogs/risk-classification
```json
{
  "riskClassifications": [
    { "code": "STANDARD", "description": "Riesgo estándar" },
    { "code": "PREFERRED", "description": "Riesgo preferente" },
    { "code": "SUBSTANDARD", "description": "Riesgo subestándar" }
  ]
}
```

### GET /v1/catalogs/guarantees
```json
{
  "guarantees": [
    { "code": "GUA-FIRE", "description": "Incendio edificios", "tarifable": true },
    { "code": "GUA-THEFT", "description": "Robo", "tarifable": true },
    { "code": "GUA-GLASS", "description": "Vidrios", "tarifable": true }
  ]
}
```

### GET /v1/tariffs
Retorna los factores técnicos y tarifas para el cálculo.
```json
{
  "tariffs": {
    "fireRate": 0.0015,
    "cattevFactor": 0.0008,
    "catfhmFactor": 0.0005,
    "theftRate": 0.003,
    "electronicEquipmentRate": 0.002
  }
}
```

---

## 9. Códigos de error globales

| Código | HTTP | Descripción |
|--------|------|-------------|
| `FOLIO_NOT_FOUND` | 404 | El folio especificado no existe |
| `LOCATION_NOT_FOUND` | 404 | El índice de ubicación no existe en la cotización |
| `VERSION_CONFLICT` | 409 | La versión enviada no coincide con la almacenada (optimistic lock) |
| `VALIDATION_ERROR` | 422 | Uno o más campos no pasaron validación |
| `INVALID_REFERENCE` | 400 | Suscriptor o agente no válido |
| `NO_CALCULABLE_LOCATIONS` | 422 | Todas las ubicaciones tienen alertas bloqueantes |
| `ZIP_CODE_NOT_FOUND` | 404 | Código postal no existe en el catálogo |
| `INTERNAL_ERROR` | 500 | Error interno del servidor |

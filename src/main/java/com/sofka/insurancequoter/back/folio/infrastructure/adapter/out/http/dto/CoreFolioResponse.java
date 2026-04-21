package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.http.dto;

import java.time.Instant;

// Response DTO for GET /v1/folios from the core service
public record CoreFolioResponse(String folioNumber, Instant generatedAt) {}

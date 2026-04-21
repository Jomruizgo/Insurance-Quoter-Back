package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.swaggerdocs;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto.CreateFolioRequest;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto.FolioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

// Swagger annotations live here — FolioController stays clean
@Tag(name = "Folios", description = "Gestión de folios de cotización")
@RequestMapping("/v1/folios")
public interface FolioApi {

    @Operation(summary = "Crear o recuperar folio de cotización",
               description = "Crea un nuevo folio si no existe uno activo para el mismo suscriptor y agente (CREATED). "
                             + "Si ya existe, lo retorna con HTTP 200 (idempotencia).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Folio creado exitosamente"),
            @ApiResponse(responseCode = "200", description = "Folio existente recuperado (idempotencia)"),
            @ApiResponse(responseCode = "400", description = "Suscriptor o agente no encontrado en el core service"),
            @ApiResponse(responseCode = "422", description = "Datos de entrada inválidos")
    })
    @PostMapping
    ResponseEntity<FolioResponse> createFolio(@Valid @RequestBody CreateFolioRequest request);
}

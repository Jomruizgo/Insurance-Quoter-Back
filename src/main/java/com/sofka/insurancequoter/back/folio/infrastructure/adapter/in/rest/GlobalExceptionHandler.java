package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.folio.application.usecase.InvalidReferenceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

// Translates domain and validation exceptions into standardised HTTP error responses
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidReferenceException.class)
    public ResponseEntity<Map<String, String>> handleInvalidReference(InvalidReferenceException ex) {
        return ResponseEntity.status(400)
                .body(Map.of(
                        "error", "Invalid subscriber or agent",
                        "code", "INVALID_REFERENCE"
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException ex) {
        List<String> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField())
                .toList();
        return ResponseEntity.status(422)
                .body(Map.of(
                        "error", "Validation failed",
                        "code", "VALIDATION_ERROR",
                        "fields", fields
                ));
    }
}

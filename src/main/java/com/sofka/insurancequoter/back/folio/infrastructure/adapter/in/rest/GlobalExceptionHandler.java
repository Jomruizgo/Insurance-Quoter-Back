package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.calculation.application.usecase.exception.CalculationResultNotFoundException;
import com.sofka.insurancequoter.back.calculation.application.usecase.exception.NoCalculableLocationsException;
import com.sofka.insurancequoter.back.coverage.application.usecase.exception.InvalidCoverageCodeException;
import com.sofka.insurancequoter.back.folio.application.usecase.CoreServiceException;
import com.sofka.insurancequoter.back.folio.application.usecase.InvalidReferenceException;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import com.sofka.insurancequoter.back.location.application.usecase.LocationNotFoundException;
import com.sofka.insurancequoter.back.location.application.usecase.VersionConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

// Translates domain and validation exceptions into standardised HTTP error responses
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(404)
                .body(Map.of(
                        "error", "Resource not found",
                        "code", "NOT_FOUND"
                ));
    }

    @ExceptionHandler(FolioNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleFolioNotFound(FolioNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(Map.of(
                        "error", "Folio not found",
                        "code", "FOLIO_NOT_FOUND"
                ));
    }

    @ExceptionHandler(CalculationResultNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCalculationResultNotFound(CalculationResultNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(Map.of(
                        "error", "Calculation result not found",
                        "code", "CALCULATION_RESULT_NOT_FOUND"
                ));
    }

    @ExceptionHandler(VersionConflictException.class)
    public ResponseEntity<Map<String, String>> handleVersionConflict(VersionConflictException ex) {
        return ResponseEntity.status(409)
                .body(Map.of(
                        "error", "Optimistic lock conflict",
                        "code", "VERSION_CONFLICT"
                ));
    }

    @ExceptionHandler(LocationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleLocationNotFound(LocationNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(Map.of(
                        "error", "Location index not found",
                        "code", "LOCATION_NOT_FOUND"
                ));
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<Map<String, String>> handleOptimisticLock(Exception ex) {
        return ResponseEntity.status(409)
                .body(Map.of(
                        "error", "Optimistic lock conflict",
                        "code", "VERSION_CONFLICT"
                ));
    }

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(422)
                .body(Map.of(
                        "error", "Validation failed",
                        "code", "VALIDATION_ERROR",
                        "fields", List.of(ex.getMessage())
                ));
    }

    @ExceptionHandler(InvalidCoverageCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCoverageCode(InvalidCoverageCodeException ex) {
        return ResponseEntity.status(422)
                .body(Map.of(
                        "error", "Validation failed",
                        "code", "VALIDATION_ERROR",
                        "fields", List.of(Map.of(
                                "field", "coverageOptions[].code",
                                "message", ex.getMessage()
                        ))
                ));
    }

    @ExceptionHandler(NoCalculableLocationsException.class)
    public ResponseEntity<Map<String, String>> handleNoCalculableLocations(NoCalculableLocationsException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(Map.of(
                        "error", "No calculable locations",
                        "code", "NO_CALCULABLE_LOCATIONS"
                ));
    }

    @ExceptionHandler(CoreServiceException.class)
    public ResponseEntity<Map<String, String>> handleCoreServiceError(CoreServiceException ex) {
        log.error("Core service unavailable: {}", ex.getMessage());
        return ResponseEntity.status(502)
                .body(Map.of(
                        "error", "Core service unavailable",
                        "code", "CORE_SERVICE_ERROR"
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(500)
                .body(Map.of(
                        "error", "Internal server error",
                        "code", "INTERNAL_ERROR"
                ));
    }
}

package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofka.insurancequoter.back.location.domain.model.Guarantee;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

// Converts List<Guarantee> to JSON string for JSONB storage and back
@Converter
public class GuaranteesConverter implements AttributeConverter<List<Guarantee>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<Guarantee>> TYPE_REF = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<Guarantee> guarantees) {
        if (guarantees == null || guarantees.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(guarantees);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize guarantees to JSON", e);
        }
    }

    @Override
    public List<Guarantee> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, TYPE_REF);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize guarantees from JSON", e);
        }
    }
}

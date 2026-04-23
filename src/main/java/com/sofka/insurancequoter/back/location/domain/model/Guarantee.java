package com.sofka.insurancequoter.back.location.domain.model;

import java.math.BigDecimal;

// Value object representing a coverage guarantee with its insured value
public record Guarantee(String code, BigDecimal insuredValue) {
}

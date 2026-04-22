package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response;

import java.math.BigDecimal;

public record GuaranteeResponse(String code, BigDecimal insuredValue) {
}

package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.request;

import java.math.BigDecimal;

public record GuaranteeRequest(String code, BigDecimal insuredValue) {
}

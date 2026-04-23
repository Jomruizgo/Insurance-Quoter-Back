package com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// DTO representing insured party data in REST requests and responses
public record InsuredDataDto(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 13) String rfc,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(max = 20) String phone
) {}

package com.sofka.insurancequoter.back.generalinfo.domain.model;

// Value object representing the insured party's contact and tax data
public record InsuredData(
        String name,
        String rfc,
        String email,
        String phone
) {}

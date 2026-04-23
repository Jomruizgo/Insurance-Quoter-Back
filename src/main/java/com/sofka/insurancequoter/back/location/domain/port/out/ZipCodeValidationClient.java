package com.sofka.insurancequoter.back.location.domain.port.out;

import com.sofka.insurancequoter.back.location.domain.model.ZipCodeInfo;

import java.util.Optional;

// Output port for validating zip codes against the core service
public interface ZipCodeValidationClient {

    Optional<ZipCodeInfo> validate(String zipCode);
}

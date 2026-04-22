package com.sofka.insurancequoter.back.location.domain.model;

// Enumeration of known codes that identify the cause of a blocking alert on a location
public enum BlockingAlertCode {
    MISSING_ZIP_CODE,
    MISSING_FIRE_KEY,
    NO_TARIFABLE_GUARANTEES
}

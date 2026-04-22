package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Embeddable mapping for the location_blocking_alerts table rows
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockingAlertEmbeddable {

    @Column(name = "alert_code", nullable = false, length = 50)
    private String alertCode;

    @Column(name = "alert_message", nullable = false, length = 255)
    private String alertMessage;
}

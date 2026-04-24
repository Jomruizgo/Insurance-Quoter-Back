package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "quotes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folio_number", nullable = false, unique = true, length = 20)
    private String folioNumber;

    @Column(name = "quote_status", nullable = false, length = 20)
    private String quoteStatus;

    @Column(name = "subscriber_id", nullable = false, length = 50)
    private String subscriberId;

    @Column(name = "agent_code", nullable = false, length = 50)
    private String agentCode;

    @Column(name = "number_of_locations")
    private Integer numberOfLocations;

    @Column(name = "location_type", length = 10)
    private String locationType;

    // General-info fields (written by the generalinfo bounded context)
    @Column(name = "insured_name", length = 100)
    private String insuredName;

    @Column(name = "insured_rfc", length = 13)
    private String insuredRfc;

    @Column(name = "insured_email", length = 100)
    private String insuredEmail;

    @Column(name = "insured_phone", length = 20)
    private String insuredPhone;

    @Column(name = "risk_classification", length = 20)
    private String riskClassification;

    @Column(name = "business_type", length = 20)
    private String businessType;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

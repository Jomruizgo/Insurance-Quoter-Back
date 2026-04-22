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

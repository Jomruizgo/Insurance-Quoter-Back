package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities;

import com.sofka.insurancequoter.back.location.domain.model.Guarantee;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.converter.GuaranteesConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "locations",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_locations_quote_index",
                columnNames = {"quote_id", "index"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quote_id", nullable = false)
    private Long quoteId;

    @Column(name = "index", nullable = false)
    private Integer index;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "location_name")
    private String locationName;

    // --- detail columns added in V5 ---

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "municipality", length = 100)
    private String municipality;

    @Column(name = "neighborhood", length = 100)
    private String neighborhood;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "construction_type", length = 50)
    private String constructionType;

    @Column(name = "level")
    private Integer level;

    @Column(name = "construction_year")
    private Integer constructionYear;

    @Column(name = "business_line_code", length = 50)
    private String businessLineCode;

    @Column(name = "business_line_fire_key", length = 50)
    private String businessLineFireKey;

    @Column(name = "business_line_description", length = 255)
    private String businessLineDescription;

    @Convert(converter = GuaranteesConverter.class)
    @Column(name = "guarantees", columnDefinition = "text")
    private List<Guarantee> guarantees;

    @Column(name = "catastrophic_zone", length = 50)
    private String catastrophicZone;

    @Column(name = "validation_status", nullable = false, length = 20)
    @Builder.Default
    private String validationStatus = "INCOMPLETE";

    // --- blocking alerts (V6 table) ---

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "location_blocking_alerts",
            joinColumns = @JoinColumn(name = "location_id"))
    @Builder.Default
    private List<BlockingAlertEmbeddable> blockingAlerts = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

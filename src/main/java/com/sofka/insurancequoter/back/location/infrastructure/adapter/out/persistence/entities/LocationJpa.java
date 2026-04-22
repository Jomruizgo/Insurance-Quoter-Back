package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

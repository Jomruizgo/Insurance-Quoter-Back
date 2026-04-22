package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.entities;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "coverage_options")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageOptionJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    private QuoteJpa quote;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "selected", nullable = false)
    private boolean selected;

    @Column(name = "deductible_percentage", precision = 5, scale = 2)
    private BigDecimal deductiblePercentage;

    @Column(name = "coinsurance_percentage", precision = 5, scale = 2)
    private BigDecimal coinsurancePercentage;
}

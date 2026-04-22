package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.entities;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// JPA entity mapping the calculation_results table — 1:1 with quotes
@Entity
@Table(name = "calculation_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculationResultJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false, unique = true)
    private QuoteJpa quote;

    @Column(name = "net_premium", nullable = false)
    private BigDecimal netPremium;

    @Column(name = "commercial_premium", nullable = false)
    private BigDecimal commercialPremium;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @OneToMany(mappedBy = "calculationResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PremiumByLocationJpa> premiumsByLocation = new ArrayList<>();
}

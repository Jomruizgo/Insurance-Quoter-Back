package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// JPA entity mapping the premiums_by_location table with 14-component coverage breakdown
@Entity
@Table(name = "premiums_by_location")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumByLocationJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculation_result_id", nullable = false)
    private CalculationResultJpa calculationResult;

    @Column(name = "location_index", nullable = false)
    private Integer locationIndex;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "net_premium")
    private BigDecimal netPremium;

    @Column(name = "commercial_premium")
    private BigDecimal commercialPremium;

    @Column(name = "calculable", nullable = false)
    private boolean calculable;

    // --- Coverage breakdown columns ---

    @Column(name = "fire_buildings")
    private BigDecimal fireBuildings;

    @Column(name = "fire_contents")
    private BigDecimal fireContents;

    @Column(name = "coverage_extension")
    private BigDecimal coverageExtension;

    @Column(name = "cattev")
    private BigDecimal cattev;

    @Column(name = "catfhm")
    private BigDecimal catfhm;

    @Column(name = "debris_removal")
    private BigDecimal debrisRemoval;

    @Column(name = "extraordinary_expenses")
    private BigDecimal extraordinaryExpenses;

    @Column(name = "rental_loss")
    private BigDecimal rentalLoss;

    @Column(name = "business_interruption")
    private BigDecimal businessInterruption;

    @Column(name = "electronic_equipment")
    private BigDecimal electronicEquipment;

    @Column(name = "theft")
    private BigDecimal theft;

    @Column(name = "cash_and_values")
    private BigDecimal cashAndValues;

    @Column(name = "glass")
    private BigDecimal glass;

    @Column(name = "luminous_signage")
    private BigDecimal luminousSignage;

    // --- Blocking alerts as element collection ---

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "premium_location_blocking_alerts",
            joinColumns = @JoinColumn(name = "premium_by_location_id"))
    @Builder.Default
    private List<PremiumBlockingAlertEmbeddable> blockingAlerts = new ArrayList<>();
}

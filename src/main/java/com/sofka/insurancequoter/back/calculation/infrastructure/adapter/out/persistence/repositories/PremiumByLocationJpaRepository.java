package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.repositories;

import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.entities.PremiumByLocationJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Spring Data repository for the premiums_by_location table
public interface PremiumByLocationJpaRepository extends JpaRepository<PremiumByLocationJpa, Long> {

    List<PremiumByLocationJpa> findByCalculationResultId(Long calculationResultId);
}

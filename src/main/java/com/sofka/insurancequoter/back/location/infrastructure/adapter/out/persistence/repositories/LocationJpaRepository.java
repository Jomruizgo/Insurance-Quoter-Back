package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.repositories;

import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities.LocationJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationJpaRepository extends JpaRepository<LocationJpa, Long> {

    List<LocationJpa> findByQuoteIdAndActiveTrue(Long quoteId);

    List<LocationJpa> findByQuoteId(Long quoteId);

    List<LocationJpa> findByQuoteIdAndIndexIn(Long quoteId, List<Integer> indices);
}

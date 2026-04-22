package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.adapter;

import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.domain.port.out.CoverageOptionRepository;
import com.sofka.insurancequoter.back.coverage.domain.port.out.QuoteLookupPort;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.entities.CoverageOptionJpa;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.mappers.CoverageOptionPersistenceMapper;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.repositories.CoverageOptionJpaRepository;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import com.sofka.insurancequoter.back.location.application.usecase.VersionConflictException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

// Implements CoverageOptionRepository and QuoteLookupPort using JPA
public class CoverageOptionJpaAdapter implements CoverageOptionRepository, QuoteLookupPort {

    private final CoverageOptionJpaRepository coverageOptionJpaRepository;
    private final QuoteJpaRepository quoteJpaRepository;
    private final CoverageOptionPersistenceMapper mapper;

    public CoverageOptionJpaAdapter(CoverageOptionJpaRepository coverageOptionJpaRepository,
                                     QuoteJpaRepository quoteJpaRepository,
                                     CoverageOptionPersistenceMapper mapper) {
        this.coverageOptionJpaRepository = coverageOptionJpaRepository;
        this.quoteJpaRepository = quoteJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public List<CoverageOption> findByFolioNumber(String folioNumber) {
        QuoteJpa quote = getQuoteOrThrow(folioNumber);
        List<CoverageOptionJpa> jpaList = coverageOptionJpaRepository.findAllByQuote_Id(quote.getId());
        return mapper.toDomainList(jpaList);
    }

    @Override
    @Transactional
    public List<CoverageOption> replaceAll(String folioNumber, List<CoverageOption> options) {
        QuoteJpa quote = getQuoteOrThrow(folioNumber);
        // Bulk DELETE + immediate flush before INSERT to respect UNIQUE(quote_id, code) constraint
        coverageOptionJpaRepository.deleteAllByQuote_Id(quote.getId());
        coverageOptionJpaRepository.flush();
        // Save new options
        List<CoverageOptionJpa> jpaList = mapper.toJpaList(options, quote);
        List<CoverageOptionJpa> savedList = coverageOptionJpaRepository.saveAll(jpaList);
        // Touch the quote to trigger @UpdateTimestamp and force version increment via @Version
        quote.setUpdatedAt(Instant.now());
        quoteJpaRepository.save(quote);
        return mapper.toDomainList(savedList);
    }

    @Override
    public void assertFolioExists(String folioNumber) {
        quoteJpaRepository.findByFolioNumber(folioNumber)
                .orElseThrow(() -> new FolioNotFoundException(folioNumber));
    }

    @Override
    public long getCurrentVersion(String folioNumber) {
        QuoteJpa quote = getQuoteOrThrow(folioNumber);
        return quote.getVersion();
    }

    @Override
    public Instant getUpdatedAt(String folioNumber) {
        return getQuoteOrThrow(folioNumber).getUpdatedAt();
    }

    @Override
    public void assertVersionMatches(String folioNumber, long expectedVersion) {
        QuoteJpa quote = getQuoteOrThrow(folioNumber);
        if (quote.getVersion() != expectedVersion) {
            throw new VersionConflictException(folioNumber, expectedVersion, quote.getVersion());
        }
    }

    private QuoteJpa getQuoteOrThrow(String folioNumber) {
        return quoteJpaRepository.findByFolioNumber(folioNumber)
                .orElseThrow(() -> new FolioNotFoundException(folioNumber));
    }
}

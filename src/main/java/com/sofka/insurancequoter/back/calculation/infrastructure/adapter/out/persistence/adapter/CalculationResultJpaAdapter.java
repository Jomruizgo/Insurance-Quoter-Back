package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.adapter;

import com.sofka.insurancequoter.back.calculation.application.usecase.command.AcceptQuoteCommand;
import com.sofka.insurancequoter.back.calculation.application.usecase.dto.QuoteCalculationSnapshot;
import com.sofka.insurancequoter.back.calculation.domain.model.CalculationResult;
import com.sofka.insurancequoter.back.calculation.domain.model.PremiumByLocation;
import com.sofka.insurancequoter.back.calculation.domain.port.out.AcceptQuoteRepository;
import com.sofka.insurancequoter.back.calculation.domain.port.out.CalculationResultRepository;
import com.sofka.insurancequoter.back.calculation.domain.port.out.GetCalculationResultRepository;
import com.sofka.insurancequoter.back.calculation.domain.port.out.QuoteCalculationReader;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.entities.CalculationResultJpa;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.entities.PremiumByLocationJpa;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.mappers.CalculationPersistenceMapper;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.repositories.CalculationResultJpaRepository;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.repositories.PremiumByLocationJpaRepository;
import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.repositories.CoverageOptionJpaRepository;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import com.sofka.insurancequoter.back.location.application.usecase.VersionConflictException;
import com.sofka.insurancequoter.back.location.domain.model.Location;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities.LocationJpa;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.mappers.LocationPersistenceMapper;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.repositories.LocationJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

// Adapter implementing persistence ports for the calculation bounded context
// Cross-context reads: accesses location, coverage and folio JPA repositories directly
public class CalculationResultJpaAdapter implements CalculationResultRepository, QuoteCalculationReader,
        GetCalculationResultRepository, AcceptQuoteRepository {

    private final QuoteJpaRepository quoteJpaRepository;
    private final LocationJpaRepository locationJpaRepository;
    private final CoverageOptionJpaRepository coverageOptionJpaRepository;
    private final CalculationResultJpaRepository calculationResultJpaRepository;
    private final PremiumByLocationJpaRepository premiumByLocationJpaRepository;
    private final CalculationPersistenceMapper calculationPersistenceMapper;
    private final LocationPersistenceMapper locationPersistenceMapper;

    public CalculationResultJpaAdapter(QuoteJpaRepository quoteJpaRepository,
                                       LocationJpaRepository locationJpaRepository,
                                       CoverageOptionJpaRepository coverageOptionJpaRepository,
                                       CalculationResultJpaRepository calculationResultJpaRepository,
                                       PremiumByLocationJpaRepository premiumByLocationJpaRepository,
                                       CalculationPersistenceMapper calculationPersistenceMapper,
                                       LocationPersistenceMapper locationPersistenceMapper) {
        this.quoteJpaRepository = quoteJpaRepository;
        this.locationJpaRepository = locationJpaRepository;
        this.coverageOptionJpaRepository = coverageOptionJpaRepository;
        this.calculationResultJpaRepository = calculationResultJpaRepository;
        this.premiumByLocationJpaRepository = premiumByLocationJpaRepository;
        this.calculationPersistenceMapper = calculationPersistenceMapper;
        this.locationPersistenceMapper = locationPersistenceMapper;
    }

    @Override
    public QuoteCalculationSnapshot getSnapshot(String folioNumber) {
        QuoteJpa quoteJpa = quoteJpaRepository.findByFolioNumber(folioNumber)
                .orElseThrow(() -> new FolioNotFoundException(folioNumber));

        List<Location> locations = locationJpaRepository
                .findByQuoteIdAndActiveTrue(quoteJpa.getId())
                .stream()
                .map(locationPersistenceMapper::toDomain)
                .toList();

        List<CoverageOption> coverageOptions = coverageOptionJpaRepository
                .findAllByQuote_Id(quoteJpa.getId())
                .stream()
                .map(c -> new CoverageOption(
                        c.getCode(), c.getDescription(), c.isSelected(),
                        c.getDeductiblePercentage(), c.getCoinsurancePercentage()))
                .toList();

        return new QuoteCalculationSnapshot(
                folioNumber, quoteJpa.getVersion(), locations, coverageOptions
        );
    }

    @Override
    @Transactional
    public long persist(String folioNumber, CalculationResult result) {
        // 1. Load quote
        QuoteJpa quoteJpa = quoteJpaRepository.findByFolioNumber(folioNumber)
                .orElseThrow(() -> new FolioNotFoundException(folioNumber));

        // 2. Update quote status — this triggers @Version increment via Hibernate
        quoteJpa.setQuoteStatus("CALCULATED");
        quoteJpa.setUpdatedAt(Instant.now());
        QuoteJpa savedQuote = quoteJpaRepository.saveAndFlush(quoteJpa);

        // 3. Delete previous calculation result (idempotent recalculation)
        calculationResultJpaRepository.deleteByQuoteId(quoteJpa.getId());

        // 4. Save new calculation result
        CalculationResultJpa calculationResultJpa = calculationPersistenceMapper.toJpa(result, savedQuote);
        CalculationResultJpa saved = calculationResultJpaRepository.save(calculationResultJpa);

        // 5. Save premium by location entries
        List<PremiumByLocationJpa> premiumJpaList = result.premiumsByLocation().stream()
                .map(pbl -> calculationPersistenceMapper.toPremiumByLocationJpa(pbl, saved))
                .toList();
        premiumByLocationJpaRepository.saveAll(premiumJpaList);

        // 6. Return the new version after Hibernate incremented it
        return savedQuote.getVersion();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CalculationResult> find(String folioNumber) {
        return quoteJpaRepository.findByFolioNumber(folioNumber)
                .flatMap(quote -> calculationResultJpaRepository.findByQuoteId(quote.getId()))
                .map(calculationPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public long accept(AcceptQuoteCommand command) {
        QuoteJpa quoteJpa = quoteJpaRepository.findByFolioNumber(command.folioNumber())
                .orElseThrow(() -> new FolioNotFoundException(command.folioNumber()));

        if (!quoteJpa.getVersion().equals(command.version())) {
            throw new VersionConflictException(command.folioNumber(), quoteJpa.getVersion(), command.version());
        }

        quoteJpa.setQuoteStatus("ISSUED");
        quoteJpa.setUpdatedAt(Instant.now());
        QuoteJpa saved = quoteJpaRepository.saveAndFlush(quoteJpa);
        return saved.getVersion();
    }
}
